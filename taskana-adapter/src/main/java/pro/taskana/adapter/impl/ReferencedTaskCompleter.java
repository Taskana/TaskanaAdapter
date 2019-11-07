package pro.taskana.adapter.impl;

import java.time.Duration;
import java.time.Instant;
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

import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.manager.AgentType;
import pro.taskana.adapter.mappings.AdapterMapper;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.adapter.util.Assert;
import pro.taskana.exceptions.SystemException;
import pro.taskana.impl.util.IdGenerator;

/**
 * Completes ReferencedTasks in the external system after completion of corresponding taskana tasks.
 *
 * @author bbr
 */
@Component
public class ReferencedTaskCompleter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferencedTaskCompleter.class);

    @Value("${taskanaAdapter.total.transaction.lifetime.in.seconds:120}")
    private int maximumTotalTransactionLifetime;

    @Autowired
    private  SqlSessionManager sqlSessionManager;

    @Autowired
    AdapterManager adapterManager;

    private AdapterMapper adapterMapper;

    @PostConstruct
    public void init() {
        adapterMapper = sqlSessionManager.getMapper(AdapterMapper.class);
    }

    @Scheduled(fixedRateString = "${taskana.adapter.scheduler.run.interval.for.complete.referenced.tasks.in.milliseconds}")
    public void retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTasks() {

        synchronized (this.getClass()) {
            if (!adapterManager.isInitialized()) {
                return;
            }

            LOGGER.debug("----------retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTasks started----------------------------");
            adapterManager.openConnection(sqlSessionManager);
            try {
                retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTask();
            } catch (Exception ex) {
                LOGGER.debug("Caught {} while trying to complete referenced tasks", ex);
            } finally {
                LOGGER.debug("----------retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTasks finished----------------------------");
                adapterManager.returnConnection(sqlSessionManager);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTask() {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
        try {
            List<TaskanaConnector> taskanaConnectors = adapterManager.getTaskanaConnectors();
            Assert.assertion(taskanaConnectors.size() == 1, "taskanaConnectors.size() == 1");
            Instant lastCompletedMinusTransactionDuration = determineStartInstant();
            TaskanaConnector taskanaSystemConnector = taskanaConnectors.get(0);
            List<ReferencedTask> candidateTasksCompletedByTaskana = taskanaSystemConnector
                .retrieveCompletedTaskanaTasks(lastCompletedMinusTransactionDuration);
            List<ReferencedTask> tasksToBeCompletedInExternalSystem = findTasksToBeCompletedInExternalSystem(
                candidateTasksCompletedByTaskana);
            for (ReferencedTask referencedTask : tasksToBeCompletedInExternalSystem) {
                completeReferencedTask(referencedTask);
            }
            adapterMapper.rememberLastQueryTime(IdGenerator.generateWithPrefix("TCA"), Instant.now(), "NONE",
                AgentType.HANDLE_FINISHED_TASKANA_TASKS);
        } finally {
            LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(),
                Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    private Instant determineStartInstant() {
        Instant now = Instant.now();
        Instant lastRetrievedMinusTransactionDuration = adapterMapper.getLatestCompletedTimestamp();
        if (lastRetrievedMinusTransactionDuration == null) {
            lastRetrievedMinusTransactionDuration = now.minus(Duration.ofDays(7));
        } else {
            lastRetrievedMinusTransactionDuration = lastRetrievedMinusTransactionDuration
                .minus(Duration.ofSeconds(maximumTotalTransactionLifetime));
        }
        return lastRetrievedMinusTransactionDuration;
    }

    private List<ReferencedTask> findTasksToBeCompletedInExternalSystem(
        List<ReferencedTask> candidateTasksForCompletion) {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());
        if (candidateTasksForCompletion.isEmpty()) {
            return candidateTasksForCompletion;
        }
        List<String> candidateTaskIds = candidateTasksForCompletion.stream()
            .map(ReferencedTask::getId)
            .collect(Collectors.toList());
        List<String> alreadyCompletedTaskIds = adapterMapper.findAlreadyCompletedTaskIds(candidateTaskIds);
        List<String> taskIdsToBeCompleted = candidateTaskIds;
        taskIdsToBeCompleted.removeAll(alreadyCompletedTaskIds);
        LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());
        return candidateTasksForCompletion.stream()
            .filter(t -> taskIdsToBeCompleted.contains(t.getId()))
            .collect(Collectors.toList());
    }


    @Transactional
    public void completeReferencedTask(ReferencedTask referencedTask) {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());
        try {
            SystemConnector connector = adapterManager.getSystemConnectors().get(referencedTask.getSystemURL());
            if (connector != null) {
                adapterMapper.registerTaskCompleted(referencedTask.getId(), Instant.now());
                connector.completeReferencedTask(referencedTask);
            } else {
                throw new SystemException("couldnt find a connector for systemUrl " + referencedTask.getSystemURL());
            }
        } catch (Exception ex) {
            LOGGER.error("Caught {} when attempting to complete referenced task {}", ex, referencedTask);
        }
        LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());
    }

}
