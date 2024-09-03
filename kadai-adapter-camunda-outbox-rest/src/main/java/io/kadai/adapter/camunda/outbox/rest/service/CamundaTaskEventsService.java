package io.kadai.adapter.camunda.outbox.rest.service;

import io.kadai.adapter.camunda.OutboxRestConfiguration;
import io.kadai.adapter.camunda.outbox.rest.exception.CamundaTaskEventNotFoundException;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentException;
import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import jakarta.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spinjar.com.fasterxml.jackson.databind.JsonNode;
import spinjar.com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of the Outbox REST service.
 */
public class CamundaTaskEventsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskEventsService.class);
  private static final String CREATE = "create";
  private static final String COMPLETE = "complete";
  private static final String DELETE = "delete";
  private static final String RETRIES = "retries";
  private static final String TYPE = "type";

  private static final String LOCK_FOR = "lock-for";

  private static final List<String> ALLOWED_PARAMS = Stream.of(TYPE, RETRIES, LOCK_FOR)
      .collect(Collectors.toList());

  private static final String OUTBOX_SCHEMA = OutboxRestConfiguration.getOutboxSchema();
  private static final String SQL_GET_CREATE_EVENTS =
      "select * from %s.event_store where type = ? "
          + "and remaining_retries>0 and blocked_until < ? fetch first %d rows only";
  private static final String SQL_GET_AVAILABLE_CREATE_EVENTS =
      "select * from %s.event_store where type = ? and (lock_expire < ? or lock_expire is null) "
          + "and remaining_retries>0 and blocked_until < ? fetch first %d rows only for update";
  private static final String SQL_GET_ALL_EVENTS = "select * from %s.event_store";
  private static final String SQL_GET_ALL_AVAILABLE_EVENTS =
      "select * from %s.event_store where lock_expire "
          + "< ? or lock_expire is null for update";
  private static final String SQL_GET_EVENT = "select * from %s.event_store where id = ? ";
  private static final String SQL_GET_COMPLETE_AND_DELETE_EVENTS =
      "select * from %s.event_store where type = ? OR type = ? fetch first %d rows only";
  private static final String SQL_GET_AVAILABLE_COMPLETE_AND_DELETE_EVENTS =
      "select * from %s.event_store where (type = ? OR type = ?) and (lock_expire < ? or"
          + " lock_expire is null)"
          + " fetch first %d rows only for update";
  private static final String SQL_GET_EVENTS_FILTERED_BY_RETRIES =
      "select * from %s.event_store where remaining_retries = ?";

  private static final String SQL_GET_AVAILABLE_EVENTS_FILTERED_BY_RETRIES =
      "select * from %s.event_store where remaining_retries = ? and lock_expire "
          + "< ? or lock_expire is null for update";
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

  private static final String SQL_SET_LOCK_EXPIRE =
      "update %s.event_store set lock_expire = ? where id in (%s)";

  private static final String SQL_UNLOCK =
      "update %s.event_store set lock_expire = null where id in (%s)";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private static int maxNumberOfEventsReturned = 0;

  static {
    if (maxNumberOfEventsReturned == 0) {
      maxNumberOfEventsReturned = OutboxRestConfiguration.getOutboxMaxNumberOfEvents();
    }
    LOGGER.info(
        "Outbox Rest Api will return at max {} events per request", maxNumberOfEventsReturned);
  }

  private DataSource dataSource = null;

  public List<CamundaTaskEvent> getEvents(MultivaluedMap<String, String> filterParams)
      throws InvalidArgumentException {

    verifyNoInvalidParameters(filterParams);
    Duration lockDuration = null;
    List<CamundaTaskEvent> camundaTaskEvents;
    if (filterParams.containsKey(LOCK_FOR)) {
      lockDuration = Duration.of(Long.parseLong(filterParams.get(LOCK_FOR).get(0)),
          ChronoUnit.SECONDS);
    }
    if (filterParams.containsKey(TYPE) && filterParams.get(TYPE).contains(CREATE)) {

      camundaTaskEvents = getCreateEvents(lockDuration);

    } else if (filterParams.containsKey(TYPE)
        && filterParams.get(TYPE).contains(DELETE)
        && filterParams.get(TYPE).contains(COMPLETE)) {

      camundaTaskEvents = getCompleteAndDeleteEvents(lockDuration);

    } else if (filterParams.containsKey(RETRIES) && filterParams.get(RETRIES) != null) {

      int remainingRetries = getRetries(filterParams.get(RETRIES));

      camundaTaskEvents = getEventsFilteredByRetries(remainingRetries, lockDuration);
    } else {
      camundaTaskEvents = getAllEvents(lockDuration);
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

      JsonNode id = OBJECT_MAPPER.readTree(eventIdAndErrorLog).get("taskEventId");

      JsonNode errorLog = OBJECT_MAPPER.readTree(eventIdAndErrorLog).get("errorLog");

      Instant blockedUntil = getBlockedUntil();
      preparedStatement.setTimestamp(1, Timestamp.from(blockedUntil));
      preparedStatement.setString(2, errorLog.toString());
      preparedStatement.setInt(3, id.asInt());
      preparedStatement.execute();

    } catch (Exception e) {
      LOGGER.warn(
          "Caught Exception while trying to decrease the remaining retries of camunda task event",
          e);
    }
  }

  public List<CamundaTaskEvent> getEventsFilteredByRetries(Integer remainingRetries,
      Duration lockDuration) {

    List<CamundaTaskEvent> camundaTaskEventsFilteredByRetries = new ArrayList<>();
    String sqlStatement = lockDuration == null
        ? SQL_GET_EVENTS_FILTERED_BY_RETRIES : SQL_GET_AVAILABLE_EVENTS_FILTERED_BY_RETRIES;
    String getEventsFilteredByRetriesSql =
        String.format(sqlStatement, OUTBOX_SCHEMA);
    List<Integer> ids = null;

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement =
            connection.prepareStatement(getEventsFilteredByRetriesSql)) {
      try {
        preparedStatement.setInt(1, remainingRetries);
        if (lockDuration != null) {
          preparedStatement.setTimestamp(1, Timestamp.from(Instant.now()));
        }
        ResultSet camundaTaskEventFilteredByRetriesResultSet = preparedStatement.executeQuery();
        camundaTaskEventsFilteredByRetries =
            getCamundaTaskEvents(camundaTaskEventFilteredByRetriesResultSet);
        ids = camundaTaskEventsFilteredByRetries.stream().map(CamundaTaskEvent::getId)
            .collect(Collectors.toList());
        lockEvents(ids, lockDuration, connection);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Events locked: {}",
              ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
        }
      } catch (Exception e) {
        LOGGER.warn("Caught Exception while trying to retrieve failed events from the outbox",
            e);
        if (ids != null) {
          try {
            unlockEvents(ids, connection);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "Events unlocked: {}",
                  ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
            }
          } catch (Exception ex) {
            LOGGER.error("Failed to unlock events", ex);
          }
        }
      }
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

  public CamundaTaskEvent setRemainingRetries(int id, int retriesToSet)
      throws CamundaTaskEventNotFoundException {

    CamundaTaskEvent event = getEvent(id);

    event.setRemainingRetries(retriesToSet);

    String setRemainingRetriesSql = String.format(SQL_SET_REMAINING_RETRIES, OUTBOX_SCHEMA);

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(setRemainingRetriesSql)) {

      preparedStatement.setInt(1, retriesToSet);
      preparedStatement.setInt(2, id);
      preparedStatement.execute();

    } catch (Exception e) {
      LOGGER.warn(
          "Caught Exception while trying to set remaining retries for camunda task event", e);
    }

    return event;
  }

  public List<CamundaTaskEvent> setRemainingRetriesForMultipleEvents(
      int retries, int retriesToSet) {

    List<CamundaTaskEvent> camundaTaskEventsFilteredByRetries = getEventsFilteredByRetries(retries,
        null);

    camundaTaskEventsFilteredByRetries.forEach(
        camundaTaskEvent -> camundaTaskEvent.setRemainingRetries(retriesToSet));

    String setRemainingRetriesForAllFilteredByRetriesSql =
        String.format(SQL_SET_REMAINING_RETRIES_FOR_MULTIPLE_EVENTS, OUTBOX_SCHEMA);

    try (Connection connection = getConnection();
        PreparedStatement preparedStatement =
            connection.prepareStatement(setRemainingRetriesForAllFilteredByRetriesSql)) {

      preparedStatement.setInt(1, retriesToSet);
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

  public CamundaTaskEvent getEvent(int id) throws CamundaTaskEventNotFoundException {

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
        camundaTaskEvent.setCamundaTaskId(completeAndDeleteEventsResultSet.getString(8));

        return camundaTaskEvent;
      }

    } catch (SQLException e) {
      LOGGER.error("Caughr exception while trying to retrieve camunda task event", e);
    }

    throw new CamundaTaskEventNotFoundException("camunda task event not found");
  }

  public List<CamundaTaskEvent> getAllEvents(Duration lockDuration) {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();
    String sqlStatement = lockDuration == null
        ? SQL_GET_ALL_EVENTS : SQL_GET_ALL_AVAILABLE_EVENTS;
    String sql = String.format(sqlStatement, OUTBOX_SCHEMA);
    List<Integer> ids = null;
    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      try {
        if (lockDuration != null) {
          preparedStatement.setTimestamp(1, Timestamp.from(Instant.now()));
        }
        ResultSet camundaTaskEventResultSet = preparedStatement.executeQuery();
        camundaTaskEvents = getCamundaTaskEvents(camundaTaskEventResultSet);
        ids = camundaTaskEvents.stream().map(CamundaTaskEvent::getId)
            .collect(Collectors.toList());
        lockEvents(ids, lockDuration, connection);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Events locked: {}",
              ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
        }
      } catch (Exception e) {
        LOGGER.warn("Caught Exception while trying to retrieve all events from the outbox",
            e);
        if (ids != null) {
          try {
            unlockEvents(ids, connection);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "Events unlocked: {}",
                  ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
            }
          } catch (Exception ex) {
            LOGGER.error("Failed to unlock events", ex);
          }
        }
      }
    } catch (Exception e) {
      LOGGER.warn("Caught Exception while trying to retrieve all events from the outbox", e);
    }
    return camundaTaskEvents;
  }

  public void unlockEventForId(Integer eventId) {
    try (Connection connection = getConnection()) {
      unlockEvents(Collections.singletonList((eventId)), connection);
    } catch (Exception e) {
      LOGGER.error("Failed to unlock events", e);
    }
  }

  private int getRetries(List<String> retries) throws InvalidArgumentException {

    try {
      return Integer.parseInt(retries.get(0));

    } catch (NumberFormatException e) {
      throw new InvalidArgumentException("retries param must be of type Integer!");
    }
  }

  private void verifyNoInvalidParameters(MultivaluedMap<String, String> filterParams)
      throws InvalidArgumentException {

    List<String> invalidParams =
        filterParams.keySet().stream()
            .filter(key -> !ALLOWED_PARAMS.contains(key))
            .collect(Collectors.toList());

    if (!invalidParams.isEmpty()) {
      throw new InvalidArgumentException("Provided invalid request params: " + invalidParams);
    }
  }

  private Instant getBlockedUntil() {

    Duration blockedDuration = OutboxRestConfiguration.getDurationBetweenTaskCreationRetries();

    return Instant.now().plus(blockedDuration);
  }

  private static DataSource createDatasource(
      String driver, String jdbcUrl, String username, String password) {
    return new PooledDataSource(driver, jdbcUrl, username, password);
  }

  private List<CamundaTaskEvent> getCreateEvents(Duration lockDuration) {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();
    String sqlStatement = lockDuration == null
        ? SQL_GET_CREATE_EVENTS : SQL_GET_AVAILABLE_CREATE_EVENTS;
    List<Integer> ids = null;
    try (Connection connection = getConnection()) {

      String sql = String.format(sqlStatement, OUTBOX_SCHEMA, maxNumberOfEventsReturned);
      try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
        preparedStatement.setString(1, CREATE);
        preparedStatement.setTimestamp(2, Timestamp.from(Instant.now()));
        if (lockDuration != null) {
          preparedStatement.setTimestamp(3, Timestamp.from(Instant.now()));
        }
        ResultSet camundaTaskEventResultSet = preparedStatement.executeQuery();
        camundaTaskEvents = getCamundaTaskEvents(camundaTaskEventResultSet);
        ids = camundaTaskEvents.stream().map(CamundaTaskEvent::getId)
            .collect(Collectors.toList());
        lockEvents(ids, lockDuration, connection);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Events locked: {}",
              ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
        }
      } catch (Exception e) {
        LOGGER.warn("Caught Exception while trying to retrieve all events from the outbox",
              e);
        if (ids != null) {
          try {
            unlockEvents(ids, connection);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "Events unlocked: {}",
                  ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
            }
          } catch (Exception ex) {
            LOGGER.error("Failed to unlock events", ex);
          }
        }
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
      camundaTaskEvent.setCamundaTaskId(createEventsResultSet.getString(8));
      camundaTaskEvent.setSystemEngineIdentifier(createEventsResultSet.getString(9));
      camundaTaskEvent.setLockExpiresAt(formatDate(createEventsResultSet.getTimestamp(10)));

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

  private List<CamundaTaskEvent> getCompleteAndDeleteEvents(Duration lockDuration) {

    List<CamundaTaskEvent> camundaTaskEvents = new ArrayList<>();
    String sqlStatement = lockDuration == null
        ? SQL_GET_COMPLETE_AND_DELETE_EVENTS : SQL_GET_AVAILABLE_COMPLETE_AND_DELETE_EVENTS;
    String sql =
        String.format(sqlStatement, OUTBOX_SCHEMA, maxNumberOfEventsReturned);
    List<Integer> ids = null;
    try (Connection connection = getConnection();
        PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
      try {
        preparedStatement.setString(1, COMPLETE);
        preparedStatement.setString(2, DELETE);
        if (lockDuration != null) {
          preparedStatement.setTimestamp(3, Timestamp.from(Instant.now()));
        }
        ResultSet completeAndDeleteEventsResultSet = preparedStatement.executeQuery();
        camundaTaskEvents = getCamundaTaskEvents(completeAndDeleteEventsResultSet);
        ids = camundaTaskEvents.stream().map(CamundaTaskEvent::getId)
            .collect(Collectors.toList());

        lockEvents(ids, lockDuration, connection);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Events locked: {}",
              ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
        }
      } catch (Exception e) {
        LOGGER.warn("Caught Exception while trying to retrieve all events from the outbox",
            e);
        if (ids != null) {
          try {
            unlockEvents(ids, connection);
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug(
                  "Events unlocked: {}",
                  ids.stream().map(Object::toString).collect(Collectors.joining(";\n")));
            }
          } catch (Exception ex) {
            LOGGER.error("Failed to unlock events", ex);
          }
        }
      }
    } catch (SQLException | NullPointerException e) {
      LOGGER.warn(
          "Caught exception while trying to retrieve complete/delete events from the outbox", e);
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

  public void lockEvents(List<Integer> ids, Duration lockDuration, Connection connection) {
    if (lockDuration == null || ids.isEmpty()) {
      return;
    }
    String commaSeparatedIds = ids.stream()
        .map(Object::toString)
        .collect(Collectors.joining(", "));
    String sql = String.format(SQL_SET_LOCK_EXPIRE, OUTBOX_SCHEMA, commaSeparatedIds);
    try {
      PreparedStatement preparedStatement = connection.prepareStatement(sql);
      preparedStatement.setTimestamp(1,
          Timestamp.from(Instant.now().plus(lockDuration)));

      preparedStatement.execute();

    } catch (SQLException e) {
      LOGGER.error("Caught exception while trying to lock events", e);
    }
  }

  private void unlockEvents(List<Integer> ids, Connection connection) throws SQLException {
    String commaSeparatedIds = ids.stream()
        .map(Object::toString)
        .collect(Collectors.joining(", "));
    String sql = String.format(SQL_UNLOCK, OUTBOX_SCHEMA, commaSeparatedIds);
    try (PreparedStatement preparedStatement2 = connection.prepareStatement(sql)) {
      preparedStatement2.execute();
    }
  }

  private DataSource getDataSourceFromPropertiesFile() {
    try {

      String jndiUrl = OutboxRestConfiguration.getOutboxDatasourceJndi();
      if (jndiUrl != null) {
        dataSource = (DataSource) new InitialContext().lookup(jndiUrl);

      } else {

        dataSource =
            createDatasource(
                OutboxRestConfiguration.getOutboxDatasourceDriver(),
                OutboxRestConfiguration.getOutboxDatasourceUrl(),
                OutboxRestConfiguration.getOutboxDatasourceUsername(),
                OutboxRestConfiguration.getOutboxDatasourcePassword());
      }

    } catch (NamingException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve the datasource from the provided properties file",
          e.getClass().getName());
    }

    return dataSource;
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
