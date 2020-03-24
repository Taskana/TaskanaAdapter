package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.time.Instant;
import java.util.List;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.common.api.exceptions.NotAuthorizedException;
import pro.taskana.security.JaasRunner;
import pro.taskana.security.WithAccessId;
import pro.taskana.task.api.TaskState;
import pro.taskana.task.api.exceptions.InvalidOwnerException;
import pro.taskana.task.api.exceptions.InvalidStateException;
import pro.taskana.task.api.exceptions.TaskNotFoundException;
import pro.taskana.task.api.models.TaskSummary;

/**
 * Test class to test the cancellation of camunda tasks upon cancellation of taskana tasks and vice
 * versa.
 */
@SpringBootTest(
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@RunWith(JaasRunner.class)
@ContextConfiguration
public class TestCancelledTaskRetrieval extends AbsIntegrationTest {

  @Autowired private JobExecutor jobExecutor;

  @WithAccessId(
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  public void deletion_of_taskana_task_should_delete_camunda_task_and_process()
      throws TaskNotFoundException, NotAuthorizedException, JSONException, InterruptedException,
          InvalidOwnerException, InvalidStateException {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check taskanaTaskId
      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(taskanaTasks).hasSize(1);
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertThat(camundaTaskId).isEqualTo(taskanaTaskExternalId);
      String taskanaTaskId = taskanaTasks.get(0).getId();

      // complete taskana-task and wait
      this.taskService.claim(taskanaTaskId);
      this.taskService.completeTask(taskanaTaskId);
      try {
        this.taskService.deleteTask(taskanaTaskId);
        fail("expected an InvalidStateExcpetion but no Exception was thrown");
      } catch (InvalidStateException e) {
        assertThat(
                e.getMessage()
                    .endsWith("cannot be deleted because its callback is not yet processed"))
            .isTrue();
      }
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
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  public void deletion_of_camunda_process_instance_should_terminate_taskana_task()
      throws JSONException, InterruptedException {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check taskanaTaskId
      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(taskanaTasks).hasSize(1);
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertThat(camundaTaskId).isEqualTo(taskanaTaskExternalId);

      // delete camunda process instance and wait
      boolean camundaProcessCancellationSucessful =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, false);
      assertThat(camundaProcessCancellationSucessful).isTrue();

      // assert deletion was successful by attempting to delete again
      boolean camundaProcessCancellationSucessful2 =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, false);
      assertThat(camundaProcessCancellationSucessful2).isFalse();
      Thread.sleep((long) (this.adapterCancelPollingInterval * 1.2));

      // assert taskana task was completed but still exists
      taskanaTasks = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(taskanaTasks).hasSize(1);
      Instant taskanaTaskCompletion = taskanaTasks.get(0).getCompleted();
      Instant taskanaTaskCreation = taskanaTasks.get(0).getCreated();
      TaskState taskanaTaskState = taskanaTasks.get(0).getState();
      assertThat(TaskState.TERMINATED.equals(taskanaTaskState)).isTrue();
      assertThat(taskanaTaskCompletion == null).isFalse();
      assertThat(taskanaTaskCompletion.compareTo(taskanaTaskCreation)).isEqualTo(1);
    }
  }

  @WithAccessId(
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  public void deletion_of_taskana_task_with_deleted_camunda_task_should_be_handled_gracefully()
      throws JSONException, InterruptedException, TaskNotFoundException, NotAuthorizedException,
          InvalidStateException, InvalidOwnerException {
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
      // retrieve and check taskanaTaskId
      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(taskanaTasks).hasSize(1);
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertThat(camundaTaskId).isEqualTo(taskanaTaskExternalId);
      taskService.forceCompleteTask(taskanaTasks.get(0).getId());

      Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

      // now it should be possible to delete the taskana task.
      taskService.deleteTask(taskanaTasks.get(0).getId());

      assertThatThrownBy(() -> taskService.getTask(taskanaTasks.get(0).getId()))
          .isInstanceOf(TaskNotFoundException.class);
    }
  }

  @WithAccessId(
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  public void interruption_of_camunda_task_by_timer_should_cancel_taskana_task()
      throws InterruptedException {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_timed_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check taskanaTaskId
      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(taskanaTasks).hasSize(1);
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertThat(camundaTaskId).isEqualTo(taskanaTaskExternalId);

      // wait for the camunda task to be interrupted by the timer event (1 second), then the camunda
      // job poll.
      // Assert it was interrupted.
      Thread.sleep(1000 + (long) (this.jobExecutor.getMaxWait() * 1.2));
      boolean taskRetrievalSuccessful =
          this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
      assertThat(taskRetrievalSuccessful).isFalse();

      // wait for the adapter to register the interruption
      Thread.sleep((long) (this.adapterCancelledClaimPollingInterval * 1.2));

      // assert taskana task was completed but still exists
      taskanaTasks = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(taskanaTasks).hasSize(1);
      Instant taskanaTaskCompletion = taskanaTasks.get(0).getCompleted();
      Instant taskanaTaskCreation = taskanaTasks.get(0).getCreated();
      TaskState taskanaTaskState = taskanaTasks.get(0).getState();
      assertThat(taskanaTaskCompletion).isNotNull();
      assertThat(taskanaTaskCompletion.compareTo(taskanaTaskCreation)).isEqualTo(1);
      assertThat(taskanaTaskState).isEqualTo(TaskState.CANCELLED);
    }
  }
}
