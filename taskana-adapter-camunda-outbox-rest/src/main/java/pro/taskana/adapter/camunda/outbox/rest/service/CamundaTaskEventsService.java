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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spinjar.com.fasterxml.jackson.databind.JsonNode;
import spinjar.com.fasterxml.jackson.databind.ObjectMapper;

import pro.taskana.adapter.camunda.TaskanaConfigurationProperties;
import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEvent;

/** Implementation of the Outbox REST service. */
public class CamundaTaskEventsService implements TaskanaConfigurationProperties {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskEventsService.class);
  private static final String OUTBOX_SCHEMA = getSchemaFromProperties();
  private static final String SQL_GET_CREATE_EVENTS =
      "SELECT * FROM " + OUTBOX_SCHEMA + ".event_store WHERE type = ?";
  private static final String SQL_GET_COMPLETE_AND_DELETE_EVENTS =
      "SELECT * FROM " + OUTBOX_SCHEMA + ".event_store WHERE type = ? OR type = ?";
  private static final String SQL_WITHOUT_PLACEHOLDERS_DELETE_EVENTS =
      "DELETE FROM " + OUTBOX_SCHEMA + ".event_store WHERE id in (%s)";

  private DataSource dataSource = null;

  public List<CamundaTaskEvent> getEvents(List<String> requestedEventTypes) {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();

    if (requestedEventTypes.contains("create")) {

      camundaTaskEvents = getCreateEvents();

    } else if (requestedEventTypes.contains("delete") && requestedEventTypes.contains("complete")) {

      camundaTaskEvents = getCompleteAndDeleteEvents();
    }

    return camundaTaskEvents;
  }

  public void deleteEvents(String idsAsJsonArray) {

    List<Integer> idsAsIntegers = getIdsAsIntegers(idsAsJsonArray);

    String deleteEventsSqlWithPlaceholders =
        String.format(
            SQL_WITHOUT_PLACEHOLDERS_DELETE_EVENTS, preparePlaceHolders(idsAsIntegers.size()));

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement =
            connection.prepareStatement(deleteEventsSqlWithPlaceholders)) {

      setPreparedStatementValues(preparedStatement, idsAsIntegers);
      preparedStatement.execute();

    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to delete events from the outbox table", e);
    }
  }

  public static DataSource createDatasource(
      String driver, String jdbcUrl, String username, String password) {
    return new PooledDataSource(driver, jdbcUrl, username, password);
  }

  private List<CamundaTaskEvent> getCreateEvents() {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = getPreparedCreateEventsStatement(connection)) {

      ResultSet camundaTaskEventResultSet = preparedStatement.executeQuery();
      camundaTaskEvents = getCamundaTaskEvents(camundaTaskEventResultSet);

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
        idsAsJsonArrayNode.forEach(id -> idsAsIntegers.add(id.asInt()));
      }

    } catch (IOException e) {
      LOGGER.warn(
          "Caught IOException while trying to read the passed JSON-Object in the POST-Request"
              + " to delete events from the outbox table",
          e);
    }
    return idsAsIntegers;
  }

  private PreparedStatement getPreparedCreateEventsStatement(Connection connection)
      throws SQLException {

    PreparedStatement preparedStatement = connection.prepareStatement(SQL_GET_CREATE_EVENTS);
    preparedStatement.setString(1, "create");

    return preparedStatement;
  }

  private List<CamundaTaskEvent> getCompleteAndDeleteEvents() {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement =
            getPreparedCompleteAndDeleteEventsStatement(connection); ) {

      ResultSet completeAndDeleteEventsResultSet = preparedStatement.executeQuery();
      camundaTaskEvents = getCamundaTaskEvents(completeAndDeleteEventsResultSet);

    } catch (SQLException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve complete/delete events from the outbox",
          e.getClass().getName());
    }

    return camundaTaskEvents;
  }

  private PreparedStatement getPreparedCompleteAndDeleteEventsStatement(Connection connection)
      throws SQLException {

    PreparedStatement preparedStatement =
        connection.prepareStatement(SQL_GET_COMPLETE_AND_DELETE_EVENTS);
    preparedStatement.setString(1, "complete");
    preparedStatement.setString(2, "delete");

    return preparedStatement;
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

    InputStream datasourceConfig =
        CamundaTaskEventsService.class
            .getClassLoader()
            .getResourceAsStream(TASKANA_OUTBOX_PROPERTIES);

    Properties properties = new Properties();

    try {

      properties.load(datasourceConfig);
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
