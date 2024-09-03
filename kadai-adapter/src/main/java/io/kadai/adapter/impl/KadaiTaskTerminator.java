package io.kadai.adapter.impl;

import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Terminates KADAI tasks if the associated task in the external system was finished. */
@Component
public class KadaiTaskTerminator {

  private static final Logger LOGGER = LoggerFactory.getLogger(KadaiTaskTerminator.class);

  @Value("${kadai.adapter.run-as.user}")
  protected String runAsUser;

  @Autowired AdapterManager adapterManager;

  @Scheduled(
      fixedRateString =
          "${kadai.adapter.scheduler.run.interval.for.check.finished.referenced.tasks."
              + "in.milliseconds:5000}")
  public void retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks() {

    synchronized (AdapterManager.class) {
      if (!adapterManager.isInitialized()) {
        adapterManager.init();
        return;
      }
    }

    synchronized (KadaiTaskTerminator.class) {
      if (!adapterManager.isInitialized()) {
        return;
      }

      LOGGER.debug(
          "--retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks started-----");

      try {

        for (SystemConnector systemConnector : (adapterManager.getSystemConnectors().values())) {
          UserContext.runAsUser(
              runAsUser,
              () -> {
                retrieveFinishededReferencedTasksAndTerminateCorrespondingKadaiTasks(
                    systemConnector);
                return null;
              });
        }
      } catch (Exception e) {
        LOGGER.warn(
            "caught exception while trying to retrieve "
                + "finished referenced tasks and terminate corresponding kadai tasks",
            e);
      }
    }
  }

  public void retrieveFinishededReferencedTasksAndTerminateCorrespondingKadaiTasks(
      SystemConnector systemConnector) {
    LOGGER.trace(
        "KadaiTaskTerminator."
            + "retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks ENTRY ");

    List<ReferencedTask> tasksSuccessfullyTerminatedInKadai = new ArrayList<>();

    try {
      List<ReferencedTask> kadaiTasksToTerminate =
          systemConnector.retrieveFinishedReferencedTasks();

      for (ReferencedTask referencedTask : kadaiTasksToTerminate) {
        try {
          terminateKadaiTask(referencedTask);
          tasksSuccessfullyTerminatedInKadai.add(referencedTask);
        } catch (TaskTerminationFailedException ex) {
          LOGGER.error(
              "attempted to terminate task with external Id {} and caught exception",
              referencedTask.getId(),
              ex);
          systemConnector.unlockEvent(referencedTask.getOutboxEventId());
        } catch (Exception e) {
          LOGGER.warn(
              "caught unexpected Exception when attempting to start KadaiTask "
                  + "for referencedTask {}",
              referencedTask,
              e);
          systemConnector.unlockEvent(referencedTask.getOutboxEventId());
        }
      }
      systemConnector.kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(kadaiTasksToTerminate);

    } finally {
      LOGGER.trace(
          "KadaiTaskTerminator."
              + "retrieveFinishedReferencedTasksAndTerminateCorrespondingKadaiTasks EXIT ");
    }
  }

  private void terminateKadaiTask(ReferencedTask referencedTask)
      throws TaskTerminationFailedException {
    LOGGER.trace("KadaiTaskTerminator.terminateKadaiTask ENTRY ");
    KadaiConnector kadaiConnector = adapterManager.getKadaiConnector();
    kadaiConnector.terminateKadaiTask(referencedTask);

    LOGGER.trace("KadaiTaskTerminator.terminateKadaiTask EXIT ");
  }
}
