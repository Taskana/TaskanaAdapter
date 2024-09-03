package io.kadai.adapter.camunda;

import io.kadai.common.api.exceptions.SystemException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutboxRestConfiguration {

  private static final String KADAI_OUTBOX_PROPERTIES = "kadai-outbox.properties";
  private static final String KADAI_ADAPTER_OUTBOX_SCHEMA = "kadai.adapter.outbox.schema";
  private static final String KADAI_ADAPTER_OUTBOX_DATASOURCE_JNDI =
      "kadai.adapter.outbox.datasource.jndi";
  private static final String KADAI_ADAPTER_OUTBOX_DATASOURCE_DRIVER =
      "kadai.adapter.outbox.datasource.driver";
  private static final String KADAI_ADAPTER_OUTBOX_DATASOURCE_URL =
      "kadai.adapter.outbox.datasource.url";
  private static final String KADAI_ADAPTER_OUTBOX_DATASOURCE_USERNAME =
      "kadai.adapter.outbox.datasource.username";
  private static final String KADAI_ADAPTER_OUTBOX_DATASOURCE_PASSWORD =
      "kadai.adapter.outbox.datasource.password";
  private static final String KADAI_ADAPTER_OUTBOX_MAX_NUMBER_OF_EVENTS =
      "kadai.adapter.outbox.max.number.of.events";
  private static final String KADAI_ADAPTER_OUTBOX_DURATION_BETWEEN_TASK_CREATION_RETRIES =
      "kadai.adapter.outbox.duration.between.task.creation.retries";
  private static final String OUTBOX_SYSTEM_PROPERTY = "kadai.outbox.properties";

  private static final String OUTBOX_SCHEMA_DEFAULT = "kadai_tables";
  private static final int MAX_NUMBER_OF_EVENTS_DEFAULT = 50;
  private static final Duration DURATION_BETWEEN_TASK_CREATION_RETRIES_DEFAULT =
      Duration.ofHours(1);

  private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRestConfiguration.class);

  private final Properties outboxProperties = new Properties();

  private OutboxRestConfiguration() {

    String outboxPropertiesFile = System.getProperty(OUTBOX_SYSTEM_PROPERTY);

    if (outboxPropertiesFile != null) {

      try (FileInputStream propertiesStream = new FileInputStream(outboxPropertiesFile)) {

        outboxProperties.load(propertiesStream);

        LOGGER.info(
            String.format("Outbox properties were loaded from file %s.", outboxPropertiesFile));

      } catch (Exception e) {
        LOGGER.warn(
            String.format(
                "Caught Exception while trying to load properties from "
                    + "provided properties file %s. "
                    + "Trying to read properties from classpath",
                outboxPropertiesFile),
            e);

        readPropertiesFromClasspath();
      }
    } else {
      readPropertiesFromClasspath();
    }
  }

  public static OutboxRestConfiguration getInstance() {
    return OutboxRestConfiguration.LazyHolder.INSTANCE;
  }

  public static String getOutboxSchema() {

    String outboxSchema = getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_SCHEMA);

    if (outboxSchema == null || outboxSchema.isEmpty()) {
      LOGGER.info("Couldn't retrieve property entry for outbox schema, setting to default ");
      return OUTBOX_SCHEMA_DEFAULT;

    } else {
      return outboxSchema;
    }
  }

  public static String getOutboxDatasourceJndi() {
    return getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_DATASOURCE_JNDI);
  }

  public static String getOutboxDatasourceDriver() {
    return getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_DATASOURCE_DRIVER);
  }

  public static String getOutboxDatasourceUrl() {
    return getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_DATASOURCE_URL);
  }

  public static String getOutboxDatasourceUsername() {
    return getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_DATASOURCE_USERNAME);
  }

  public static String getOutboxDatasourcePassword() {
    return getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_DATASOURCE_PASSWORD);
  }

  public static int getOutboxMaxNumberOfEvents() {

    int maxNumberOfEventsReturned;

    String maxNumberOfEventsString =
        getInstance().outboxProperties.getProperty(KADAI_ADAPTER_OUTBOX_MAX_NUMBER_OF_EVENTS);

    try {
      maxNumberOfEventsReturned = Integer.parseInt(maxNumberOfEventsString);
    } catch (NumberFormatException e) {
      maxNumberOfEventsReturned = MAX_NUMBER_OF_EVENTS_DEFAULT;
      LOGGER.warn(
          String.format(
              "Attempted to retrieve max number of events to be returned and caught Exception. "
                  + "Setting default for max number of events to be returned to %d  ",
              maxNumberOfEventsReturned),
          e);
    }

    return maxNumberOfEventsReturned;
  }

  public static Duration getDurationBetweenTaskCreationRetries() {

    String durationBetweentaskCreationRetriesProperty =
        getInstance()
            .outboxProperties
            .getProperty(KADAI_ADAPTER_OUTBOX_DURATION_BETWEEN_TASK_CREATION_RETRIES);

    if (durationBetweentaskCreationRetriesProperty == null
        || durationBetweentaskCreationRetriesProperty.isEmpty()) {
      LOGGER.info(
          "Couldn't retrieve property entry for duration between task creation retries, "
              + "setting to default ");
      return DURATION_BETWEEN_TASK_CREATION_RETRIES_DEFAULT;
    } else {
      try {
        return Duration.parse(durationBetweentaskCreationRetriesProperty);
      } catch (Exception e) {
        LOGGER.warn(
            String.format(
                "Attempted to retrieve duration between task creation retries and caught Exception."
                    + "Setting default to %s ",
                durationBetweentaskCreationRetriesProperty),
            e);

        return DURATION_BETWEEN_TASK_CREATION_RETRIES_DEFAULT;
      }
    }
  }

  private void readPropertiesFromClasspath() {
    try (InputStream propertiesStream =
        this.getClass().getClassLoader().getResourceAsStream(KADAI_OUTBOX_PROPERTIES)) {

      outboxProperties.load(propertiesStream);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            String.format(
                "Outbox properties were loaded from file %s from classpath.",
                KADAI_OUTBOX_PROPERTIES));
      }
    } catch (Exception e) {
      LOGGER.warn(
          String.format(
              "Caught Exception while trying to load properties from file %s from classpath",
              KADAI_OUTBOX_PROPERTIES),
          e);
      throw new SystemException(
          String.format(
              "Internal System error when processing properties file %s ",
              KADAI_OUTBOX_PROPERTIES),
          e.getCause());
    }
  }

  private static class LazyHolder {
    private static final OutboxRestConfiguration INSTANCE = new OutboxRestConfiguration();
  }
}
