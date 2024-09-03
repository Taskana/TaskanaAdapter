package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.exceptions.InvalidCallbackStateException;
import io.kadai.task.api.exceptions.TaskNotFoundException;
import io.kadai.task.api.models.TaskSummary;
import java.time.Instant;
import java.util.List;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test class to test the cancellation of camunda tasks upon cancellation of kadai tasks and vice
 * versa.
 */
@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
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
  void should_DeleteCamundaTaskAndProcess_When_DeleteKadaiTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check kadaiTaskId
      List<TaskSummary> kadaiTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
      assertThat(camundaTaskId).isEqualTo(kadaiTaskExternalId);
      String kadaiTaskId = kadaiTasks.get(0).getId();

      // complete kadai-task and wait
      this.taskService.claim(kadaiTaskId);
      this.taskService.completeTask(kadaiTaskId);
      assertThatThrownBy(() -> taskService.deleteTask(kadaiTaskId))
          .isInstanceOf(InvalidCallbackStateException.class)
          .hasMessageContaining(
              "Expected callback state of Task with id '%s' "
                  + "to be: '[NONE, CLAIMED, CALLBACK_PROCESSING_COMPLETED]', "
                  + "but found 'CALLBACK_PROCESSING_REQUIRED'",
              kadaiTaskId);
      Thread.sleep((long) (this.adapterCancelPollingInterval * 1.2));

      // assert camunda task was deleted
      boolean taskRetrievalSuccessful =
          this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
      assertThat(taskRetrievalSuccessful).isFalse();

      // attempt to delete process instance, should fail because process instance should already be
      // deleted in
      // response
      boolean processInstanceDeletionSuccessful =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, false);
      assertThat(processInstanceDeletionSuccessful).isFalse();
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_TerminateKadaiTask_When_DeleteCamundaProcessInstance() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check kadaiTaskId
      List<TaskSummary> kadaiTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
      assertThat(camundaTaskId).isEqualTo(kadaiTaskExternalId);

      // delete camunda process instance and wait
      boolean camundaProcessCancellationSucessful =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, false);
      assertThat(camundaProcessCancellationSucessful).isTrue();

      // assert deletion was successful by attempting to delete again
      boolean camundaProcessCancellationSucessful2 =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, false);
      assertThat(camundaProcessCancellationSucessful2).isFalse();
      Thread.sleep((long) (this.adapterCancelPollingInterval * 1.2));

      // assert kadai task was completed but still exists
      kadaiTasks = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      Instant kadaiTaskCompletion = kadaiTasks.get(0).getCompleted();
      Instant kadaiTaskCreation = kadaiTasks.get(0).getCreated();
      TaskState kadaiTaskState = kadaiTasks.get(0).getState();
      assertThat(kadaiTaskState).isEqualTo(TaskState.TERMINATED);
      assertThat(kadaiTaskCompletion).isNotNull();
      assertThat(kadaiTaskCompletion.compareTo(kadaiTaskCreation)).isEqualTo(1);
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"admin"})
  @Test
  void should_BeAbleToDeleteKadaiTask_When_DeleteCamundaTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // delete camunda process without notifying the listeners
    boolean camundaProcessCancellationSucessful =
        this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, true);
    assertThat(camundaProcessCancellationSucessful).isTrue();

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check kadaiTaskId
      List<TaskSummary> kadaiTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
      assertThat(camundaTaskId).isEqualTo(kadaiTaskExternalId);
      taskService.forceCompleteTask(kadaiTasks.get(0).getId());

      Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

      // now it should be possible to delete the kadai task.
      taskService.deleteTask(kadaiTasks.get(0).getId());

      assertThatThrownBy(() -> taskService.getTask(kadaiTasks.get(0).getId()))
          .isInstanceOf(TaskNotFoundException.class);
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CancelKadaiTask_When_InterruptionByTimerOfCamundaTaskOccurs() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_timed_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check kadaiTaskId
      List<TaskSummary> kadaiTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
      assertThat(camundaTaskId).isEqualTo(kadaiTaskExternalId);

      // wait for the camunda task to be interrupted by the timer event (1 second), then the camunda
      // job poll.
      // Assert it was interrupted.
      Thread.sleep(1000 + (long) (this.jobExecutor.getMaxWait() * 1.2));
      boolean taskRetrievalSuccessful =
          this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
      assertThat(taskRetrievalSuccessful).isFalse();

      // wait for the adapter to register the interruption
      Thread.sleep((long) (this.adapterCancelledClaimPollingInterval * 1.2));

      // assert kadai task was completed but still exists
      kadaiTasks = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      Instant kadaiTaskCompletion = kadaiTasks.get(0).getCompleted();
      Instant kadaiTaskCreation = kadaiTasks.get(0).getCreated();
      TaskState kadaiTaskState = kadaiTasks.get(0).getState();
      assertThat(kadaiTaskCompletion).isNotNull();
      assertThat(kadaiTaskCompletion.compareTo(kadaiTaskCreation)).isEqualTo(1);
      assertThat(kadaiTaskState).isEqualTo(TaskState.CANCELLED);
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CompleteCamundaTask_When_CancellingKadaiTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check kadaiTaskId
      List<TaskSummary> kadaiTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
      assertThat(camundaTaskId).isEqualTo(kadaiTaskExternalId);

      taskService.cancelTask(kadaiTasks.get(0).getId());

      Thread.sleep(1000 + (long) (this.jobExecutor.getMaxWait() * 1.2));

      // check if camunda task got completed and therefore doesn't exist anymore
      boolean taskRetrievalSuccessful =
          this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
      assertThat(taskRetrievalSuccessful).isFalse();
    }
  }
}
