package pro.taskana.adapter.scheduler;

import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.ibatis.session.SqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pro.taskana.Task;
import pro.taskana.adapter.configuration.AdapterSchemaCreator;
import pro.taskana.adapter.configuration.RestClientConfiguration;
import pro.taskana.adapter.exceptions.TaskConversionFailedException;
import pro.taskana.adapter.exceptions.TaskCreationFailedException;
import pro.taskana.adapter.mappings.AdapterMapper;
import pro.taskana.adapter.systemconnector.api.GeneralTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.systemconnector.spi.SystemConnectorProvider;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.adapter.taskanaconnector.spi.TaskanaConnectorProvider;
import pro.taskana.adapter.util.Assert;
import pro.taskana.exceptions.SystemException;
import pro.taskana.impl.util.IdGenerator;
import pro.taskana.impl.util.LoggerUtils;

/**
 * Scheduler for receiving general tasks, completing Taskana tasks and cleaning adapter tables.
 *
 * @author bbr
 */
@Component
public class Scheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    private boolean isRunningCreateTaskanaTasksFromGeneralTasks = false;
    private boolean isRunningCompleteGeneralTasks = false;
    private boolean isRunningCleanupTaskanaAdapterTables = false;
    private boolean isRunningRetrieveFinishedGeneralTasksAndTerminateCorrespondingTaskanaTasks = false;
    private boolean isInitializing = true;
    private boolean isFirstAttemptToProcessFinishedTasks = true;

    @Value("${taskanaAdapter.total.transaction.lifetime.in.seconds:120}")
    private int maximumTotalTransactionLifetime;

    @Value("${taskanaAdapter.scheduler.task.age.for.cleanup.in.hours:600}")
    private long maxTaskAgeBeforeCleanup;

    @Autowired
    private RestClientConfiguration clientCfg;

    @Autowired
    private AdapterMapper timestampMapper;

    @Autowired
    private String schemaName;

    @Autowired
    private  SqlSessionManager sqlSessionManager;

    private Map<String, SystemConnector> systemConnectors;
    private List<TaskanaConnector> taskanaConnectors;

    public Scheduler() {

    }

    private void openConnection() {
        initSqlSession();
        try {
            this.sqlSessionManager.getConnection().setSchema(schemaName);
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

    private void returnConnection() {
        if (this.sqlSessionManager.isManagedSessionStarted()) {
            this.sqlSessionManager.close();
        }
    }

    @Scheduled(cron = "${taskanaAdapter.scheduler.run.interval.for.cleanup.tasks.cron}")
    public void cleanupTaskanaAdapterTables() {
        LOGGER.info("----------cleanupTaskanaAdapterTables started----------------------------");
        if (isRunningCleanupTaskanaAdapterTables) {
            LOGGER.info("----------cleanupTaskanaAdapterTables stopped - another instance is already running ----------------------------");
            return;
        }
        try {
            isRunningCleanupTaskanaAdapterTables = true;
            Instant completedBefore = Instant.now().minus(Duration.ofHours(maxTaskAgeBeforeCleanup));
            timestampMapper.cleanupTasksCompletedBefore(completedBefore);
            timestampMapper.cleanupQueryHistoryEntries(completedBefore);
        } catch (Exception ex) {
            LOGGER.error("Caught {} while cleaning up aged Taskana Adapter tables", ex);
        } finally {
            isRunningCleanupTaskanaAdapterTables = false;
            LOGGER.info("----------cleanupTaskanaAdapterTables finished----------------------------");
        }
    }


    @Scheduled(fixedRateString = "${taskanaAdapter.scheduler.run.interval.for.start.taskana.tasks.in.milliseconds}")
    public void retrieveGeneralTasksAndCreateCorrespondingTaskanaTasks() {
        LOGGER.info("----------retrieveGeneralTasksAndCreateCorrespondingTaskanaTasks started----------------------------");
        if (isRunningCreateTaskanaTasksFromGeneralTasks) {
            LOGGER.info("----------retrieveGeneralTasksAndCreateCorrespondingTaskanaTasks stopped - another instance is already running ----------------------------");
            return;
        }
        try {
            isRunningCreateTaskanaTasksFromGeneralTasks = true;
            for (SystemConnector systemConnector : (systemConnectors.values())) {
                openConnection();
                try {
                    Instant lastRetrieved = timestampMapper.getLatestQueryTimestamp(systemConnector.getSystemURL(), AgentType.START_TASKANA_TASKS);
                    LOGGER.info("lastRetrieved is {}", lastRetrieved);
                    Instant lastRetrievedMinusTransactionDuration = null;
                    if (lastRetrieved != null) {
                        lastRetrievedMinusTransactionDuration = lastRetrieved.minus(Duration.ofSeconds(maximumTotalTransactionLifetime));
                        isInitializing = false;
                    }
                    LOGGER.info("searching for tasks started after {}", lastRetrievedMinusTransactionDuration);
                    timestampMapper.rememberLastQueryTime(IdGenerator.generateWithPrefix("TCA"), Instant.now(), systemConnector.getSystemURL(), AgentType.START_TASKANA_TASKS);

                    List<GeneralTask> candidateTasks = systemConnector.retrieveGeneralTasksStartedAfter(lastRetrievedMinusTransactionDuration);
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("Candidate tasks retrieved from the external system {}", LoggerUtils.listToString(candidateTasks));
                    }
                    List<GeneralTask> tasksToStart = findNewTasksInListOfCandidateTasks(systemConnector.getSystemURL(), candidateTasks);
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("About to create taskana tasks for {} ", LoggerUtils.listToString(tasksToStart.stream().map(GeneralTask::getId)
                            .collect(Collectors.toList())));
                    }

                    for (GeneralTask generalTask : tasksToStart) {
                        createTaskanaTask(generalTask, systemConnector);
                    }
                } finally {
                    returnConnection();
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Caught {} while trying to create Taskana tasks from general tasks", ex);
        } finally {
            isRunningCreateTaskanaTasksFromGeneralTasks = false;
            LOGGER.info("----------createTaskanaTasksFromGeneralTasks finished----------------------------");
        }
    }

    @Scheduled(fixedRateString = "${taskanaAdapter.scheduler.run.interval.for.check.cancelled.general.tasks.in.milliseconds}")
    public void retrieveFinishedGeneralTasksAndTerminateCorrespondingTaskanaTasks() {
        LOGGER.info("----------retrieveFinishedGeneralTasksAndTerminateCorrespondingTaskanaTasks started----------------------------");
        if (isInitializing) {
            LOGGER.info("----------retrieveFinishedGeneralTasksAndTerminateCorrespondingTaskanaTasks stopped - the adapter is still not initialized  ----------------------------");
            return;
        }
        if (isRunningRetrieveFinishedGeneralTasksAndTerminateCorrespondingTaskanaTasks) {
            LOGGER.info("----------retrieveFinishedGeneralTasksAndTerminateCorrespondingTaskanaTasks stopped - another instance is already running ----------------------------");
            return;
        }

        try {

            isRunningRetrieveFinishedGeneralTasksAndTerminateCorrespondingTaskanaTasks = true;
            for (SystemConnector systemConnector : (systemConnectors.values())) {
                openConnection();

                try {
                    Instant lowerThreshold;
                    if (isFirstAttemptToProcessFinishedTasks) {
                        Instant firstCreatedTask = timestampMapper.getOldestTaskCreationTimestamp(systemConnector.getSystemURL());
                        if (firstCreatedTask != null) {
                            lowerThreshold = firstCreatedTask.minus(Duration.ofSeconds(maximumTotalTransactionLifetime));
                            isFirstAttemptToProcessFinishedTasks = false;
                        } else {
                            LOGGER.debug("retrieveFinishedGeneralTasksAndTerminateCorrespondingTaskanaTasks didn't find running tasks -> returning");
                            return;
                        }
                    } else {
                        Instant lastQuerytime = timestampMapper.getLatestQueryTimestamp(systemConnector.getSystemURL(), AgentType.HANDLE_FINISHED_GENERAL_TASKS);
                        LOGGER.debug("lastQueryTime is {}", lastQuerytime);
                        Assert.assertion(lastQuerytime != null, "lastQueryTime != null");
                        lowerThreshold = lastQuerytime.minus(Duration.ofSeconds(maximumTotalTransactionLifetime));
                        isInitializing = false;
                    }

                    timestampMapper.rememberLastQueryTime(IdGenerator.generateWithPrefix("TCA"), Instant.now(), systemConnector.getSystemURL(), AgentType.HANDLE_FINISHED_GENERAL_TASKS);
                    List<GeneralTask> tasksFinishedBySystem = systemConnector.retrieveFinishedTasks(lowerThreshold);
                    List<GeneralTask> taskanaTasksToTerminate = determineTaskanaTasksToTerminate(tasksFinishedBySystem, systemConnector.getSystemURL());
                    for (GeneralTask generalTask : taskanaTasksToTerminate) {
                        terminateTaskanaTask(generalTask);
                    }

                } finally {
                    returnConnection();
                }
            }
        } finally {
            isRunningRetrieveFinishedGeneralTasksAndTerminateCorrespondingTaskanaTasks = false;
            LOGGER.info("----------retrieveFinishedGeneralTasksAndTerminateCorrespondingTaskanaTasks finished processing ----------------------------");
        }
    }

    private void terminateTaskanaTask(GeneralTask generalTask) {
        Assert.assertion(taskanaConnectors.size() == 1, "taskanaConnectors.size() == 1");
        TaskanaConnector taskanaConnector = taskanaConnectors.get(0);
        try {
            taskanaConnector.terminateTaskanaTask(generalTask);
            timestampMapper.registerTaskCompleted(generalTask.getId(), Instant.now());
        } catch (Exception ex) {

        }

    }

    private List<GeneralTask> determineTaskanaTasksToTerminate(List<GeneralTask> tasksFinishedBySystem,
        String systemURL) {
        if (tasksFinishedBySystem == null || tasksFinishedBySystem.isEmpty()) {
            return new ArrayList<GeneralTask>();
        }
        List<String> candidateTaskIds = tasksFinishedBySystem.stream().map(GeneralTask::getId).collect(Collectors.toList());
        List<String> actualTaskIds = timestampMapper.findActiveTasks(systemURL, candidateTaskIds);
        List<GeneralTask> result = tasksFinishedBySystem.stream().filter(t -> actualTaskIds.contains(t.getId())).collect(Collectors.toList());
        return result;
    }

    private void addVariablesToGeneralTask(GeneralTask generalTask, SystemConnector connector) {
        String variables = connector.retrieveVariables(generalTask.getId());
        generalTask.setVariables(variables);
    }

    @Transactional
    public void createTaskanaTask(GeneralTask generalTask, SystemConnector systemConnector) {
        Assert.assertion(taskanaConnectors.size() == 1, "taskanaConnectors.size() == 1");
        TaskanaConnector connector = taskanaConnectors.get(0);
        try {
            generalTask.setSystemURL(systemConnector.getSystemURL());
            addVariablesToGeneralTask(generalTask, systemConnector);
            Task taskanaTask = connector.convertToTaskanaTask(generalTask);
            connector.createTaskanaTask(taskanaTask);

            Instant created;
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                created = formatter.parse(generalTask.getCreated()).toInstant();
            } catch (ParseException excpt) {
                LOGGER.error("Caught {} when trying to parse created timestamp {} ", excpt, generalTask.getCreated());
                created = Instant.now();
            }

            timestampMapper.registerCreatedTask(generalTask.getId(), created, generalTask.getSystemURL());
        } catch (TaskCreationFailedException | TaskConversionFailedException e) {
            LOGGER.error("Caught {} when creating a task in taskana for general task {}", e, generalTask);
        }
    }

    private List<GeneralTask> findNewTasksInListOfCandidateTasks(String systemURL, List<GeneralTask> candidateTasks) {
        if (candidateTasks == null) {
            return new ArrayList<>();
        } else if (candidateTasks.isEmpty()) {
            return candidateTasks;
        }
        List<String> candidateTaskIds = candidateTasks.stream().map(GeneralTask::getId).collect(Collectors.toList());
        List<String> existingTaskIds = timestampMapper.findExistingTaskIds(systemURL, candidateTaskIds);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("findNewTasks: candidate Tasks = \n {}", LoggerUtils.listToString(candidateTaskIds));
            LOGGER.info("findNewTasks: existing  Tasks = \n {}", LoggerUtils.listToString(existingTaskIds));
        }
        List<String> newTaskIds = candidateTaskIds;
        newTaskIds.removeAll(existingTaskIds);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("findNewTasks: to create Tasks = \n {}", LoggerUtils.listToString(newTaskIds));
        }
        return candidateTasks.stream()
            .filter(t -> newTaskIds.contains(t.getId()))
            .collect(Collectors.toList());
    }

    @Scheduled(fixedRateString = "${taskanaAdapter.scheduler.run.interval.for.complete.general.tasks.in.milliseconds}")
    public void retrieveFinishedTaskanaTasksAndCompleteCorrespondingGeneralTasks() {
        LOGGER.info("----------completeGeneralTasks started----------------------------");
        if (isRunningCompleteGeneralTasks) {
            LOGGER.info("----------completeGeneralTasks stopped - another instance is already running ----------------------------");
            return;
        }
        try {
            isRunningCompleteGeneralTasks = true;
            openConnection();
            try {

                Assert.assertion(taskanaConnectors.size() == 1, "taskanaConnectors.size() == 1");
                Instant now = Instant.now();
                Instant lastRetrievedMinusTransactionDuration = timestampMapper.getLatestCompletedTimestamp();
                if (lastRetrievedMinusTransactionDuration == null) {
                    lastRetrievedMinusTransactionDuration = now.minus(Duration.ofDays(1));
                } else {
                    lastRetrievedMinusTransactionDuration = lastRetrievedMinusTransactionDuration.minus(Duration.ofSeconds(maximumTotalTransactionLifetime));
                }
                TaskanaConnector taskanaSystemConnector = taskanaConnectors.get(0);
                List<GeneralTask> candidateTasksCompletedByTaskana = taskanaSystemConnector.retrieveCompletedTaskanaTasks(lastRetrievedMinusTransactionDuration);
                List<GeneralTask> tasksToBeCompletedInExternalSystem = findTasksToBeCompletedInExternalSystem(candidateTasksCompletedByTaskana);
                for (GeneralTask generalTask : tasksToBeCompletedInExternalSystem) {
                    completeGeneralTask(generalTask);
                }
            } finally {
                returnConnection();
            }
        } catch (Exception ex) {
            LOGGER.error("Caught {} while trying to complete general tasks", ex);
        } finally {
            isRunningCompleteGeneralTasks = false;
        }
    }

    @Transactional
    public void completeGeneralTask(GeneralTask generalTask) {
        try {
            SystemConnector connector = systemConnectors.get(generalTask.getSystemURL());
            if (connector != null) {
                timestampMapper.registerTaskCompleted(generalTask.getId(), Instant.now());
                connector.completeGeneralTask(generalTask);
            } else {
                throw new SystemException("couldnt find a connector for systemUrl " + generalTask.getSystemURL());
            }
        } catch (Exception ex) {
            LOGGER.error("Caught {} when attempting to complete general task {}", ex, generalTask);
        }
    }

    private List<GeneralTask> findTasksToBeCompletedInExternalSystem(List<GeneralTask> candidateTasksForCompletion) {
        if (candidateTasksForCompletion.isEmpty()) {
            return candidateTasksForCompletion;
        }
        List<String> candidateTaskIds = candidateTasksForCompletion.stream().map(GeneralTask::getId).collect(Collectors.toList());
        List<String> alreadyCompletedTaskIds = timestampMapper.findAlreadyCompletedTaskIds(candidateTaskIds);
        List<String> taskIdsToBeCompleted = candidateTaskIds;
        taskIdsToBeCompleted.removeAll(alreadyCompletedTaskIds);
        return candidateTasksForCompletion.stream()
            .filter(t -> taskIdsToBeCompleted.contains(t.getId()))
            .collect(Collectors.toList());
    }

    private void initSystemProviders() {
        initSystemConnectors();
        initTaskanaConnectors();
    }

    private void initTaskanaConnectors() {
        taskanaConnectors = new ArrayList<>();
        ServiceLoader<TaskanaConnectorProvider> loader = ServiceLoader.load(TaskanaConnectorProvider.class);
        for (TaskanaConnectorProvider provider : loader) {
            List<TaskanaConnector> connectors = provider.create();
            taskanaConnectors.addAll(connectors);
        }
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

    @PostConstruct
    private void init() {
        initSystemProviders();
        initDatabase();
    }

    private void initDatabase() {
        AdapterSchemaCreator schemaCreator = new AdapterSchemaCreator(clientCfg.dataSource(), schemaName);
        try {
            schemaCreator.run();
        } catch (SQLException ex) {
            LOGGER.error("Caught {} when attempting to initialize the database", ex);
        }
    }

}
