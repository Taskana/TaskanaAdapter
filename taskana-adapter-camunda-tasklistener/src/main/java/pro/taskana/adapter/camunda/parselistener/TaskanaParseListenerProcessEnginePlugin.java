package pro.taskana.adapter.camunda.parselistener;

import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.taskana.adapter.camunda.schemacreator.TaskanaOutboxSchemaCreator;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaskanaParseListenerProcessEnginePlugin extends AbstractProcessEnginePlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaParseListenerProcessEnginePlugin.class);

    private static final String DEFAULT_SCHEMA = "taskana_tables";

    @Override
    public void preInit(ProcessEngineConfigurationImpl processEngineConfiguration) {

        initParseListeners (processEngineConfiguration);
        initOutbox(processEngineConfiguration);
    }

    private void initParseListeners(ProcessEngineConfigurationImpl processEngineConfiguration) {

        List<BpmnParseListener> preParseListeners = processEngineConfiguration.getCustomPreBPMNParseListeners();

        if(preParseListeners == null) {
            preParseListeners = new ArrayList<BpmnParseListener>();
            processEngineConfiguration.setCustomPreBPMNParseListeners(preParseListeners);
        }

        preParseListeners.add(new TaskanaParseListener());

    }

    private void initOutbox(ProcessEngineConfigurationImpl processEngineConfiguration) {

        DataSource dataSource = processEngineConfiguration.getDataSource();

        String schema = getSchemaFrom(dataSource);
        schema = (schema==null || schema.isEmpty()) ? schema : DEFAULT_SCHEMA;

        TaskanaOutboxSchemaCreator schemaCreator = new TaskanaOutboxSchemaCreator(dataSource, schema);
        try {
            schemaCreator.run();
        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to initialize the outbox-table", e);
            //processEngineConfiguration.getProcessEngine().close();
        }
    }

    private String getSchemaFrom(DataSource dataSource) {
        try {
            Connection connection = dataSource.getConnection();
            String schema = connection.getSchema();
            connection.close();
            return schema;
        } catch(SQLException e) {
            return null;
        }
    }

}

