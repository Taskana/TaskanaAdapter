package pro.taskana.adapter.camunda.parselistener;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.adapter.camunda.schemacreator.TaskanaOutboxSchemaCreator;

/**
 * Camunda engine plugin for the taskana parse listener.
 *
 * @author jhe
 */
public class TaskanaParseListenerProcessEnginePlugin extends AbstractProcessEnginePlugin {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(TaskanaParseListenerProcessEnginePlugin.class);

  private static final String DEFAULT_SCHEMA = "taskana_tables";

  @Override
  public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

    initParseListeners(processEngineConfiguration);
    initOutbox(processEngineConfiguration);
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

    DataSource dataSource = processEngineConfiguration.getDataSource();
    if (dataSource == null) {
      return;
    }

    String outboxSchema = getSchemaFromProperties();
    outboxSchema = (outboxSchema == null || outboxSchema.isEmpty()) ? DEFAULT_SCHEMA : outboxSchema;

    TaskanaOutboxSchemaCreator schemaCreator = new TaskanaOutboxSchemaCreator(dataSource,
        outboxSchema);
    try {
      schemaCreator.run();
    } catch (Exception e) {
      LOGGER.warn("Caught {} while trying to initialize the outbox-table", e);
      // processEngineConfiguration.getProcessEngine().close();
    }

    LOGGER.debug("TaskanaOutbox initialized successfully");
  }

  private String getSchemaFromProperties() {
    InputStream propertiesStream =
        TaskanaParseListenerProcessEnginePlugin.class
            .getClassLoader()
            .getResourceAsStream("taskana-outbox-schema.properties");

    Properties properties = new Properties();
    String outboxSchema = null;

    try {

      properties.load(propertiesStream);
      outboxSchema = properties.getProperty("taskana.outbox.schema");


    } catch (IOException | NullPointerException e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve the outbox-schema from the provided properties file",
          e);
    }

    return outboxSchema;
  }
}
