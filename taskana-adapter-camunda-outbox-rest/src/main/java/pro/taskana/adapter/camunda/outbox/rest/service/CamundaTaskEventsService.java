package pro.taskana.adapter.camunda.outbox.rest.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spinjar.com.fasterxml.jackson.databind.JsonNode;
import spinjar.com.fasterxml.jackson.databind.ObjectMapper;

import pro.taskana.adapter.camunda.TaskanaConfigurationProperties;
import pro.taskana.adapter.camunda.outbox.rest.exceptions.SystemException;
import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEvent;

/** Implementation of the Outbox REST service. */
public class CamundaTaskEventsService implements TaskanaConfigurationProperties {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskEventsService.class);
  private static final String CREATE = "create";
  private static final String COMPLETE = "complete";
  private static final String DELETE = "delete";

  private static final String OUTBOX_SCHEMA = getSchemaFromProperties();
  private static final String SQL_GET_CREATE_EVENTS =
      "select * from %s.event_store where type = ? fetch first %d rows only";
  private static final String SQL_GET_COMPLETE_AND_DELETE_EVENTS =
      "select * from %s.event_store where type = ? OR type = ? fetch first %d rows only";
  private static final String SQL_WITHOUT_PLACEHOLDERS_DELETE_EVENTS =
      "delete from %s.event_store where id in (%s)";
  private static final int MAX_NUMBER_OF_EVENTS_DEFAULT = 50;

  private static Properties outboxProperties;
  private static int maxNumberOfEventsReturned = 0;

  static {
    try {
      Properties properties = getProperties();
      if (maxNumberOfEventsReturned == 0) {
        String maxNumberOfEventsString =
            properties.getProperty(TASKANA_ADAPTER_OUTBOX_MAX_NUMBER_OF_EVENTS);
        if (maxNumberOfEventsString != null && !maxNumberOfEventsString.isEmpty()) {
          maxNumberOfEventsReturned = Integer.parseInt(maxNumberOfEventsString);
        } else {
          maxNumberOfEventsReturned = MAX_NUMBER_OF_EVENTS_DEFAULT;
        }
      }
    } catch (IOException | NumberFormatException e) {
      if (maxNumberOfEventsReturned == 0) {
        maxNumberOfEventsReturned = MAX_NUMBER_OF_EVENTS_DEFAULT;
      }
      LOGGER.warn("attempted to retrieve max number of events to be returned and caught ", e);
    }
    LOGGER.info(
        "Outbox Rest Api will return at max {} events per request",
        Integer.valueOf(maxNumberOfEventsReturned));
  }

  private DataSource dataSource = null;

  public List<CamundaTaskEvent> getEvents(List<String> requestedEventTypes) {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();

    if (requestedEventTypes.contains(CREATE)) {

      camundaTaskEvents = getCreateEvents();

    } else if (requestedEventTypes.contains(DELETE) && requestedEventTypes.contains(COMPLETE)) {

      camundaTaskEvents = getCompleteAndDeleteEvents();
    }
    if (LOGGER.isDebugEnabled()) {

      LOGGER.debug(
          "outbox retrieved {} camundaTaskEvents: {}",
          camundaTaskEvents.size(),
          camundaTaskEvents.stream().map(Object::toString).collect(Collectors.joining(";\n")));
    }
    return camundaTaskEvents;
  }

  public void deleteEvents(String idsAsJsonArray) {

    List<Integer> idsAsIntegers = getIdsAsIntegers(idsAsJsonArray);

    String deleteEventsSqlWithPlaceholders =
        String.format(
            SQL_WITHOUT_PLACEHOLDERS_DELETE_EVENTS,
            OUTBOX_SCHEMA,
            preparePlaceHolders(idsAsIntegers.size()));

    try (Connection connection = getConnection()) {

      PreparedStatement preparedStatement =
          connection.prepareStatement(deleteEventsSqlWithPlaceholders);

      setPreparedStatementValues(preparedStatement, idsAsIntegers);
      preparedStatement.execute();

    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to delete events from the outbox table", e);
    }
  }

  private static DataSource createDatasource(
      String driver, String jdbcUrl, String username, String password) {
    return new PooledDataSource(driver, jdbcUrl, username, password);
  }

  private List<CamundaTaskEvent> getCreateEvents() {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();

    try (Connection connection = getConnection()) {

      String sql =
          String.format(
              SQL_GET_CREATE_EVENTS, OUTBOX_SCHEMA, Integer.valueOf(maxNumberOfEventsReturned));
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

        preparedStatement.setString(1, CREATE);

        ResultSet camundaTaskEventResultSet = preparedStatement.executeQuery();
        camundaTaskEvents = getCamundaTaskEvents(camundaTaskEventResultSet);
      }

    } catch (SQLException | NullPointerException e) {
      LOGGER.warn("Caught Exception while trying to retrieve create events from the outbox", e);
    }
    return camundaTaskEvents;
  }

  private String preparePlaceHolders(int length) {
    return String.join(",", Collections.nCopies(length, "?"));
  }

  private void setPreparedStatementValues(PreparedStatement preparedStatement, List<Integer> ids)
      throws SQLException {
    for (int i = 0; i < ids.size(); i++) {
      preparedStatement.setObject(i + 1, ids.get(i));
    }
  }

  private List<CamundaTaskEvent> getCamundaTaskEvents(ResultSet createEventsResultSet)
      throws SQLException {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();

    while (createEventsResultSet.next()) {

      CamundaTaskEvent camundaTaskEvent = new CamundaTaskEvent();

      camundaTaskEvent.setId(createEventsResultSet.getInt(1));
      camundaTaskEvent.setType(createEventsResultSet.getString(2));
      camundaTaskEvent.setCreated(formatDate(createEventsResultSet.getTimestamp(3)));
      camundaTaskEvent.setPayload(createEventsResultSet.getString(4));

      camundaTaskEvents.add(camundaTaskEvent);
    }

    return camundaTaskEvents;
  }

  private List<Integer> getIdsAsIntegers(String idsAsJsonArray) {

    ObjectMapper objectMapper = new ObjectMapper();
    List<Integer> idsAsIntegers = new ArrayList<>();

    try {
      JsonNode idsAsJsonArrayNode = objectMapper.readTree(idsAsJsonArray).get("taskCreationIds");

      if (idsAsJsonArrayNode != null) {
        idsAsJsonArrayNode.forEach(id -> idsAsIntegers.add(Integer.valueOf(id.asInt())));
      }

    } catch (IOException e) {
      LOGGER.warn(
          "Caught IOException while trying to read the passed JSON-Object in the POST-Request"
              + " to delete events from the outbox table",
          e);
    }
    return idsAsIntegers;
  }

  private List<CamundaTaskEvent> getCompleteAndDeleteEvents() {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();

    String sql =
        String.format(
            SQL_GET_COMPLETE_AND_DELETE_EVENTS,
            OUTBOX_SCHEMA,
            Integer.valueOf(maxNumberOfEventsReturned));
    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

      preparedStatement.setString(1, COMPLETE);
      preparedStatement.setString(2, DELETE);

      ResultSet completeAndDeleteEventsResultSet = preparedStatement.executeQuery();
      camundaTaskEvents = getCamundaTaskEvents(completeAndDeleteEventsResultSet);

    } catch (SQLException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve complete/delete events from the outbox",
          e.getClass().getName());
    }

    return camundaTaskEvents;
  }

  private Connection getConnection() {

    Connection connection = null;
    try {
      connection = getDataSource().getConnection();
    } catch (SQLException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve a connection from the provided datasource",
          e.getClass().getName());
    }

    if (connection == null) {
      LOGGER.warn("Retrieved connection was NULL, Please make sure to provide a valid datasource.");
      throw new SystemException(
          "Retrieved connection was NULL. Please make sure to provide a valid datasource.");
    }
    return connection;
  }

  private DataSource getDataSource() {

    synchronized (CamundaTaskEventsService.class) {
      if (dataSource == null) {
        return getDataSourceFromPropertiesFile();
      }
    }
    return dataSource;
  }

  private DataSource getDataSourceFromPropertiesFile() {
    try {
      Properties properties = getProperties();
      String jndiUrl = properties.getProperty(TASKANA_ADAPTER_OUTBOX_DATASOURCE_JNDI);

      if (jndiUrl != null) {
        dataSource = (DataSource) new InitialContext().lookup(jndiUrl);

      } else {

        dataSource =
            createDatasource(
                properties.getProperty(TASKANA_ADAPTER_OUTBOX_DATASOURCE_DRIVER),
                properties.getProperty(TASKANA_ADAPTER_OUTBOX_DATASOURCE_URL),
                properties.getProperty(TASKANA_ADAPTER_OUTBOX_DATASOURCE_USERNAME),
                properties.getProperty(TASKANA_ADAPTER_OUTBOX_DATASOURCE_PASSWORD));
      }

    } catch (IOException | NamingException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve the datasource from the provided properties file",
          e.getClass().getName());
    }

    return dataSource;
  }

  private static Properties getProperties() throws IOException {
    if (outboxProperties == null) {
      InputStream propertiesInputStream =
          CamundaTaskEventsService.class
              .getClassLoader()
              .getResourceAsStream(TASKANA_OUTBOX_PROPERTIES);
      outboxProperties = new Properties();
      outboxProperties.load(propertiesInputStream);
    }
    return outboxProperties;
  }

  private static String getSchemaFromProperties() {

    String defaultSchema = "taskana_tables";

    InputStream propertiesStream =
        CamundaTaskEventsService.class
            .getClassLoader()
            .getResourceAsStream(TASKANA_OUTBOX_PROPERTIES);

    Properties properties = new Properties();
    String outboxSchema = null;

    try {

      properties.load(propertiesStream);
      outboxSchema = properties.getProperty(TASKANA_ADAPTER_OUTBOX_SCHEMA);

    } catch (IOException | NullPointerException e) {
      LOGGER.warn(
          "Caught Exception {} while trying to retrieve the outbox-schema "
              + "from the provided properties file.",
          e.getClass().getName());
    }

    outboxSchema = (outboxSchema == null || outboxSchema.isEmpty()) ? defaultSchema : outboxSchema;

    return outboxSchema;
  }

  private String formatDate(Date date) {
    if (date == null) {
      return null;
    } else {
      return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
          .withZone(ZoneId.systemDefault())
          .format(date.toInstant());
    }
  }
}
