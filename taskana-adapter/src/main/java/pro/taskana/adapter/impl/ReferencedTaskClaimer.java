package pro.taskana.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pro.taskana.CallbackState;
import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.exceptions.SystemException;
/**
 * Periodically polls claimed TASKANA tasks and claims them in camunda.
 *
 * @author jhe
 */
@Component
public class ReferencedTaskClaimer {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReferencedTaskClaimer.class);

    @Autowired
    AdapterManager adapterManager;

    @Scheduled(
        fixedRateString = "${taskana.adapter.scheduler.run.interval.for.claimed.referenced.tasks.in.milliseconds}")
    public void retrieveClaimedTaskanaTasksAndClaimCorrespondingReferencedTasks() {

        synchronized (ReferencedTaskClaimer.class) {
            if (!adapterManager.isInitialized()) {
                return;
            }

            LOGGER.debug(
                "----------retrieveClaimedTaskanaTasksAndClaimCorrespondingReferencedTasks started----------------------------");
            try {
                retrieveClaimedTaskanaTasksAndClaimCorrespondingReferencedTask();
            } catch (Exception ex) {
                LOGGER.debug("Caught {} while trying to claim referenced tasks", ex);
            }
        }
    }


    private void retrieveClaimedTaskanaTasksAndClaimCorrespondingReferencedTask() {

        LOGGER
            .trace("ReferencedTaskClaimer.retrieveClaimedTaskanaTasksAndClaimCorrespondingReferencedTask ENTRY");
        try {
            TaskanaConnector taskanaSystemConnector = adapterManager.getTaskanaConnector();

            List<ReferencedTask> tasksClaimedByTaskana = taskanaSystemConnector.retrieveClaimedTaskanaTasksAsReferencedTasks();
            List<ReferencedTask> tasksClaimedInExternalSystem = claimReferencedTasksInExternalSystem(
                tasksClaimedByTaskana);

            taskanaSystemConnector.changeReferencedTaskCallbackState(tasksClaimedInExternalSystem,
                CallbackState.CLAIMED);
        } finally {
            LOGGER.trace(
                "ReferencedTaskClaimer.retrieveClaimedTaskanaTasksAndClaimCorrespondingReferencedTask EXIT ");
        }

    }

    private List<ReferencedTask> claimReferencedTasksInExternalSystem(List<ReferencedTask> tasksClaimedByTaskana) {

        List<ReferencedTask> tasksClaimedInExternalSystem = new ArrayList<>();
        for (ReferencedTask referencedTask : tasksClaimedByTaskana) {
            if (claimReferencedTask(referencedTask)) {
                tasksClaimedInExternalSystem.add(referencedTask);
            }
        }
        return tasksClaimedInExternalSystem;

    }

    private boolean claimReferencedTask(ReferencedTask referencedTask) {
        LOGGER.trace("ENTRY to ReferencedTaskClaimer.claimReferencedTask, TaskId = {} ", referencedTask.getId());
        boolean success = false;
        try {
            SystemConnector connector = adapterManager.getSystemConnectors().get(referencedTask.getSystemURL());
            if (connector != null) {
                connector.claimReferencedTask(referencedTask);
                success = true;
            } else {
                throw new SystemException("couldnt find a connector for systemUrl " + referencedTask.getSystemURL());
            }
        } catch (Exception ex) {
            LOGGER.error("Caught {} when attempting to claim referenced task {}", ex, referencedTask);
        }
        LOGGER.trace("Exit from ReferencedTaskClaimer.claimReferencedTask, Success = {} ", success);
        return success;
    }

}


