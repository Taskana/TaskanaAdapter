package pro.taskana.adapter.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pro.taskana.adapter.configuration.AdapterSchemaCreator;
import pro.taskana.adapter.impl.ReferencedTaskCompleter;
import pro.taskana.adapter.impl.TaskanaTaskTerminator;
import pro.taskana.adapter.mappings.AdapterMapper;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.systemconnector.spi.SystemConnectorProvider;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.adapter.taskanaconnector.spi.TaskanaConnectorProvider;
import pro.taskana.exceptions.SystemException;
import pro.taskana.impl.TaskanaEngineImpl;
import pro.taskana.impl.util.LoggerUtils;

/**
 * Scheduler for receiving referenced tasks, completing Taskana tasks and cleaning adapter tables.
 *
 * @author bbr
 */
@Component
public class AdapterManager {

    private final Logger LOGGER = LoggerFactory.getLogger(AdapterManager.class);
    private static final String ADAPTER_SCHEMA_VERSION = "0.0.1";
    private boolean isSpiInitialized = false;
    private boolean isDatabaseInitialized = false;

    @Autowired
    @Qualifier("adapterDataSource")
    private DataSource adapterDataSource;

    @Value("${taskana.adapter.schemaName:TCA}")
    private String adapterSchemaName;

   private Map<String, SystemConnector> systemConnectors;
    private List<TaskanaConnector> taskanaConnectors;
        
    @PostConstruct
    public void init() {
        initDatabase();
        isDatabaseInitialized = true;
    }
    
    public void openConnection(SqlSessionManager sqlSessionManager) {
        if (!sqlSessionManager.isManagedSessionStarted()) {
            sqlSessionManager.startManagedSession();
        }
        try {
            Connection connection = sqlSessionManager.getConnection();
            connection.setSchema(adapterSchemaName);
            LOGGER.debug("openConnection called by {} sets schemaname to {} ", Thread.currentThread().getStackTrace()[2].getMethodName(), adapterSchemaName );
        } catch (SQLException e) {
            throw new SystemException(
                "Method openConnection() could not open a connection to the database. No schema has been created.",
                e.getCause());
        }
    }

    public void returnConnection(SqlSessionManager sqlSessionManager) {
        if (sqlSessionManager.isManagedSessionStarted()) {
            sqlSessionManager.close();
        }
    }

    public Map<String, SystemConnector> getSystemConnectors() {
        return systemConnectors;
    }

    public List<TaskanaConnector> getTaskanaConnectors() {
        return taskanaConnectors;
    }

    public boolean isInitialized() {
        return isSpiInitialized && isDatabaseInitialized;
    }

    public boolean isSpiInitialized() {
        return isSpiInitialized;
    }

   
    public void initSPIs() {
        if (isSpiInitialized) {
            return;
        }
        LOGGER.debug("initAdapterInfrastructure called ");
        initTaskanaConnectors();
        initSystemConnectors();
        isSpiInitialized = true;
    }

   private void initSystemConnectors() {
        systemConnectors = new HashMap<>();
        ServiceLoader<SystemConnectorProvider> loader = ServiceLoader.load(SystemConnectorProvider.class);
        for (SystemConnectorProvider provider : loader) {
            List<SystemConnector> connectors = provider.create();
            LOGGER.info("initialized system connectors {} " ,LoggerUtils.listToString(connectors));
            for (SystemConnector conn : connectors) {
                systemConnectors.put(conn.getSystemURL(), conn);
            }
        }
    }

    private void initTaskanaConnectors() {
        taskanaConnectors = new ArrayList<>();
        ServiceLoader<TaskanaConnectorProvider> loader = ServiceLoader.load(TaskanaConnectorProvider.class);
        for (TaskanaConnectorProvider provider : loader) {
            List<TaskanaConnector> connectors = provider.create();
            LOGGER.info("initialized taskana connectors {} " ,LoggerUtils.listToString(connectors));
            taskanaConnectors.addAll(connectors);
        }
    }

    private void initDatabase() {
        try {
            Connection connection = adapterDataSource.getConnection();
            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            if (TaskanaEngineImpl.isPostgreSQL(databaseProductName)) {
                this.adapterSchemaName = this.adapterSchemaName.toLowerCase();
            } else {
                this.adapterSchemaName = this.adapterSchemaName.toUpperCase();
            }
            LOGGER.info("starting AdapterSchemaCreator");
            AdapterSchemaCreator schemaCreator = new AdapterSchemaCreator(adapterDataSource, adapterSchemaName);
            schemaCreator.run();
            LOGGER.info("AdapterSchemaCreator is done");

            if (!schemaCreator.isValidSchemaVersion(ADAPTER_SCHEMA_VERSION)) {
                throw new SystemException(
                    "The Database Schema Version doesn't match the expected version " + ADAPTER_SCHEMA_VERSION);
            }
            
            
        } catch (SQLException ex) {
            LOGGER.error("Caught {} when attempting to initialize the database", ex);
        }
    }

}
