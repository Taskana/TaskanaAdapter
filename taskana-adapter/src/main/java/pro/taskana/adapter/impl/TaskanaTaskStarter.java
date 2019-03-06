package pro.taskana.adapter.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
import pro.taskana.adapter.exceptions.TaskConversionFailedException;
import pro.taskana.adapter.exceptions.TaskCreationFailedException;
import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.manager.AgentType;
import pro.taskana.adapter.mappings.AdapterMapper;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.adapter.util.Assert;
import pro.taskana.impl.util.IdGenerator;
import pro.taskana.impl.util.LoggerUtils;

/**
 * Retrieves tasks in an external system and start corresponding tasks in taskana.
 * @author bbr
 *
 */
@Component
public class TaskanaTaskStarter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskStarter.class);

    @Value("${taskanaAdapter.total.transaction.lifetime.in.seconds:120}")
    private int maximumTotalTransactionLifetime;

    @Autowired
    private  SqlSessionManager sqlSessionManager;

    private AdapterMapper adapterMapper;

    @Autowired
    AdapterManager adapterManager;

    @PostConstruct
    public void init() {
        adapterMapper = sqlSessionManager.getMapper(AdapterMapper.class);         
    }

    @Scheduled(fixedRateString = "${taskana.adapter.scheduler.run.interval.for.start.taskana.tasks.in.milliseconds}")
    public void retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks() {
        synchronized(this.getClass()) {
            if (!adapterManager.isInitialized()) {
                return;
            }

            LOGGER.debug("----------retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks started----------------------------");
            adapterManager.openConnection(sqlSessionManager);
            try {
                retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks();
            } catch (Exception ex) {
                LOGGER.error("Caught {} while trying to create Taskana tasks from referenced tasks", ex);
            } finally {

                LOGGER.debug("----------retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks finished----------------------------");
                adapterManager.returnConnection(sqlSessionManager);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks() {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        for (SystemConnector systemConnector : (adapterManager.getSystemConnectors().values())) {
            try {
                Instant lastRetrievedMinusTransactionDuration = determineStartInstant(systemConnector);

                List<ReferencedTask> candidateTasks = systemConnector.retrieveReferencedTasksStartedAfter(lastRetrievedMinusTransactionDuration);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Candidate tasks retrieved from the external system {}", LoggerUtils.listToString(candidateTasks));
                }
                List<ReferencedTask> tasksToStart = findNewTasksInListOfCandidateTasks(systemConnector.getSystemURL(), candidateTasks);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("About to create taskana tasks for {} ", LoggerUtils.listToString(tasksToStart.stream().map(ReferencedTask::getId)
                        .collect(Collectors.toList())));
                }

                for (ReferencedTask referencedTask : tasksToStart) {
                    createTaskanaTask(referencedTask, adapterManager.getTaskanaConnectors(), systemConnector);
                }
                adapterMapper.rememberLastQueryTime(IdGenerator.generateWithPrefix("TCA"), Instant.now(), systemConnector.getSystemURL(), AgentType.START_TASKANA_TASKS);
            } finally {
                LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
            }
        }
    }

    private Instant determineStartInstant(SystemConnector systemConnector) {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        Instant lastRetrieved = adapterMapper.getLatestQueryTimestamp(systemConnector.getSystemURL(), AgentType.START_TASKANA_TASKS);
        LOGGER.debug("lastRetrieved is {}", lastRetrieved);
        Instant lastRetrievedMinusTransactionDuration = null;
        if (lastRetrieved != null) {
            lastRetrievedMinusTransactionDuration = lastRetrieved.minus(Duration.ofSeconds(maximumTotalTransactionLifetime));
        }
        LOGGER.debug("searching for tasks started after {}", lastRetrievedMinusTransactionDuration);
        return lastRetrievedMinusTransactionDuration;
    }

    private List<ReferencedTask> findNewTasksInListOfCandidateTasks(String systemURL, List<ReferencedTask> candidateTasks) {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        if (candidateTasks == null) {
            return new ArrayList<>();
        } else if (candidateTasks.isEmpty()) {
            return candidateTasks;
        }
        List<String> candidateTaskIds = candidateTasks.stream().map(ReferencedTask::getId).collect(Collectors.toList());
        List<String> existingTaskIds = adapterMapper.findExistingTaskIds(systemURL, candidateTaskIds);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("findNewTasks: candidate Tasks = \n {}", LoggerUtils.listToString(candidateTaskIds));
            LOGGER.debug("findNewTasks: existing  Tasks = \n {}", LoggerUtils.listToString(existingTaskIds));
        }
        List<String> newTaskIds = candidateTaskIds;
        newTaskIds.removeAll(existingTaskIds);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("findNewTasks: to create Tasks = \n {}", LoggerUtils.listToString(newTaskIds));
        }

        LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        return candidateTasks.stream()
            .filter(t -> newTaskIds.contains(t.getId()))
            .collect(Collectors.toList());
    }

    @Transactional
    public void createTaskanaTask(ReferencedTask referencedTask, List<TaskanaConnector> taskanaConnectors, SystemConnector systemConnector) {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        Assert.assertion(taskanaConnectors.size() == 1, "taskanaConnectors.size() == 1");
        TaskanaConnector connector = taskanaConnectors.get(0);
        try {
            referencedTask.setSystemURL(systemConnector.getSystemURL());
            addVariablesToReferencedTask(referencedTask, systemConnector);
            Task taskanaTask = connector.convertToTaskanaTask(referencedTask);
            connector.createTaskanaTask(taskanaTask);

            Instant created;
            try {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                created = formatter.parse(referencedTask.getCreated()).toInstant();
            } catch (ParseException excpt) {
                LOGGER.error("Caught {} when trying to parse created timestamp {} ", excpt, referencedTask.getCreated());
                created = Instant.now();
            }

            adapterMapper.registerCreatedTask(referencedTask.getId(), created, referencedTask.getSystemURL());
        } catch (TaskCreationFailedException | TaskConversionFailedException e) {
            LOGGER.error("Caught {} when creating a task in taskana for referenced task {}", e, referencedTask);
        }
        LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    private void addVariablesToReferencedTask(ReferencedTask referencedTask, SystemConnector connector) {
        String variables = connector.retrieveVariables(referencedTask.getId());
        referencedTask.setVariables(variables);
    }

}
