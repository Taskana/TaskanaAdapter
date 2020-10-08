package pro.taskana.adapter.camunda.outbox.rest.service;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import pro.taskana.adapter.camunda.util.ReadPropertiesHelper;

/** Implementation of the Outbox REST service. */
public class CamundaTaskEventsService implements TaskanaConfigurationProperties {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskEventsService.class);
  private static final String CREATE = "create";
  private static final String COMPLETE = "complete";
  private static final String DELETE = "delete";

  private static final String OUTBOX_SCHEMA = getSchemaFromProperties();
  private static final String SQL_GET_CREATE_EVENTS =
      "select * from %s.event_store where type = ? "
          + "and remaining_retries>0 and blocked_until < ? fetch first %d rows only";
  private static final String SQL_GET_ALL_EVENTS = "select * from %s.event_store";
  private static final String SQL_GET_EVENT = "select * from %s.event_store where id = ? ";
  private static final String SQL_GET_COMPLETE_AND_DELETE_EVENTS =
      "select * from %s.event_store where type = ? OR type = ? fetch first %d rows only";
  private static final String SQL_GET_EVENTS_FILTERED_BY_RETRIES =
      "select * from %s.event_store where remaining_retries<= ?";
  private static final String SQL_GET_EVENTS_COUNT =
      "select count(id) from %s.event_store where remaining_retries = ?";
  private static final String SQL_WITHOUT_PLACEHOLDERS_DELETE_EVENTS =
      "delete from %s.event_store where id in (%s)";
  private static final String SQL_DECREASE_REMAINING_RETRIES =
      "update %s.event_store set remaining_retries = remaining_retries-1, blocked_until = ?, "
          + "error = ? where id = ?";
  private static final String SQL_SET_REMAINING_RETRIES =
      "update %s.event_store set remaining_retries = ? where id = ?";
  private static final String SQL_SET_REMAINING_RETRIES_FOR_MULTIPLE_EVENTS =
      "update %s.event_store set remaining_retries = ? where remaining_retries = ?";
  private static final String SQL_DELETE_FAILED_EVENT =
      "delete from %s.event_store where id = ? and remaining_retries <=0";
  private static final String SQL_DELETE_ALL_FAILED_EVENTS =
      "delete from %s.event_store where remaining_retries <= 0 ";
  private static final int MAX_NUMBER_OF_EVENTS_DEFAULT = 50;

  private static ObjectMapper objectMapper = new ObjectMapper();

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
    } else {
      camundaTaskEvents = getAllEvents();
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

  public void decreaseRemainingRetriesAndLogError(String eventIdAndErrorLog) {

    String sql = String.format(SQL_DECREASE_REMAINING_RETRIES, OUTBOX_SCHEMA);

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

      JsonNode id = objectMapper.readTree(eventIdAndErrorLog).get("taskEventId");

      JsonNode errorLog = objectMapper.readTree(eventIdAndErrorLog).get("errorLog");

      Instant blockedUntil = getBlockedUntil();

      preparedStatement.setTimestamp(1, Timestamp.from(blockedUntil));
      preparedStatement.setString(2, errorLog.asText());
      preparedStatement.setInt(3, id.asInt());
      preparedStatement.execute();

    } catch (Exception e) {
      LOGGER.warn(
          "Caught Exception while trying to decrease the remaining retries of camunda task event",
          e);
    }
  }

  public List<CamundaTaskEvent> getEventsFilteredByRetries(Integer remainingRetries) {

    List<CamundaTaskEvent> camundaTaskEventsFilteredByRetries = new ArrayList<>();

    String getEventsFilteredByRetriesSql =
        String.format(SQL_GET_EVENTS_FILTERED_BY_RETRIES, OUTBOX_SCHEMA);

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement =
            connection.prepareStatement(getEventsFilteredByRetriesSql)) {

      preparedStatement.setInt(1, remainingRetries);

      ResultSet camundaTaskEventFilteredByRetriesResultSet = preparedStatement.executeQuery();
      camundaTaskEventsFilteredByRetries =
          getCamundaTaskEvents(camundaTaskEventFilteredByRetriesResultSet);

    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to retrieve failed events from the outbox", e);
    }
    return camundaTaskEventsFilteredByRetries;
  }

  public String getEventsCount(int remainingRetries) {

    String eventsCount = "{\"eventsCount\":0}";

    String getEventsCountSql = String.format(SQL_GET_EVENTS_COUNT, OUTBOX_SCHEMA);

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(getEventsCountSql)) {

      preparedStatement.setInt(1, remainingRetries);

      ResultSet camundaTaskEventResultSet = preparedStatement.executeQuery();

      if (camundaTaskEventResultSet.next()) {

        eventsCount = eventsCount.replace("0", String.valueOf(camundaTaskEventResultSet.getInt(1)));
      }

    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to retrieve events count from the outbox", e);
    }
    return eventsCount;
  }

  public CamundaTaskEvent setRemainingRetries(int id, Map<String, Integer> newRemainingRetries) {

    CamundaTaskEvent event = getEvent(id);

    event.setRemainingRetries(newRemainingRetries.get("remainingRetries"));

    String setRemainingRetriesSql = String.format(SQL_SET_REMAINING_RETRIES, OUTBOX_SCHEMA);

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(setRemainingRetriesSql)) {

      preparedStatement.setInt(1, newRemainingRetries.get("remainingRetries"));
      preparedStatement.setInt(2, id);
      preparedStatement.execute();

    } catch (Exception e) {
      LOGGER.warn(
          "Caught Exception while trying to set remaining retries for camunda task event", e);
    }

    return event;
  }

  public List<CamundaTaskEvent> setRemainingRetriesForMultipleEvents(
      int retries, Map<String, Integer> newRemainingRetries) {

    List<CamundaTaskEvent> camundaTaskEventsFilteredByRetries = getEventsFilteredByRetries(retries);

    camundaTaskEventsFilteredByRetries.forEach(
        camundaTaskEvent ->
            camundaTaskEvent.setRemainingRetries(newRemainingRetries.get("remainingRetries")));

    String setRemainingRetriesForAllFilteredByRetriesSql =
        String.format(SQL_SET_REMAINING_RETRIES_FOR_MULTIPLE_EVENTS, OUTBOX_SCHEMA);

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement =
            connection.prepareStatement(setRemainingRetriesForAllFilteredByRetriesSql)) {

      preparedStatement.setInt(1, newRemainingRetries.get("remainingRetries"));
      preparedStatement.setInt(2, retries);
      preparedStatement.execute();

    } catch (Exception e) {
      LOGGER.warn(
          "Caught Exception while trying to set remaining retries "
              + "for all filtered by retries camunda task events",
          e);
    }

    return camundaTaskEventsFilteredByRetries;
  }

  public void deleteFailedEvent(int id) {

    String deleteFailedEventSql = String.format(SQL_DELETE_FAILED_EVENT, OUTBOX_SCHEMA);

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(deleteFailedEventSql)) {

      preparedStatement.setInt(1, id);
      preparedStatement.execute();

    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to delete failed camunda task event", e);
    }
  }

  public void deleteAllFailedEvents() {

    String flagEventsSqlWithPlaceholders =
        String.format(SQL_DELETE_ALL_FAILED_EVENTS, OUTBOX_SCHEMA);

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement =
            connection.prepareStatement(flagEventsSqlWithPlaceholders)) {

      preparedStatement.execute();

    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to delete all failed camunda task events", e);
    }
  }

  public CamundaTaskEvent getEvent(int id) {

    String sql = String.format(SQL_GET_EVENT, OUTBOX_SCHEMA);

    CamundaTaskEvent camundaTaskEvent;

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

      preparedStatement.setInt(1, id);

      ResultSet completeAndDeleteEventsResultSet = preparedStatement.executeQuery();
      if (completeAndDeleteEventsResultSet.next()) {
        camundaTaskEvent = new CamundaTaskEvent();

        camundaTaskEvent.setId(completeAndDeleteEventsResultSet.getInt(1));
        camundaTaskEvent.setType(completeAndDeleteEventsResultSet.getString(2));
        camundaTaskEvent.setCreated(formatDate(completeAndDeleteEventsResultSet.getTimestamp(3)));
        camundaTaskEvent.setPayload(completeAndDeleteEventsResultSet.getString(4));
        camundaTaskEvent.setRemainingRetries(completeAndDeleteEventsResultSet.getInt(5));
        camundaTaskEvent.setBlockedUntil(completeAndDeleteEventsResultSet.getString(6));
        camundaTaskEvent.setError(completeAndDeleteEventsResultSet.getString(7));

        return camundaTaskEvent;
      }

    } catch (SQLException throwables) {
      throwables.printStackTrace();
    }

    throw new RuntimeException("camunda task event not found");
  }

  public List<CamundaTaskEvent> getAllEvents() {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();

    String sql = String.format(SQL_GET_ALL_EVENTS, OUTBOX_SCHEMA);

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql)) {

      ResultSet camundaTaskEventResultSet = preparedStatement.executeQuery();
      camundaTaskEvents = getCamundaTaskEvents(camundaTaskEventResultSet);

    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to retrieve all events from the outbox", e);
    }
    return camundaTaskEvents;
  }

  private Instant getBlockedUntil() {

    Duration blockedDuration =
        Duration.parse(
            ReadPropertiesHelper.getPropertyValueFromFile(
                TASKANA_OUTBOX_PROPERTIES,
                TASKANA_ADAPTER_OUTBOX_DURATION_BETWEEN_TASK_CREATION_RETRIES));

    return Instant.now().plus(blockedDuration);
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
        preparedStatement.setTimestamp(2, Timestamp.from(Instant.now()));

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
      camundaTaskEvent.setRemainingRetries(createEventsResultSet.getInt(5));
      camundaTaskEvent.setBlockedUntil(createEventsResultSet.getString(6));
      camundaTaskEvent.setError(createEventsResultSet.getString(7));

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
      LOGGER.warn("Caught {} while trying to retrieve complete/delete events from the outbox", e);
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
      throw new RuntimeException(
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
