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

/** Claims ReferencedTasks in external system that have been claimed in KADAI. */
@Component
public class ReferencedTaskClaimer {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReferencedTaskClaimer.class);

  @Value("${kadai.adapter.run-as.user}")
  protected String runAsUser;

  @Autowired AdapterManager adapterManager;

  @Scheduled(
      fixedRateString =
          "${kadai.adapter.scheduler.run.interval.for.claim.referenced.tasks."
              + "in.milliseconds:5000}")
  @Transactional
  public void retrieveClaimedKadaiTasksAndClaimCorrespondingReferencedTasks() {

    synchronized (ReferencedTaskClaimer.class) {
      if (!adapterManager.isInitialized()) {
        return;
      }

      LOGGER.debug(
          "--retrieveClaimedKadaiTasksAndClaimCorrespondingReferencedTasks started-----------");
      try {
        UserContext.runAsUser(
            runAsUser,
            () -> {
              retrieveClaimedKadaiTasksAndClaimCorrespondingReferencedTask();
              return null;
            });
      } catch (Exception ex) {
        LOGGER.debug("Caught exception while trying to claim referenced tasks", ex);
      }
    }
  }

  private void retrieveClaimedKadaiTasksAndClaimCorrespondingReferencedTask() {
    LOGGER.trace(
        "ReferencedTaskClaimer."
            + "retrieveClaimedKadaiTasksAndClaimCorrespondingReferencedTask ENTRY");
    try {
      KadaiConnector kadaiSystemConnector = adapterManager.getKadaiConnector();
      List<ReferencedTask> tasksClaimedByKadai =
          kadaiSystemConnector.retrieveClaimedKadaiTasksAsReferencedTasks();
      List<ReferencedTask> tasksClaimedInExternalSystem =
          claimReferencedTasksInExternalSystem(tasksClaimedByKadai);

      kadaiSystemConnector.changeTaskCallbackState(
          tasksClaimedInExternalSystem, CallbackState.CLAIMED);

    } finally {
      LOGGER.trace(
          "ReferencedTaskClaimer."
              + "retrieveClaimedKadaiTasksAndClaimCorrespondingReferencedTask EXIT ");
    }
  }

  private List<ReferencedTask> claimReferencedTasksInExternalSystem(
      List<ReferencedTask> tasksClaimedByKadai) {

    List<ReferencedTask> tasksClaimedInExternalSystem = new ArrayList<>();
    for (ReferencedTask referencedTask : tasksClaimedByKadai) {
      if (claimReferencedTask(referencedTask)) {
        tasksClaimedInExternalSystem.add(referencedTask);
      }
    }
    return tasksClaimedInExternalSystem;
  }

  private boolean claimReferencedTask(ReferencedTask referencedTask) {
    LOGGER.trace(
        "ENTRY to ReferencedTaskClaimer.claimReferencedTask, TaskId = {} ", referencedTask.getId());
    boolean success = false;
    try {
      SystemConnector connector =
          adapterManager.getSystemConnectors().get(referencedTask.getSystemUrl());
      if (connector != null) {
        connector.claimReferencedTask(referencedTask);
        success = true;
      } else {
        throw new SystemException(
            "couldnt find a connector for systemUrl " + referencedTask.getSystemUrl());
      }
    } catch (Exception ex) {
      LOGGER.error("Caught {} when attempting to claim referenced task {}", ex, referencedTask);
    }
    LOGGER.trace("Exit from ReferencedTaskClaimer.claimReferencedTask, Success = {} ", success);
    return success;
  }
}
