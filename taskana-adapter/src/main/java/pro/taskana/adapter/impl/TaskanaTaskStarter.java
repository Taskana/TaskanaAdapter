package pro.taskana.adapter.impl;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pro.taskana.Task;
import pro.taskana.adapter.exceptions.ReferencedTaskDoesNotExistInExternalSystemException;
import pro.taskana.adapter.exceptions.TaskCreationFailedException;
import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.exceptions.TaskAlreadyExistException;

/**
 * Retrieves tasks in an external system and start corresponding tasks in taskana.
 *
 * @author bbr
 */
@Component
public class TaskanaTaskStarter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskStarter.class);

  @Autowired AdapterManager adapterManager;

  @Scheduled(
      fixedRateString =
          "${taskana.adapter.scheduler.run.interval.for.start.taskana.tasks.in.milliseconds}")
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
        retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks();
      } catch (Exception ex) {
        LOGGER.error("Caught {} while trying to create Taskana tasks from referenced tasks", ex);
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
        LOGGER.trace(
            "\"TaskanaTaskStarter.retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks "
                + "Leaving handling of new tasks for System Connector "
                + systemConnector.getSystemUrl());
      }
    }
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
              "caught Exception {} when attempting to start TaskanaTask for referencedTask {}",
              e,
              referencedTask);
        }
      } catch (Exception e) {
        LOGGER.warn(
            "caught Exception {} when attempting to start TaskanaTask for referencedTask {}",
            e,
            referencedTask);
      }
    }
    return newCreatedTasksInTaskana;
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

  private void addVariablesToReferencedTask(
      ReferencedTask referencedTask, SystemConnector connector) {
    if (referencedTask.getVariables() == null) {
      String variables = connector.retrieveVariables(referencedTask.getId());
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
