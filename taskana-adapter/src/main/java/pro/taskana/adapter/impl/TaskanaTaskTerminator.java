package pro.taskana.adapter.impl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pro.taskana.adapter.exceptions.TaskTerminationFailedException;
import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;

/** Terminates TASKANA tasks if the associated task in the external system was finished. */
@Component
public class TaskanaTaskTerminator {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskTerminator.class);

  @Value("${taskana.adapter.run-as.user}")
  protected String runAsUser;

  @Autowired AdapterManager adapterManager;

  @Scheduled(
      fixedRateString =
          "${taskana.adapter.scheduler.run.interval.for.check.finished.referenced.tasks."
              + "in.milliseconds:5000}")
  public void retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks() {

    synchronized (AdapterManager.class) {
      if (!adapterManager.isInitialized()) {
        adapterManager.init();
        return;
      }
    }

    synchronized (TaskanaTaskTerminator.class) {
      if (!adapterManager.isInitialized()) {
        return;
      }

      LOGGER.debug(
          "--retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks started-----");

      try {

        for (SystemConnector systemConnector : (adapterManager.getSystemConnectors().values())) {
          UserContext.runAsUser(
              runAsUser,
              () -> {
                retrieveFinishededReferencedTasksAndTerminateCorrespondingTaskanaTasks(
                    systemConnector);
                return null;
              });
        }
      } catch (Exception e) {
        LOGGER.warn(
            "caught exception while trying to retrieve "
                + "finished referenced tasks and terminate corresponding taskana tasks",
            e);
      }
    }
  }

  public void retrieveFinishededReferencedTasksAndTerminateCorrespondingTaskanaTasks(
      SystemConnector systemConnector) {
    LOGGER.trace(
        "TaskanaTaskTerminator."
            + "retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks ENTRY ");

    List<ReferencedTask> tasksSuccessfullyTerminatedInTaskana = new ArrayList<>();

    try {
      List<ReferencedTask> taskanaTasksToTerminate =
          systemConnector.retrieveFinishedReferencedTasks();

      for (ReferencedTask referencedTask : taskanaTasksToTerminate) {
        try {
          terminateTaskanaTask(referencedTask);
          tasksSuccessfullyTerminatedInTaskana.add(referencedTask);
        } catch (TaskTerminationFailedException ex) {
          LOGGER.error(
              "attempted to terminate task with external Id {} and caught exception",
              referencedTask.getId(),
              ex);
          systemConnector.unlockEvent(referencedTask.getOutboxEventId());
        } catch (Exception e) {
          LOGGER.warn(
              "caught unexpected Exception when attempting to start TaskanaTask "
                  + "for referencedTask {}",
              referencedTask,
              e);
          systemConnector.unlockEvent(referencedTask.getOutboxEventId());
        }
      }
      systemConnector.taskanaTasksHaveBeenTerminatedForFinishedReferencedTasks(
          taskanaTasksToTerminate);

    } finally {
      LOGGER.trace(
          "TaskanaTaskTerminator."
              + "retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks EXIT ");
    }
  }

  private void terminateTaskanaTask(ReferencedTask referencedTask)
      throws TaskTerminationFailedException {
    LOGGER.trace("TaskanaTaskTerminator.terminateTaskanaTask ENTRY ");
    TaskanaConnector taskanaConnector = adapterManager.getTaskanaConnector();
    taskanaConnector.terminateTaskanaTask(referencedTask);

    LOGGER.trace("TaskanaTaskTerminator.terminateTaskanaTask EXIT ");
  }
}
