package pro.taskana.adapter.camunda.parselistener;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.adapter.camunda.CamundaListenerConfiguration;
import pro.taskana.adapter.camunda.schemacreator.DB;
import pro.taskana.adapter.camunda.schemacreator.TaskanaOutboxSchemaCreator;
import pro.taskana.common.api.exceptions.SystemException;

/**
 * Camunda engine plugin responsible for adding the TaskanaParseListener to the
 * ProcessEngineConfguration, as well as initializing the outbox tables.
 */
public class TaskanaParseListenerProcessEnginePlugin extends AbstractProcessEnginePlugin {

  private static final String OUTBOX_SCHEMA_VERSION = "1.0.0";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(TaskanaParseListenerProcessEnginePlugin.class);

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

    initParseListeners(processEngineConfiguration);
    initOutbox(processEngineConfiguration);
  }

  private void initParseListeners(ProcessEngineConfigurationImpl processEngineConfiguration) {

    try {

      List<BpmnParseListener> preParseListeners =
          processEngineConfiguration.getCustomPreBPMNParseListeners();

      if (preParseListeners == null) {
        preParseListeners = new ArrayList<>();
        processEngineConfiguration.setCustomPreBPMNParseListeners(preParseListeners);
      }
      preParseListeners.add(new TaskanaParseListener());

      LOGGER.debug("TaskanaParseListener registered successfully");

    } catch (Exception e) {

      LOGGER.warn("Caught exception while trying to register TaskanaParseListener", e);

      throw new SystemException(
          "An error occured while trying to register the TaskanaParseListener."
              + " Aborting the boot of camunda.");
    }
  }

  private void initOutbox(ProcessEngineConfigurationImpl processEngineConfiguration) {

    DataSource camundaDataSource = retrieveCamundaDatasource(processEngineConfiguration);

    createSchema(camundaDataSource);

    LOGGER.info("TaskanaOutbox initialized successfully");
  }

  private void createSchema(DataSource camundaDataSource) {

    String outboxSchema = initSchemaName(camundaDataSource);

    TaskanaOutboxSchemaCreator schemaCreator =
        new TaskanaOutboxSchemaCreator(camundaDataSource, outboxSchema);

    boolean isSchemaPreexisting = schemaCreator.isSchemaPreexisting();

    boolean shouldSchemaBeCreated = CamundaListenerConfiguration.getCreateOutboxSchema();

    if (!isSchemaPreexisting) {
      if (shouldSchemaBeCreated) {
        LOGGER.debug("Running scripts to create schema and tables for TaskanaOutbox");
        if (!schemaCreator.createSchema()) {
          LOGGER.error(
                  "An error occured while trying to automatically create the "
                          + "TaskanaOutbox schema and table. "
                          + "Aborting the boot of camunda.");
          throw new SystemException(
                  "An error occured while trying to automatically create the"
                          + " TaskanaOutbox schema and table. "
                          + "Aborting the boot of camunda.");
        }
      } else {
        LOGGER.error(
                "TaskanaOutbox schema does not exist and shall not be created.");
        throw new SystemException(
                "TaskanaOutbox schema does not exist and shall not be created.");
      }
    }

    if (!schemaCreator.isValidSchemaVersion(OUTBOX_SCHEMA_VERSION)) {

      LOGGER.warn(
          "Aborting start up of camunda. "
              + "Please migrate to the newest version of the TaskanaOutbox schema");
      throw new SystemException(
          "The Database Schema Version doesn't match the expected version "
              + OUTBOX_SCHEMA_VERSION
              + ". Aborting the boot of camunda.");
    }
  }

  private String initSchemaName(DataSource dataSource) {

    String outboxSchema = CamundaListenerConfiguration.getOutboxSchema();

    try (Connection connection = dataSource.getConnection()) {
      String databaseProductName = connection.getMetaData().getDatabaseProductName();
      if (DB.isPostgreSql(databaseProductName)) {
        outboxSchema = outboxSchema.toLowerCase();
      } else {
        outboxSchema = outboxSchema.toUpperCase();
      }
    } catch (SQLException ex) {
      LOGGER.error("Caught exception when attempting to initialize the schema name", ex);
    }

    LOGGER.debug("Using schema name {}", outboxSchema);

    return outboxSchema;
  }

  private DataSource retrieveCamundaDatasource(
      ProcessEngineConfigurationImpl processEngineConfiguration) {

    DataSource dataSource = processEngineConfiguration.getDataSource();

    if (dataSource == null) {
      LOGGER.warn(
          "ProcessEngineConfiguration returns null DataSource. "
              + "Retrieving DataSource from properties.");
      dataSource = getDataSourceFromPropertiesFile();
      if (dataSource == null) {
        LOGGER.warn(
            "getDataSourceFromPropertiesFile returns null. "
                + "Outbox tables must be initialized manually.");
        throw new MissingResourceException(
            "could not retrieve dataSource to initialize the outbox tables.",
            "TaskanaOutboxSchemaCreator",
            "TaskanaOutboxSchema");
      }
    }
    return dataSource;
  }

  private DataSource getDataSourceFromPropertiesFile() {
    DataSource dataSource = null;
    try {
      String jndiLookup = CamundaListenerConfiguration.getOutboxDatasourceJndi();

      if (jndiLookup != null) {
        dataSource = (DataSource) new InitialContext().lookup(jndiLookup);
        if (dataSource != null) {
          LOGGER.info("retrieved Datasource from jndi lookup {}", jndiLookup);
        } else {
          LOGGER.info("jndi lookup {} didn't return a Datasource.", jndiLookup);
        }
      } else {
        String driver = CamundaListenerConfiguration.getOutboxDatasourceDriver();
        String jdbcUrl = CamundaListenerConfiguration.getOutboxDatasourceUrl();
        String userName = CamundaListenerConfiguration.getOutboxDatasourceUsername();
        String password = CamundaListenerConfiguration.getOutboxDatasourcePassword();
        dataSource = createDatasource(driver, jdbcUrl, userName, password);
        LOGGER.info("created Datasource from properties {}, ...", jdbcUrl);
      }

    } catch (NamingException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve the datasource from the provided properties file",
          e.getClass().getName());
    }
    return dataSource;
  }

  private static DataSource createDatasource(
      String driver, String jdbcUrl, String username, String password) {
    return new PooledDataSource(driver, jdbcUrl, username, password);
  }
}
