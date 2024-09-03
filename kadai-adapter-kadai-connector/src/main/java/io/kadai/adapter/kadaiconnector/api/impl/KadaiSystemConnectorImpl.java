package io.kadai.adapter.kadaiconnector.api.impl;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.exceptions.TaskCreationFailedException;
import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.api.exceptions.KadaiException;
import io.kadai.common.api.exceptions.NotAuthorizedException;
import io.kadai.task.api.CallbackState;
import io.kadai.task.api.TaskService;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.exceptions.InvalidOwnerException;
import io.kadai.task.api.exceptions.InvalidTaskStateException;
import io.kadai.task.api.exceptions.TaskNotFoundException;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import io.kadai.workbasket.api.exceptions.NotAuthorizedOnWorkbasketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Implements KadaiConnector. */
@Component
public class KadaiSystemConnectorImpl implements KadaiConnector {

  static final String REFERENCED_TASK_ID = "referenced_task_id";
  static final String REFERENCED_TASK_VARIABLES = "referenced_task_variables";
  static final String SYSTEM_URL = "system_url";
  private static final String TASK_STATE_CANCELLED = "CANCELLED";
  private static final String TASK_STATE_TERMINATED = "TERMINATED";
  private static final Logger LOGGER = LoggerFactory.getLogger(KadaiSystemConnectorImpl.class);

  @Autowired private TaskService taskService;
  @Autowired private KadaiEngine kadaiEngine;

  @Autowired private TaskInformationMapper taskInformationMapper;

  @Autowired private DataSource kadaiDataSource;
  Integer batchSize = AdapterSpringContextProvider.getBean(Integer.class);

  public List<ReferencedTask> retrieveFinishedKadaiTasksAsReferencedTasks() {

    List<TaskSummary> finishedTasks =
        taskService
            .createTaskQuery()
            .lockResultsEquals(batchSize)
            .stateIn(TaskState.COMPLETED, TaskState.CANCELLED, TaskState.TERMINATED)
            .callbackStateIn(CallbackState.CALLBACK_PROCESSING_REQUIRED, CallbackState.CLAIMED)
            .list();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "the following kadai tasks were completed {} and must process their callback.",
          finishedTasks);
    }

    return retrieveKadaiTasksAndConvertToReferencedTasks(finishedTasks);
  }

  @Override
  public List<ReferencedTask> retrieveClaimedKadaiTasksAsReferencedTasks() {

    List<TaskSummary> claimedTasks =
        taskService
            .createTaskQuery()
            .lockResultsEquals(batchSize)
            .stateIn(TaskState.CLAIMED)
            .callbackStateIn(CallbackState.CALLBACK_PROCESSING_REQUIRED)
            .list();

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "the following kadai tasks were claimed {} and must process their callback.",
          claimedTasks);
    }

    return retrieveKadaiTasksAndConvertToReferencedTasks(claimedTasks);
  }

  @Override
  public List<ReferencedTask> retrieveCancelledClaimKadaiTasksAsReferencedTasks() {

    List<TaskSummary> claimedTasks =
        taskService
            .createTaskQuery()
            .stateIn(TaskState.READY)
            .lockResultsEquals(batchSize)
            .callbackStateIn(CallbackState.CLAIMED)
            .list();
    
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
          "the claims of the following kadai tasks were cancelled {} and "
              + " must process their callback.",
          claimedTasks);
    }

    return retrieveKadaiTasksAndConvertToReferencedTasks(claimedTasks);
  }

  @Override
  public void changeTaskCallbackState(
      List<ReferencedTask> referencedTasks, CallbackState callbackState) {

    List<String> externalIds =
        referencedTasks.stream().map(ReferencedTask::getId).collect(Collectors.toList());
    if (!externalIds.isEmpty()) {
      taskService.setCallbackStateForTasks(externalIds, callbackState);
    }
  }

  @Override
  public void createKadaiTask(Task kadaiTask) throws TaskCreationFailedException {
    try {
      taskService.createTask(kadaiTask);
    } catch (KadaiException e) {
      LOGGER.error("Caught Exception {} when creating kadai task {} ", e, kadaiTask);
      throw new TaskCreationFailedException(kadaiTask.getExternalId(), e);
    }
  }

  @Override
  public Task convertToKadaiTask(ReferencedTask camundaTask) {
    return taskInformationMapper.convertToKadaiTask(camundaTask);
  }

  @Override
  public ReferencedTask convertToReferencedTask(Task task) {
    return taskInformationMapper.convertToReferencedTask(task);
  }

  @Override
  public void terminateKadaiTask(ReferencedTask referencedTask)
      throws TaskTerminationFailedException {
    String taskId = null;
    TaskSummary taskSummary = null;
    try {
      taskSummary = taskService.createTaskQuery().externalIdIn(referencedTask.getId()).single();
      if (taskSummary != null) {
        taskId = taskSummary.getId();

        switch (referencedTask.getTaskState()) {
          case TASK_STATE_TERMINATED:
            taskService.terminateTask(taskId);
            break;
          case TASK_STATE_CANCELLED:
            taskService.cancelTask(taskId);
            break;
          default:
            taskService.forceCompleteTask(taskId);
            break;
        }
        // take care that the adapter doesn't attempt to finish the corresponding camunda task
        List<String> externalIds = Stream.of(referencedTask.getId()).collect(Collectors.toList());
        taskService.setCallbackStateForTasks(
            externalIds, CallbackState.CALLBACK_PROCESSING_COMPLETED);
      }
    } catch (TaskNotFoundException e1) {
      LOGGER.debug("Nothing to do in terminateKadaiTask. Task {} is already gone", taskId);
    } catch (InvalidOwnerException
        | NotAuthorizedException
        | NotAuthorizedOnWorkbasketException
        | InvalidTaskStateException e2) {
      if (TaskState.COMPLETED.equals(taskSummary.getState())) {
        LOGGER.debug("Nothing to do in terminateKadaiTask. Task {} is already completed", taskId);
      } else {
        throw new TaskTerminationFailedException("Task termination failed for task " + taskId, e2);
      }
    }
  }

  private List<ReferencedTask> retrieveKadaiTasksAndConvertToReferencedTasks(
      List<TaskSummary> requestedTasks) {

    List<ReferencedTask> result = new ArrayList<>();

    for (TaskSummary taskSummary : requestedTasks) {
      try {
        Task kadaiTask = taskService.getTask(taskSummary.getId());
        Map<String, String> callbackInfo = kadaiTask.getCallbackInfo();
        if (callbackInfo != null
            && callbackInfo.get(REFERENCED_TASK_ID) != null
            && callbackInfo.get(SYSTEM_URL) != null) {
          result.add(taskInformationMapper.convertToReferencedTask(kadaiTask));
        }
      } catch (TaskNotFoundException | NotAuthorizedOnWorkbasketException e) {
        LOGGER.error("Caught {} when trying to retrieve requested kadai tasks.", e, e);
      }
    }

    return result;
  }

  @Override
  public String toString() {
    return "KadaiSystemConnectorImpl [taskService=" + taskService + "]";
  }
}
