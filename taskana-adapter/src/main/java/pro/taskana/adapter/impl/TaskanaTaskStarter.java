package pro.taskana.adapter.impl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pro.taskana.adapter.exceptions.ReferencedTaskDoesNotExistInExternalSystemException;
import pro.taskana.adapter.exceptions.TaskCreationFailedException;
import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.task.api.exceptions.TaskAlreadyExistException;
import pro.taskana.task.api.models.Task;

/** Retrieves tasks in an external system and starts corresponding tasks in TASKANA. */
@Component
public class TaskanaTaskStarter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskStarter.class);

  @Value("${taskana.adapter.run-as.user}")
  protected String runAsUser;

  @Autowired AdapterManager adapterManager;

  @Scheduled(
      fixedRateString =
          "${taskana.adapter.scheduler.run.interval.for.start.taskana.tasks.in.milliseconds:5000}")
  public void retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks() {
    if (!adapterIsInitialized()) {
      return;
    }
    synchronized (TaskanaTaskStarter.class) {
      if (!adapterManager.isInitialized()) {
        return;
      }

      LOGGER.debug(
          "-retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks started---------------");
      try {
        UserContext.runAsUser(
            runAsUser,
            () -> {
              retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks();
              return null;
            });
      } catch (Exception ex) {
        LOGGER.error(
            "Caught exception while trying to create Taskana tasks from referenced tasks", ex);
      }
    }
  }

  public void retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks() {
    LOGGER.trace(
        "TaskanaTaskStarter.retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks ENTRY ");
    for (SystemConnector systemConnector : (adapterManager.getSystemConnectors().values())) {
      try {

        List<ReferencedTask> tasksToStart = systemConnector.retrieveNewStartedReferencedTasks();

        List<ReferencedTask> newCreatedTasksInTaskana =
            createAndStartTaskanaTasks(systemConnector, tasksToStart);

        systemConnector.taskanaTasksHaveBeenCreatedForNewReferencedTasks(newCreatedTasksInTaskana);
      } finally {
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
              String.format(
                  "TaskanaTaskStarter.retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks "
                      + "Leaving handling of new tasks for System Connector %s",
                  systemConnector.getSystemUrl()));
        }
      }
    }
  }

  public void createTaskanaTask(
      ReferencedTask referencedTask, TaskanaConnector connector, SystemConnector systemConnector)
      throws TaskCreationFailedException {
    LOGGER.trace("TaskanaTaskStarter.createTaskanaTask ENTRY ");
    referencedTask.setSystemUrl(systemConnector.getSystemUrl());
    try {
      addVariablesToReferencedTask(referencedTask, systemConnector);
      Task taskanaTask = connector.convertToTaskanaTask(referencedTask);
      connector.createTaskanaTask(taskanaTask);
    } catch (ReferencedTaskDoesNotExistInExternalSystemException e) {
      LOGGER.warn(
          "While attempting to retrieve variables for task {} caught ", referencedTask.getId(), e);
    }

    LOGGER.trace("TaskanaTaskStarter.createTaskanaTask EXIT ");
  }

  private List<ReferencedTask> createAndStartTaskanaTasks(
      SystemConnector systemConnector, List<ReferencedTask> tasksToStart) {
    List<ReferencedTask> newCreatedTasksInTaskana = new ArrayList<>();
    for (ReferencedTask referencedTask : tasksToStart) {
      try {
        createTaskanaTask(referencedTask, adapterManager.getTaskanaConnector(), systemConnector);
        newCreatedTasksInTaskana.add(referencedTask);
      } catch (TaskCreationFailedException e) {
        if (e.getCause() instanceof TaskAlreadyExistException) {
          newCreatedTasksInTaskana.add(referencedTask);
        } else {
          LOGGER.warn(
              "caught Exception when attempting to start TaskanaTask for referencedTask {}",
              referencedTask,
              e);
        }
      } catch (Exception e) {
        LOGGER.warn(
            "caught Exception when attempting to start TaskanaTask for referencedTask {}",
            referencedTask,
            e);
      }
    }
    return newCreatedTasksInTaskana;
  }

  private void addVariablesToReferencedTask(
      ReferencedTask referencedTask, SystemConnector connector) {
    if (referencedTask.getVariables() == null) {
      String variables = connector.retrieveReferencedTaskVariables(referencedTask.getId());
      referencedTask.setVariables(variables);
    }
  }

  private boolean adapterIsInitialized() {
    synchronized (AdapterManager.class) {
      if (!adapterManager.isInitialized()) {
        adapterManager.init();
        return false;
      }
      return true;
    }
  }
}
