package pro.taskana.adapter.taskanaconnector.api.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pro.taskana.adapter.exceptions.TaskCreationFailedException;
import pro.taskana.adapter.exceptions.TaskTerminationFailedException;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.classification.api.exceptions.ClassificationNotFoundException;
import pro.taskana.common.api.LoggerUtils;
import pro.taskana.common.api.exceptions.InvalidArgumentException;
import pro.taskana.common.api.exceptions.NotAuthorizedException;
import pro.taskana.task.api.CallbackState;
import pro.taskana.task.api.TaskService;
import pro.taskana.task.api.TaskState;
import pro.taskana.task.api.exceptions.InvalidOwnerException;
import pro.taskana.task.api.exceptions.InvalidStateException;
import pro.taskana.task.api.exceptions.TaskAlreadyExistException;
import pro.taskana.task.api.exceptions.TaskNotFoundException;
import pro.taskana.task.api.models.Task;
import pro.taskana.task.api.models.TaskSummary;
import pro.taskana.workbasket.api.exceptions.WorkbasketNotFoundException;

/**
 * Implements TaskanaConnector.
 *
 * @author bbr
 */
@Component
public class TaskanaSystemConnectorImpl implements TaskanaConnector {

  static final String REFERENCED_TASK_ID = "referenced_task_id";
  static final String REFERENCED_TASK_VARIABLES = "referenced_task_variables";
  static final String SYSTEM_URL = "system_url";

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaSystemConnectorImpl.class);

  @Autowired private TaskService taskService;

  @Autowired private TaskInformationMapper taskInformationMapper;

  public List<ReferencedTask> retrieveCompletedTaskanaTasksAsReferencedTasks() {

    List<TaskSummary> completedTasks =
        taskService
            .createTaskQuery()
            .stateIn(TaskState.COMPLETED)
            .callbackStateIn(CallbackState.CALLBACK_PROCESSING_REQUIRED, CallbackState.CLAIMED)
            .list();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "the following taskana tasks were completed {} and must process their callback.",
          LoggerUtils.listToString(completedTasks));
    }

    return retrieveTaskanaTasksAndConvertToReferencedTasks(completedTasks);
  }

  @Override
  public List<ReferencedTask> retrieveClaimedTaskanaTasksAsReferencedTasks() {

    List<TaskSummary> claimedTasks =
        taskService
            .createTaskQuery()
            .stateIn(TaskState.CLAIMED)
            .callbackStateIn(CallbackState.CALLBACK_PROCESSING_REQUIRED)
            .list();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "the following taskana tasks were claimed {} and must process their callback.",
          LoggerUtils.listToString(claimedTasks));
    }

    return retrieveTaskanaTasksAndConvertToReferencedTasks(claimedTasks);
  }

  @Override
  public List<ReferencedTask> retrieveCancelledClaimTaskanaTasksAsReferencedTasks() {

    List<TaskSummary> claimedTasks =
        taskService
            .createTaskQuery()
            .stateIn(TaskState.READY)
            .callbackStateIn(CallbackState.CLAIMED)
            .list();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "the claims of the following taskana tasks were cancelled {} and "
              + " must process their callback.",
          LoggerUtils.listToString(claimedTasks));
    }

    return retrieveTaskanaTasksAndConvertToReferencedTasks(claimedTasks);
  }

  @Override
  public void changeReferencedTaskCallbackState(
      List<ReferencedTask> referencedTasks, CallbackState callbackState) {

    List<String> externalIds =
        referencedTasks.stream().map(ReferencedTask::getId).collect(Collectors.toList());
    if (!externalIds.isEmpty()) {
      taskService.setCallbackStateForTasks(externalIds, callbackState);
    }
  }

  @Override
  public void createTaskanaTask(Task taskanaTask) throws TaskCreationFailedException {
    try {
      taskService.createTask(taskanaTask);
    } catch (NotAuthorizedException
        | InvalidArgumentException
        | ClassificationNotFoundException
        | WorkbasketNotFoundException
        | TaskAlreadyExistException e) {
      LOGGER.error("Caught Exception {} when creating taskana task {} ", e, taskanaTask);
      throw new TaskCreationFailedException(
          "Error when creationg a taskana task " + taskanaTask, e);
    }
  }

  @Override
  public Task convertToTaskanaTask(ReferencedTask camundaTask) {
    return taskInformationMapper.convertToTaskanaTask(camundaTask);
  }

  @Override
  public ReferencedTask convertToReferencedTask(Task task) {
    return taskInformationMapper.convertToReferencedTask(task);
  }

  @Override
  public void terminateTaskanaTask(ReferencedTask referencedTask)
      throws TaskTerminationFailedException {
    String taskId = null;
    TaskSummary taskSummary = null;
    try {
      taskSummary = taskService.createTaskQuery().externalIdIn(referencedTask.getId()).single();
      if (taskSummary != null) {
        taskId = taskSummary.getId();
        taskService.forceCompleteTask(taskId);
        // take care that the adapter doesn't attempt to finish the corresponding camunda task
        List<String> externalIds = Stream.of(referencedTask.getId()).collect(Collectors.toList());
        taskService.setCallbackStateForTasks(
            externalIds, CallbackState.CALLBACK_PROCESSING_COMPLETED);
      }
    } catch (TaskNotFoundException e1) {
      LOGGER.debug("Nothing to do in terminateTaskanaTask. Task {} is already gone", taskId);
    } catch (InvalidOwnerException | InvalidStateException | NotAuthorizedException e2) {
      if (taskSummary != null && TaskState.COMPLETED.equals(taskSummary.getState())) {
        LOGGER.debug("Nothing to do in terminateTaskanaTask. Task {} is already completed", taskId);
      } else {
        throw new TaskTerminationFailedException("Task termination failed for task " + taskId, e2);
      }
    }
  }

  private List<ReferencedTask> retrieveTaskanaTasksAndConvertToReferencedTasks(
      List<TaskSummary> requestedTasks) {

    List<ReferencedTask> result = new ArrayList<>();

    for (TaskSummary taskSummary : requestedTasks) {
      try {
        Task taskanaTask = taskService.getTask(taskSummary.getId());
        Map<String, String> callbackInfo = taskanaTask.getCallbackInfo();
        if (callbackInfo != null
            && callbackInfo.get(REFERENCED_TASK_ID) != null
            && callbackInfo.get(SYSTEM_URL) != null) {
          result.add(taskInformationMapper.convertToReferencedTask(taskanaTask));
        }
      } catch (TaskNotFoundException | NotAuthorizedException e) {
        LOGGER.error("Caught {} when trying to retrieve requested taskana tasks.", e, e);
      }
    }

    return result;
  }
}
