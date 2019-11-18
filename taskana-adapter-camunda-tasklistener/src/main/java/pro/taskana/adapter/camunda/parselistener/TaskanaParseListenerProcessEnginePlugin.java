package pro.taskana.adapter.camunda.parselistener;

import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParseListener;
import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TaskanaParseListenerProcessEnginePlugin extends AbstractProcessEnginePlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaParseListenerProcessEnginePlugin.class);

    private static final String SQL_CREATE_TASKANA_SCHEMA = "CREATE SCHEMA IF NOT EXISTS taskana_tables";

    private static final String SQL_CREATE_SEQUENCE = "CREATE SEQUENCE IF NOT EXISTS taskana_tables.event_store_id_seq"
            + " INCREMENT 1"
            + " START 1"
            + " MINVALUE 1"
            + " MAXVALUE 2147483647"
            + " CACHE 1 ";

    private static final String SQL_CREATE_EVENT_STORE = "CREATE TABLE IF NOT EXISTS taskana_tables.event_store"
            + "("
            + " ID integer NOT NULL DEFAULT nextval('taskana_tables.event_store_id_seq'::regclass),"
            + " TYPE text COLLATE pg_catalog.\"default\","
            + " CREATED timestamp(4) without time zone,"
            + " PAYLOAD text COLLATE pg_catalog.\"default\","
            + " CONSTRAINT event_store_pkey PRIMARY KEY (id)"
            + ")";

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

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.execute(SQL_CREATE_TASKANA_SCHEMA);
            statement.execute(SQL_CREATE_SEQUENCE);
            statement.execute(SQL_CREATE_EVENT_STORE);

        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to initialize the outbox-table", e);
        }
    }

}

