package pro.taskana.adapter.camunda.parselistener;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.adapter.camunda.TaskanaConfigurationProperties;
import pro.taskana.adapter.camunda.schemacreator.TaskanaOutboxSchemaCreator;
import pro.taskana.adapter.camunda.util.ReadPropertiesHelper;

/**
 * Camunda engine plugin for the taskana parse listener.
 *
 * @author jhe
 */
public class TaskanaParseListenerProcessEnginePlugin extends AbstractProcessEnginePlugin
    implements TaskanaConfigurationProperties {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(TaskanaParseListenerProcessEnginePlugin.class);


  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

    initParseListeners(processEngineConfiguration);
    initOutbox(processEngineConfiguration);
  }

  public static DataSource createDatasource(
      String driver, String jdbcUrl, String username, String password) {
    return new PooledDataSource(driver, jdbcUrl, username, password);
  }

  private void initParseListeners(ProcessEngineConfigurationImpl processEngineConfiguration) {

    List<BpmnParseListener> preParseListeners =
        processEngineConfiguration.getCustomPreBPMNParseListeners();

    if (preParseListeners == null) {
      preParseListeners = new ArrayList<BpmnParseListener>();
      processEngineConfiguration.setCustomPreBPMNParseListeners(preParseListeners);
    }

    preParseListeners.add(new TaskanaParseListener());

    LOGGER.debug("TaskanaParseListener registered successfully");
  }

  private void initOutbox(ProcessEngineConfigurationImpl processEngineConfiguration) {

    boolean processEngineMustBeClosed = false;
    DataSource dataSource = processEngineConfiguration.getDataSource();
    if (dataSource == null) {
      LOGGER.warn("ProcessEngineConfiguration returns null DataSource. "
                      + "Retrieving DataSource from properties.");
      dataSource = getDataSourceFromPropertiesFile();
      if (dataSource == null) {
        LOGGER.warn("getDataSourceFromPropertiesFile returns null. "
                        + "Outbox tables must be initialized manually.");
        return;
      }
    } else {
      processEngineMustBeClosed = true;
    }

    String outboxSchema = ReadPropertiesHelper.getPropertyValueFromFile(TASKANA_OUTBOX_PROPERTIES,
        TASKANA_ADAPTER_OUTBOX_SCHEMA);
    outboxSchema = (outboxSchema == null || outboxSchema.isEmpty()) ? TASKANA_OUTBOX_DEFAULT_SCHEMA
                       : outboxSchema;

    String createSchemaProperty = ReadPropertiesHelper
                                      .getPropertyValueFromFile(TASKANA_OUTBOX_PROPERTIES,
                                          TASKANA_ADAPTER_CREATE_SCHEMA);
    boolean createSchema = true;
    if (createSchemaProperty != null && "false".equals(createSchemaProperty.toLowerCase())) {
      createSchema = false;
    }
    if (createSchema) {
      TaskanaOutboxSchemaCreator schemaCreator = new TaskanaOutboxSchemaCreator(dataSource,
          outboxSchema);
      try {
        schemaCreator.run();
      } catch (Exception e) {
        LOGGER.error("Caught exception while trying to initialize the outbox-table", e);
        throw new MissingResourceException("could not initialize the outbox tables.",
            "TaskanaOutboxSchemaCreator", "TaskanaOutboxSchema");
      } finally {
        if (processEngineMustBeClosed) {
          ProcessEngine processEngine = processEngineConfiguration.getProcessEngine();
          if (processEngine != null) {
            processEngine.close();
          }
        }
      }
    }

    LOGGER.info("TaskanaOutbox initialized successfully");
  }

  private DataSource getDataSourceFromPropertiesFile() {
    DataSource dataSource = null;
    try {
      String jndiLookup = ReadPropertiesHelper.getPropertyValueFromFile(
          TASKANA_OUTBOX_PROPERTIES, TASKANA_ADAPTER_OUTBOX_DATASOURCE_JNDI);

      if (jndiLookup != null) {
        dataSource = (DataSource) new InitialContext().lookup(jndiLookup);
        if (dataSource != null) {
          LOGGER.info("retrieved Datasource from jndi lookup {}", jndiLookup);
        } else {
          LOGGER.info("jndi lookup {} didn't return a Datasource.", jndiLookup);
        }
      } else {
        String driver = ReadPropertiesHelper.getPropertyValueFromFile(
            TASKANA_OUTBOX_PROPERTIES, TASKANA_ADAPTER_OUTBOX_DATASOURCE_DRIVER);
        String jdbcUrl = ReadPropertiesHelper.getPropertyValueFromFile(
            TASKANA_OUTBOX_PROPERTIES, TASKANA_ADAPTER_OUTBOX_DATASOURCE_URL);
        String userName = ReadPropertiesHelper.getPropertyValueFromFile(
            TASKANA_OUTBOX_PROPERTIES, TASKANA_ADAPTER_OUTBOX_DATASOURCE_USERNAME);
        String password = ReadPropertiesHelper.getPropertyValueFromFile(
            TASKANA_OUTBOX_PROPERTIES, TASKANA_ADAPTER_OUTBOX_DATASOURCE_PASSWORD);

        dataSource =
            createDatasource(driver, jdbcUrl, userName, password);

        LOGGER.info("created Datasource from properties {}, ...", jdbcUrl);
      }

    } catch (NamingException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve the datasource from the provided properties file",
          e.getClass().getName());
    }
    return dataSource;
  }
}
