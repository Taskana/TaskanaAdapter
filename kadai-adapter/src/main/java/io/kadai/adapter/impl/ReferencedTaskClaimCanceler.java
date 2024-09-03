package io.kadai.adapter.impl;

import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.common.api.exceptions.SystemException;
import io.kadai.task.api.CallbackState;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Cancels claims of ReferencedTasks in external system that have been cancel claimed in KADAI. */
@Component
public class ReferencedTaskClaimCanceler {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencedTaskClaimCanceler.class);

  @Value("${kadai.adapter.run-as.user}")
  protected String runAsUser;

  @Autowired AdapterManager adapterManager;

  @Scheduled(
      fixedRateString =
          "${kadai.adapter.scheduler.run.interval.for.cancel.claim.referenced.tasks."
              + "in.milliseconds:5000}")
  @Transactional
  public void retrieveCancelledClaimKadaiTasksAndCancelClaimCorrespondingReferencedTasks() {

    synchronized (ReferencedTaskClaimCanceler.class) {
      if (!adapterManager.isInitialized()) {
        return;
      }

      LOGGER.debug(
          "----retrieveCancelledClaimKadaiTasksAndCancelCorrespondingReferencedTasks started--");
      try {
        UserContext.runAsUser(
            runAsUser,
            () -> {
              retrieveCancelledClaimKadaiTasksAndCancelClaimCorrespondingReferencedTask();
              return null;
            });
      } catch (Exception ex) {
        LOGGER.debug("Caught exception while trying to cancel claim referenced tasks", ex);
      }
    }
  }

  public void retrieveCancelledClaimKadaiTasksAndCancelClaimCorrespondingReferencedTask() {

    try {
      KadaiConnector kadaiSystemConnector = adapterManager.getKadaiConnector();

      List<ReferencedTask> tasksCancelClaimedByKadai =
          kadaiSystemConnector.retrieveCancelledClaimKadaiTasksAsReferencedTasks();
      List<ReferencedTask> tasksCancelClaimedInExternalSystem =
          cancelClaimReferencedTasksInExternalSystem(tasksCancelClaimedByKadai);

      kadaiSystemConnector.changeTaskCallbackState(
          tasksCancelClaimedInExternalSystem, CallbackState.CALLBACK_PROCESSING_REQUIRED);
    } finally {
      LOGGER.trace(
          "ReferencedTaskClaimer."
              + "retrieveCancelledClaimKadaiTasksAndCancel"
              + "ClaimCorrespondingReferencedTask EXIT ");
    }
  }

  private List<ReferencedTask> cancelClaimReferencedTasksInExternalSystem(
      List<ReferencedTask> tasksUnclaimedByKadai) {

    List<ReferencedTask> tasksCancelClaimedInExternalSystem = new ArrayList<>();
    for (ReferencedTask referencedTaskToCancelClaim : tasksUnclaimedByKadai) {
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
