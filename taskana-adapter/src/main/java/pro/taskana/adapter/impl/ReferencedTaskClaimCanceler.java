package pro.taskana.adapter.impl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.common.api.exceptions.SystemException;
import pro.taskana.task.api.CallbackState;

/**
 * Cancels claims of ReferencedTasks in external system that have been cancel claimed in TASKANA.
 */
@Component
public class ReferencedTaskClaimCanceler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencedTaskClaimCanceler.class);

  @Value("${taskana.adapter.run-as.user}")
  protected String runAsUser;

  @Autowired AdapterManager adapterManager;

  @Scheduled(
      fixedRateString =
          "${taskana.adapter.scheduler.run.interval.for.cancel.claim.referenced.tasks."
              + "in.milliseconds:5000}")
  public void retrieveCancelledClaimTaskanaTasksAndCancelClaimCorrespondingReferencedTasks() {

    synchronized (ReferencedTaskClaimCanceler.class) {
      if (!adapterManager.isInitialized()) {
        return;
      }

      LOGGER.debug(
          "----retrieveCancelledClaimTaskanaTasksAndCancelCorrespondingReferencedTasks started--");
      try {
        UserContext.runAsUser(
            runAsUser,
            () -> {
              retrieveCancelledClaimTaskanaTasksAndCancelClaimCorrespondingReferencedTask();
              return null;
            });
      } catch (Exception ex) {
        LOGGER.debug("Caught exception while trying to cancel claim referenced tasks", ex);
      }
    }
  }

  public void retrieveCancelledClaimTaskanaTasksAndCancelClaimCorrespondingReferencedTask() {

    try {
      TaskanaConnector taskanaSystemConnector = adapterManager.getTaskanaConnector();

      List<ReferencedTask> tasksCancelClaimedByTaskana =
          taskanaSystemConnector.retrieveCancelledClaimTaskanaTasksAsReferencedTasks();
      List<ReferencedTask> tasksCancelClaimedInExternalSystem =
          cancelClaimReferencedTasksInExternalSystem(tasksCancelClaimedByTaskana);

      taskanaSystemConnector.changeTaskCallbackState(
          tasksCancelClaimedInExternalSystem, CallbackState.CALLBACK_PROCESSING_REQUIRED);
    } finally {
      LOGGER.trace(
          "ReferencedTaskClaimer."
              + "retrieveCancelledClaimTaskanaTasksAndCancel"
              + "ClaimCorrespondingReferencedTask EXIT ");
    }
  }

  private List<ReferencedTask> cancelClaimReferencedTasksInExternalSystem(
      List<ReferencedTask> tasksUnclaimedByTaskana) {

    List<ReferencedTask> tasksCancelClaimedInExternalSystem = new ArrayList<>();
    for (ReferencedTask referencedTaskToCancelClaim : tasksUnclaimedByTaskana) {
      if (cancelClaimReferencedTask(referencedTaskToCancelClaim)) {
        tasksCancelClaimedInExternalSystem.add(referencedTaskToCancelClaim);
      }
    }
    return tasksCancelClaimedInExternalSystem;
  }

  private boolean cancelClaimReferencedTask(ReferencedTask referencedTask) {

    LOGGER.trace(
        "ENTRY to ReferencedTaskClaimer.cancelClaimReferencedTask, TaskId = {} ",
        referencedTask.getId());
    boolean success = false;
    try {
      SystemConnector connector =
          adapterManager.getSystemConnectors().get(referencedTask.getSystemUrl());
      if (connector != null) {
        connector.cancelClaimReferencedTask(referencedTask);
        success = true;
      } else {
        throw new SystemException(
            "couldnt find a connector for systemUrl " + referencedTask.getSystemUrl());
      }
    } catch (Exception ex) {
      LOGGER.error(
          "Caught {} when attempting to cancel the claim for a referenced task {}",
          ex,
          referencedTask);
    }
    LOGGER.trace(
        "Exit from ReferencedTaskClaimerCanceller.cancelClaimReferencedTask, Success = {} ",
        success);
    return success;
  }
}
