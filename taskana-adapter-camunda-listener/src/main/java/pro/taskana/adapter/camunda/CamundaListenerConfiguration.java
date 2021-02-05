package pro.taskana.adapter.camunda;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.adapter.camunda.exceptions.SystemException;


public class CamundaListenerConfiguration {

  private static final String TASKANA_ADAPTER_CREATE_OUTBOX_SCHEMA =
      "taskana.adapter.create_outbox_schema";
  private static final String TASKANA_OUTBOX_PROPERTIES = "taskana-outbox.properties";
  private static final String TASKANA_ADAPTER_OUTBOX_SCHEMA = "taskana.adapter.outbox.schema";
  private static final String TASKANA_ADAPTER_OUTBOX_DATASOURCE_JNDI =
      "taskana.adapter.outbox.datasource.jndi";
  private static final String TASKANA_ADAPTER_OUTBOX_DATASOURCE_DRIVER =
      "taskana.adapter.outbox.datasource.driver";
  private static final String TASKANA_ADAPTER_OUTBOX_DATASOURCE_URL =
      "taskana.adapter.outbox.datasource.url";
  private static final String TASKANA_ADAPTER_OUTBOX_DATASOURCE_USERNAME =
      "taskana.adapter.outbox.datasource.username";
  private static final String TASKANA_ADAPTER_OUTBOX_DATASOURCE_PASSWORD =
      "taskana.adapter.outbox.datasource.password";
  private static final String TASKANA_ADAPTER_OUTBOX_INITIAL_NUMBER_OF_TASK_CREATION_RETRIES =
      "taskana.adapter.outbox.initial.number.of.task.creation.retries";
  private static final String OUTBOX_SYSTEM_PROPERTY = "taskana.outbox.properties";

  private static final String OUTBOX_SCHEMA_DEFAULT = "taskana_tables";
  private static final boolean CREATE_OUTBOX_SCHEMA_DEFAULT = true;
  private static final int INITIAL_NUMBER_OF_TASK_CREATION_RETRIES_DEFAULT = 5;

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaListenerConfiguration.class);

  private final Properties outboxProperties = new Properties();

  private CamundaListenerConfiguration() {

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

  public static CamundaListenerConfiguration getInstance() {
    return CamundaListenerConfiguration.LazyHolder.INSTANCE;
  }

  public static boolean getCreateOutboxSchema() {

    String createOutboxSchemaProperty =
        getInstance().outboxProperties.getProperty(TASKANA_ADAPTER_CREATE_OUTBOX_SCHEMA);

    if ((createOutboxSchemaProperty == null) || createOutboxSchemaProperty.isEmpty()) {
      LOGGER.info(
          "Couldn't retrieve boolean property to create schema or not, setting to default ");
      return CREATE_OUTBOX_SCHEMA_DEFAULT;
    } else {
      return !"false".equalsIgnoreCase(createOutboxSchemaProperty);
    }
  }

  public static String getOutboxSchema() {

    String outboxSchema = getInstance().outboxProperties.getProperty(TASKANA_ADAPTER_OUTBOX_SCHEMA);

    if (outboxSchema == null || outboxSchema.isEmpty()) {
      LOGGER.info("Couldn't retrieve property entry for outbox schema, setting to default ");
      return OUTBOX_SCHEMA_DEFAULT;

    } else {
      return outboxSchema;
    }
  }

  public static String getOutboxDatasourceJndi() {
    return getInstance().outboxProperties.getProperty(TASKANA_ADAPTER_OUTBOX_DATASOURCE_JNDI);
  }

  public static String getOutboxDatasourceDriver() {
    return getInstance().outboxProperties.getProperty(TASKANA_ADAPTER_OUTBOX_DATASOURCE_DRIVER);
  }

  public static String getOutboxDatasourceUrl() {
    return getInstance().outboxProperties.getProperty(TASKANA_ADAPTER_OUTBOX_DATASOURCE_URL);
  }

  public static String getOutboxDatasourceUsername() {
    return getInstance().outboxProperties.getProperty(TASKANA_ADAPTER_OUTBOX_DATASOURCE_USERNAME);
  }

  public static String getOutboxDatasourcePassword() {
    return getInstance().outboxProperties.getProperty(TASKANA_ADAPTER_OUTBOX_DATASOURCE_PASSWORD);
  }

  public static int getInitialNumberOfTaskCreationRetries() {
    int initialNumberOfTaskCreationRetries;

    String initialNumberOfTaskCreationRetriesProperty =
        getInstance()
            .outboxProperties
            .getProperty(TASKANA_ADAPTER_OUTBOX_INITIAL_NUMBER_OF_TASK_CREATION_RETRIES);

    try {
      initialNumberOfTaskCreationRetries =
          Integer.parseInt(initialNumberOfTaskCreationRetriesProperty);
    } catch (NumberFormatException e) {
      initialNumberOfTaskCreationRetries = INITIAL_NUMBER_OF_TASK_CREATION_RETRIES_DEFAULT;
      LOGGER.warn(
          String.format(
              "Attempted to retrieve initial number of task creation retries and caught "
                  + "Exception. Setting default for initial number of "
                  + "task creation retries to %d ",
              initialNumberOfTaskCreationRetries),
          e);
    }

    return initialNumberOfTaskCreationRetries;
  }

  private void readPropertiesFromClasspath() {

    try (InputStream propertiesStream =
        this.getClass().getClassLoader().getResourceAsStream(TASKANA_OUTBOX_PROPERTIES)) {

      outboxProperties.load(propertiesStream);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            String.format(
                "Outbox properties were loaded from file %s from classpath.",
                TASKANA_OUTBOX_PROPERTIES));
      }
    } catch (Exception e) {
      LOGGER.warn(
          String.format(
              "Caught Exception while trying to load properties from file %s from classpath",
              TASKANA_OUTBOX_PROPERTIES),
          e);
      throw new SystemException(
          String.format(
              "Internal System error when processing properties file %s ",
              TASKANA_OUTBOX_PROPERTIES),
          e.getCause());
    }
  }

  private static class LazyHolder {
    private static final CamundaListenerConfiguration INSTANCE = new CamundaListenerConfiguration();
  }
}
