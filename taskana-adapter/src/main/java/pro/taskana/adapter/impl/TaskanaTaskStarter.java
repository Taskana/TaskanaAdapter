package pro.taskana.adapter.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pro.taskana.Task;
import pro.taskana.adapter.exceptions.TaskConversionFailedException;
import pro.taskana.adapter.exceptions.TaskCreationFailedException;
import pro.taskana.adapter.mappings.AdapterMapper;
import pro.taskana.adapter.scheduler.AgentType;
import pro.taskana.adapter.scheduler.Scheduler;
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

    private Scheduler scheduler;

    @Value("${taskanaAdapter.total.transaction.lifetime.in.seconds:120}")
    private int maximumTotalTransactionLifetime;


    @Autowired
    private AdapterMapper adapterMapper;

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Transactional(rollbackFor = Exception.class)
    public void retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks() {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        for (SystemConnector systemConnector : (scheduler.getSystemConnectors().values())) {
            try {
                Instant lastRetrievedMinusTransactionDuration = determineStartInstant(systemConnector);

                List<ReferencedTask> candidateTasks = systemConnector.retrieveReferencedTasksStartedAfter(lastRetrievedMinusTransactionDuration);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Candidate tasks retrieved from the external system {}", LoggerUtils.listToString(candidateTasks));
                }
                List<ReferencedTask> tasksToStart = findNewTasksInListOfCandidateTasks(systemConnector.getSystemURL(), candidateTasks);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("About to create taskana tasks for {} ", LoggerUtils.listToString(tasksToStart.stream().map(ReferencedTask::getId)
                        .collect(Collectors.toList())));
                }

                for (ReferencedTask referencedTask : tasksToStart) {
                    createTaskanaTask(referencedTask, scheduler.getTaskanaConnectors(), systemConnector);
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
        LOGGER.info("lastRetrieved is {}", lastRetrieved);
        Instant lastRetrievedMinusTransactionDuration = null;
        if (lastRetrieved != null) {
            lastRetrievedMinusTransactionDuration = lastRetrieved.minus(Duration.ofSeconds(maximumTotalTransactionLifetime));
        }
        LOGGER.info("searching for tasks started after {}", lastRetrievedMinusTransactionDuration);
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
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("findNewTasks: candidate Tasks = \n {}", LoggerUtils.listToString(candidateTaskIds));
            LOGGER.info("findNewTasks: existing  Tasks = \n {}", LoggerUtils.listToString(existingTaskIds));
        }
        List<String> newTaskIds = candidateTaskIds;
        newTaskIds.removeAll(existingTaskIds);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("findNewTasks: to create Tasks = \n {}", LoggerUtils.listToString(newTaskIds));
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
