package pro.taskana.adapter.scheduler;

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

import org.apache.ibatis.session.SqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pro.taskana.adapter.configuration.AdapterConfiguration;
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

/**
 * Scheduler for receiving general tasks, completing Taskana tasks and cleaning adapter tables.
 *
 * @author bbr
 */
@Component
public class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    private static boolean isRunningCreateTaskanaTasksFromReferencedTasks = false;
    private static boolean isRunningCompleteReferencedTasks = false;
    private static boolean isRunningCleanupTaskanaAdapterTables = false;
    private static boolean isRunningRetrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks = false;
    private static boolean isInitializing = true;

    @Value("${taskana.adapter.total.transaction.lifetime.in.seconds:120}")
    private int maximumTotalTransactionLifetime;

    @Value("${taskana.adapter.scheduler.task.age.for.cleanup.in.hours:600}")
    private long maxTaskAgeBeforeCleanup;

    @Autowired
    private AdapterConfiguration adapterConfiguration;

    @Autowired
    private AdapterMapper adapterMapper;


    @Autowired
    private TaskanaTaskStarter taskanaTaskStarter;
    @Autowired
    private ReferencedTaskCompleter referencedTaskCompleter;
    @Autowired
    private TaskanaTaskTerminator taskanaTaskTerminator;

    @Autowired
    private String schemaName;

    @Autowired
    private  SqlSessionManager sqlSessionManager;

    private Map<String, SystemConnector> systemConnectors;
    private List<TaskanaConnector> taskanaConnectors;

    public Scheduler() {
    }

    public void openConnection() {
        initSqlSession();
        try {
            Connection connection = this.sqlSessionManager.getConnection();
            TaskanaEngineImpl.setSchemaToConnection(connection, schemaName);
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
            LOGGER.error("Caught {} while trying to create Taskana tasks from general tasks", ex);
        } finally {
            isInitializing = false;
            isRunningCreateTaskanaTasksFromReferencedTasks = false;
            LOGGER.info("----------createTaskanaTasksFromReferencedTasks finished----------------------------");
            returnConnection();
        }
    }


    @Scheduled(fixedRateString = "${taskana.adapter.scheduler.run.interval.for.check.cancelled.general.tasks.in.milliseconds}")
    public void retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks() {
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

    @Scheduled(fixedRateString = "${taskana.adapter.scheduler.run.interval.for.complete.general.tasks.in.milliseconds}")
    public void retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTasks() {
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
            LOGGER.error("Caught {} while trying to complete general tasks", ex);
        } finally {
            isRunningCompleteReferencedTasks = false;
            returnConnection();
        }
    }

    @PostConstruct
    private void init() {
        initSPIs();
        initDatabase();
        initJobHandlers();
    }


    private void initJobHandlers() {
        taskanaTaskStarter.setScheduler(this);
        referencedTaskCompleter.setScheduler(this);
        taskanaTaskTerminator.setScheduler(this);
    }

    private void initSPIs() {
        initSystemConnectors();
        initTaskanaConnectors();
    }

    private void initSystemConnectors() {
        systemConnectors = new HashMap<>();
        ServiceLoader<SystemConnectorProvider> loader = ServiceLoader.load(SystemConnectorProvider.class);
        for (SystemConnectorProvider provider : loader) {
            List<SystemConnector> connectors = provider.create();
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
            taskanaConnectors.addAll(connectors);
        }
    }

    private void initDatabase() {
        AdapterSchemaCreator schemaCreator = new AdapterSchemaCreator(adapterConfiguration.dataSource(), schemaName);
        try {
            schemaCreator.run();
        } catch (SQLException ex) {
            LOGGER.error("Caught {} when attempting to initialize the database", ex);
        }
    }

}
