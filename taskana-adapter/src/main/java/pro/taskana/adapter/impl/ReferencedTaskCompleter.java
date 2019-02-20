package pro.taskana.adapter.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pro.taskana.adapter.mappings.AdapterMapper;
import pro.taskana.adapter.scheduler.Scheduler;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.adapter.util.Assert;
import pro.taskana.exceptions.SystemException;

/**
 * Completes ReferencedTasks in the external system after completion of corresponding taskana tasks.
 * @author bbr
 *
 */
@Component
public class ReferencedTaskCompleter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferencedTaskCompleter.class);

    @Autowired
    private AdapterMapper adapterMapper;

    @Value("${taskanaAdapter.total.transaction.lifetime.in.seconds:120}")
    private int maximumTotalTransactionLifetime;

    private Scheduler scheduler;

    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public void retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTask() {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        scheduler.openConnection();
        try {
            List<TaskanaConnector> taskanaConnectors = scheduler.getTaskanaConnectors();
            Assert.assertion(taskanaConnectors.size() == 1, "taskanaConnectors.size() == 1");
            Instant lastRetrievedMinusTransactionDuration = determineStartInstant();
            TaskanaConnector taskanaSystemConnector = taskanaConnectors.get(0);
            List<ReferencedTask> candidateTasksCompletedByTaskana = taskanaSystemConnector.retrieveCompletedTaskanaTasks(lastRetrievedMinusTransactionDuration);
            List<ReferencedTask> tasksToBeCompletedInExternalSystem = findTasksToBeCompletedInExternalSystem(candidateTasksCompletedByTaskana);
            for (ReferencedTask referencedTask : tasksToBeCompletedInExternalSystem) {
                completeReferencedTask(referencedTask);
            }
        } finally {
            LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
            scheduler.returnConnection();
        }
    }

    private Instant determineStartInstant() {
        Instant now = Instant.now();
        Instant lastRetrievedMinusTransactionDuration = adapterMapper.getLatestCompletedTimestamp();
        if (lastRetrievedMinusTransactionDuration == null) {
            lastRetrievedMinusTransactionDuration = now.minus(Duration.ofDays(1));
        } else {
            lastRetrievedMinusTransactionDuration = lastRetrievedMinusTransactionDuration.minus(Duration.ofSeconds(maximumTotalTransactionLifetime));
        }
        return lastRetrievedMinusTransactionDuration;
    }

    private List<ReferencedTask> findTasksToBeCompletedInExternalSystem(List<ReferencedTask> candidateTasksForCompletion) {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        if (candidateTasksForCompletion.isEmpty()) {
            return candidateTasksForCompletion;
        }
        List<String> candidateTaskIds = candidateTasksForCompletion.stream().map(ReferencedTask::getId).collect(Collectors.toList());
        List<String> alreadyCompletedTaskIds = adapterMapper.findAlreadyCompletedTaskIds(candidateTaskIds);
        List<String> taskIdsToBeCompleted = candidateTaskIds;
        taskIdsToBeCompleted.removeAll(alreadyCompletedTaskIds);
        LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        return candidateTasksForCompletion.stream()
            .filter(t -> taskIdsToBeCompleted.contains(t.getId()))
            .collect(Collectors.toList());
    }


    @Transactional
    public void completeReferencedTask(ReferencedTask referencedTask) {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        try {
            SystemConnector connector = scheduler.getSystemConnectors().get(referencedTask.getSystemURL());
            if (connector != null) {
                adapterMapper.registerTaskCompleted(referencedTask.getId(), Instant.now());
                connector.completeReferencedTask(referencedTask);
            } else {
                throw new SystemException("couldnt find a connector for systemUrl " + referencedTask.getSystemURL());
            }
        } catch (Exception ex) {
            LOGGER.error("Caught {} when attempting to complete general task {}", ex, referencedTask);
        }
        LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
    }


}
