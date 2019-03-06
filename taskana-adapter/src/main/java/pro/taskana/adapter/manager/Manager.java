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
import pro.taskana.adapter.impl.TaskanaTaskStarter;
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
public class Manager {

    private static final Logger LOGGER = LoggerFactory.getLogger(Manager.class);
    private static final String ADAPTER_SCHEMA_VERSION = "0.0.1";
    private static boolean isRunningCreateTaskanaTasksFromReferencedTasks = false;
    private static boolean isRunningCompleteReferencedTasks = false;
    private static boolean isRunningCleanupTaskanaAdapterTables = false;
    private static boolean isRunningRetrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks = false;
    private static boolean isInitializing = true;
    private static boolean isSpiInitialized = false;

    @Value("${taskana.adapter.total.transaction.lifetime.in.seconds:120}")
    private int maximumTotalTransactionLifetime;

    @Value("${taskana.adapter.scheduler.task.age.for.cleanup.in.hours:600}")
    private long maxTaskAgeBeforeCleanup;

    @Autowired
    private AdapterMapper adapterMapper;

    @Autowired
    @Qualifier("adapterDataSource")
    private DataSource adapterDataSource;

    @Autowired
    private TaskanaTaskStarter taskanaTaskStarter;
    @Autowired
    private ReferencedTaskCompleter referencedTaskCompleter;
    @Autowired
    private TaskanaTaskTerminator taskanaTaskTerminator;

    @Value("${taskana.adapter.schemaName:TCA}")
    private String adapterSchemaName;

    @Autowired
    private  SqlSessionManager sqlSessionManager;

    private Map<String, SystemConnector> systemConnectors;
    private List<TaskanaConnector> taskanaConnectors;

    public Manager() {
    }

    public void openConnection() {
        initSqlSession();
        try {
            Connection connection = this.sqlSessionManager.getConnection();
            connection.setSchema(adapterSchemaName);
            LOGGER.info("openConnection called by {} sets schemaname to {} ", Thread.currentThread().getStackTrace()[2].getMethodName(), adapterSchemaName );
        } catch (SQLException e) {
            throw new SystemException(
                "Method openConnection() could not open a connection to the database. No schema has been created.",
                e.getCause());
        }
    }

    private void initSqlSession() {
        if (!this.sqlSessionManager.isManagedSessionStarted()) {
            this.sqlSessionManager.startManagedSession();
        }
    }

    public void returnConnection() {
        if (this.sqlSessionManager.isManagedSessionStarted()) {
            this.sqlSessionManager.close();
        }
    }

    public Map<String, SystemConnector> getSystemConnectors() {
        return systemConnectors;
    }

    public List<TaskanaConnector> getTaskanaConnectors() {
        return taskanaConnectors;
    }

    @Transactional(rollbackFor = Exception.class)
    @Scheduled(cron = "${taskana.adapter.scheduler.run.interval.for.cleanup.tasks.cron}")
    public void cleanupTaskanaAdapterTables() {
        if (!isSpiInitialized) {
            return;
        }

        LOGGER.info("----------cleanupTaskanaAdapterTables started----------------------------");
        if (isRunningCleanupTaskanaAdapterTables) {
            LOGGER.info("----------cleanupTaskanaAdapterTables stopped - another instance is already running ----------------------------");
            return;
        }
        openConnection();
        try {
            isRunningCleanupTaskanaAdapterTables = true;
            Instant completedBefore = Instant.now().minus(Duration.ofHours(maxTaskAgeBeforeCleanup));
            adapterMapper.cleanupTasksCompletedBefore(completedBefore);
            adapterMapper.cleanupQueryHistoryEntries(completedBefore);
        } catch (Exception ex) {
            LOGGER.error("Caught {} while cleaning up aged Taskana Adapter tables", ex);
        } finally {
            isRunningCleanupTaskanaAdapterTables = false;
            LOGGER.info("----------cleanupTaskanaAdapterTables finished----------------------------");
            returnConnection();
        }
    }


    @Scheduled(fixedRateString = "${taskana.adapter.scheduler.run.interval.for.start.taskana.tasks.in.milliseconds}")
    public void retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks() {
        if (!isSpiInitialized) {
            initAdapterInfrastructre();
            return;
        }

        LOGGER.info("----------retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks started----------------------------");
        if (isRunningCreateTaskanaTasksFromReferencedTasks) {
            LOGGER.info("----------retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks stopped - another instance is already running ----------------------------");
            return;
        }
        openConnection();
        try {
            isRunningCreateTaskanaTasksFromReferencedTasks = true;
            taskanaTaskStarter.retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks();
        } catch (Exception ex) {
            LOGGER.error("Caught {} while trying to create Taskana tasks from referenced tasks", ex);
        } finally {
            isInitializing = false;
            isRunningCreateTaskanaTasksFromReferencedTasks = false;
            LOGGER.info("----------createTaskanaTasksFromReferencedTasks finished----------------------------");
            returnConnection();
        }
    }

    @Scheduled(fixedRateString = "${taskana.adapter.scheduler.run.interval.for.check.cancelled.referenced.tasks.in.milliseconds}")
    public void retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks() {
        if (!isSpiInitialized) {
            return;
        }

        LOGGER.info("----------retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks started----------------------------");
        if (isInitializing) {
            LOGGER.info("----------retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks stopped - the adapter is still not initialized  ----------------------------");
            return;
        }
        if (isRunningRetrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks) {
            LOGGER.info("----------retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks stopped - another instance is already running ----------------------------");
            return;
        }

        openConnection();
        try {
            isRunningRetrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks = true;
            for (SystemConnector systemConnector : (systemConnectors.values())) {
                taskanaTaskTerminator.retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks(systemConnector);
            }
        } finally {
            isRunningRetrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks = false;
            LOGGER.info("----------retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks finished processing ----------------------------");
            returnConnection();
        }
    }

    @Scheduled(fixedRateString = "${taskana.adapter.scheduler.run.interval.for.complete.referenced.tasks.in.milliseconds}")
    public void retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTasks() {
        if (!isSpiInitialized) {
            return;
        }

        LOGGER.info("----------completeReferencedTasks started----------------------------");
        if (isRunningCompleteReferencedTasks) {
            LOGGER.info("----------completeReferencedTasks stopped - another instance is already running ----------------------------");
            return;
        }
        openConnection();
        try {
            isRunningCompleteReferencedTasks = true;
            referencedTaskCompleter.retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTask();
        } catch (Exception ex) {
            LOGGER.error("Caught {} while trying to complete referenced tasks", ex);
        } finally {
            isRunningCompleteReferencedTasks = false;
            returnConnection();
        }
    }
   
    public static boolean isInitializing() {
        return isInitializing;
    }

    public static void setInitializing(boolean isInitializing) {
        Manager.isInitializing = isInitializing;
    }
    
    public static boolean isSpiInitialized() {
        return isSpiInitialized;
    }
    
    public static void setSpiInitialized(boolean isSpiInitialized) {
        Manager.isSpiInitialized = isSpiInitialized;
    }

    private void initAdapterInfrastructre() {
        if (isSpiInitialized) {
            return;
        }
        LOGGER.info("initAdapterInfrastructure called ");
        initSPIs();
        initDatabase();
        initJobHandlers();
        isSpiInitialized = true;
    }

    private void initJobHandlers() {
        taskanaTaskStarter.setManager(this);
        referencedTaskCompleter.setManager(this);
        taskanaTaskTerminator.setManager(this);
    }

    private void initSPIs() {
        initTaskanaConnectors();
        initSystemConnectors();
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
            AdapterSchemaCreator schemaCreator = new AdapterSchemaCreator(adapterDataSource, adapterSchemaName, ADAPTER_SCHEMA_VERSION);
            schemaCreator.run();
            if (!schemaCreator.isValidSchemaVersion(ADAPTER_SCHEMA_VERSION)) {
                throw new SystemException(
                    "The Database Schema Version doesn't match the expected version " + ADAPTER_SCHEMA_VERSION);
            }
            
            
        } catch (SQLException ex) {
            LOGGER.error("Caught {} when attempting to initialize the database", ex);
        }
    }

}
