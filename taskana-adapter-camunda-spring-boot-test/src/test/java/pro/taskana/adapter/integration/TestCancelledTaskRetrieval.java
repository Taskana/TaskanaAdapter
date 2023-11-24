package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static pro.taskana.utils.AwaitilityUtils.checkCamundaTaskIsCompleted;
import static pro.taskana.utils.AwaitilityUtils.getDuration;
import static pro.taskana.utils.AwaitilityUtils.getTaskSummary;

import java.util.List;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.task.api.TaskState;
import pro.taskana.task.api.exceptions.InvalidCallbackStateException;
import pro.taskana.task.api.exceptions.TaskNotFoundException;
import pro.taskana.task.api.models.TaskSummary;
import pro.taskana.utils.TaskStateMatcher;

/**
 * Test class to test the cancellation of camunda tasks upon cancellation of taskana tasks and vice
 * versa.
 */
@SpringBootTest(
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class TestCancelledTaskRetrieval extends AbsIntegrationTest {

  @Autowired private JobExecutor jobExecutor;

  @WithAccessId(
      user = "teamlead_1",
      groups = {"admin"})
  @Test
  void should_DeleteCamundaTaskAndProcess_When_DeleteTaskanaTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    // retrieve and check taskanaTaskId
    TaskSummary taskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTaskSummary.getExternalId());

    String taskanaTaskId = taskanaTaskSummary.getId();

    // complete taskana-task and wait
    this.taskService.claim(taskanaTaskId);
    this.taskService.completeTask(taskanaTaskId);
    assertThatThrownBy(() -> taskService.deleteTask(taskanaTaskId))
        .isInstanceOf(InvalidCallbackStateException.class)
        .hasMessageContaining(
            "Expected callback state of Task with id '%s' "
                + "to be: '[NONE, CLAIMED, CALLBACK_PROCESSING_COMPLETED]', "
                + "but found 'CALLBACK_PROCESSING_REQUIRED'",
            taskanaTaskId);

    // assert camunda task was deleted
    await()
        .atMost(getDuration(adapterCancelPollingInterval))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(
            () -> this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId), is(false));

    // attempt to delete process instance, should fail because process instance should already be
    // deleted in response
    boolean processInstanceDeletionSuccessful =
        this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, false);
    assertThat(processInstanceDeletionSuccessful).isFalse();
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_TerminateTaskanaTask_When_DeleteCamundaProcessInstance() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    // retrieve and check taskanaTaskId
    TaskSummary taskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTaskSummary.getExternalId());

    // delete camunda process instance and wait
    boolean camundaProcessCancellationSucessful =
        this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, false);
    assertThat(camundaProcessCancellationSucessful).isTrue();

    // assert deletion was successful by attempting to delete again
    boolean camundaProcessCancellationSucessful2 =
        this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, false);
    assertThat(camundaProcessCancellationSucessful2).isFalse();

    // assert taskana task was completed but still exists
    TaskSummary canceledTaskanaTask =
        await()
            .atMost(getDuration(adapterCancelPollingInterval))
            .with()
            .pollInterval(ONE_HUNDRED_MILLISECONDS)
            .until(
                () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list().get(0),
                new TaskStateMatcher(TaskState.TERMINATED));
    assertThat(canceledTaskanaTask.getCompleted()).isNotNull();
    assertThat(canceledTaskanaTask.getCompleted().compareTo(canceledTaskanaTask.getCreated()))
        .isEqualTo(1);
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"admin"})
  @Test
  void should_BeAbleToDeleteTaskanaTask_When_DeleteCamundaTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    // delete camunda process without notifying the listeners
    await()
        .atMost(getDuration(adapterTaskPollingInterval))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(
            () ->
                this.camundaProcessengineRequester.deleteProcessInstanceWithId(
                    processInstanceId, true),
            is(true));

    // retrieve and check taskanaTaskId
    TaskSummary taskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTaskSummary.getExternalId());
    taskService.forceCompleteTask(taskanaTaskSummary.getId());

    // now it should be possible to delete the taskana task.
    await()
        .atMost(getDuration(adapterTaskPollingInterval))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        // ignoring because, we need to wait for an undefined amount of time until deletion is
        // successful
        .ignoreException(InvalidCallbackStateException.class)
        .until(
            () -> {
              taskService.deleteTask(taskanaTaskSummary.getId());
              return true;
            },
            is(true));
    assertThatThrownBy(() -> taskService.getTask(taskanaTaskSummary.getId()))
        .isInstanceOf(TaskNotFoundException.class);
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CancelTaskanaTask_When_InterruptionByTimerOfCamundaTaskOccurs() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_timed_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    // retrieve and check taskanaTaskId
    TaskSummary taskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTaskSummary.getExternalId());

    // wait for the camunda task to be interrupted by the timer event (1 second), then the camunda
    // job poll.
    // Assert it was interrupted.
    checkCamundaTaskIsCompleted(
        jobExecutor,
        camundaProcessengineRequester,
        camundaTaskId
    );

    TaskSummary interruptedTask =
        await()
            .atMost(getDuration(adapterCancelledClaimPollingInterval))
            .with()
            .pollInterval(ONE_HUNDRED_MILLISECONDS)
            .until(
                () -> {
                  List<TaskSummary> interruptedTasks =
                      this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
                  if (!interruptedTasks.isEmpty()
                      && interruptedTasks.get(0).getState() == TaskState.CANCELLED) {
                    return interruptedTasks.get(0);
                  } else {
                    return null;
                  }
                },
                notNullValue());
    assertThat(interruptedTask.getCompleted()).isAfter(interruptedTask.getCreated());
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CompleteCamundaTask_When_CancellingTaskanaTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    // retrieve and check taskanaTaskId
    TaskSummary taskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTaskSummary.getExternalId());

    taskService.cancelTask(taskanaTaskSummary.getId());

    // check if camunda task got completed and therefore doesn't exist anymore
    checkCamundaTaskIsCompleted(
        jobExecutor,
        camundaProcessengineRequester,
        camundaTaskId
    );
  }
}
