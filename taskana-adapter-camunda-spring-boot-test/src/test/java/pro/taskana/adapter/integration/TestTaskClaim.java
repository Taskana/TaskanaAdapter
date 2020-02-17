package pro.taskana.adapter.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.common.api.exceptions.NotAuthorizedException;
import pro.taskana.security.JaasRunner;
import pro.taskana.security.WithAccessId;
import pro.taskana.task.api.TaskState;
import pro.taskana.task.api.exceptions.InvalidOwnerException;
import pro.taskana.task.api.exceptions.InvalidStateException;
import pro.taskana.task.api.exceptions.TaskNotFoundException;
import pro.taskana.task.api.models.Task;
import pro.taskana.task.api.models.TaskSummary;

/**
 * Test class to test the synchronisation of assignees when tasks get claimed in camunda or taskana.
 */
@SpringBootTest(
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@RunWith(JaasRunner.class)
@ContextConfiguration
public class TestTaskClaim extends AbsIntegrationTest {

  @WithAccessId(
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  public void claim_of_taskana_task_should_claim_unclaimed_camunda_task()
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
      assertEquals(1, taskanaTasks.size());
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertEquals(taskanaTaskExternalId, camundaTaskId);
      String taskanaTaskId = taskanaTasks.get(0).getId();

      // verify that no assignee for camunda task is set yet
      boolean noAssigneeSet =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null);
      assertTrue(noAssigneeSet);

      // verify that TaskState of taskana task is 'READY' first
      Task task = this.taskService.getTask(taskanaTaskId);
      assertEquals(task.getState(), TaskState.READY);

      // claim task in taskana and verify updated TaskState to be 'CLAIMED'
      this.taskService.claim(taskanaTaskId);
      Task updatedTask = this.taskService.getTask(taskanaTaskId);
      assertEquals(updatedTask.getState(), TaskState.CLAIMED);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task
      boolean assigneeSetSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertTrue(assigneeSetSuccessfully);
    }
  }

  @WithAccessId(
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  public void claim_of_taskana_task_should_claim_already_claimed_camunda_task()
      throws TaskNotFoundException, NotAuthorizedException, JSONException, InterruptedException,
          InvalidOwnerException, InvalidStateException {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_assignee_set", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {

      // retrieve and check taskanaTaskId
      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertEquals(1, taskanaTasks.size());
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertEquals(taskanaTaskExternalId, camundaTaskId);
      String taskanaTaskId = taskanaTasks.get(0).getId();

      // verify that an assignee for camunda task is already set
      boolean noAssigneeSet =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null);
      assertFalse(noAssigneeSet);

      // verify that TaskState of taskana task is 'READY' first
      Task task = this.taskService.getTask(taskanaTaskId);
      assertEquals(task.getState(), TaskState.READY);

      // claim task in taskana and verify updated TaskState to be 'CLAIMED'
      this.taskService.claim(taskanaTaskId);
      Task updatedTask = this.taskService.getTask(taskanaTaskId);
      assertEquals(updatedTask.getState(), TaskState.CLAIMED);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task
      boolean assigneeSetSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertTrue(assigneeSetSuccessfully);
    }
  }

  @WithAccessId(
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  public void cancel_claim_of_taskana_task_should_cancel_claim_of_camunda_task()
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
      assertEquals(1, taskanaTasks.size());
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertEquals(taskanaTaskExternalId, camundaTaskId);
      String taskanaTaskId = taskanaTasks.get(0).getId();

      // verify that TaskState of taskana task is 'READY' first
      Task task = this.taskService.getTask(taskanaTaskId);
      assertEquals(task.getState(), TaskState.READY);

      // claim task in taskana and verify updated TaskState to be 'CLAIMED'
      this.taskService.claim(taskanaTaskId);
      Task updatedTask = this.taskService.getTask(taskanaTaskId);
      assertEquals(updatedTask.getState(), TaskState.CLAIMED);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task
      boolean assigneeSetSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertTrue(assigneeSetSuccessfully);

      // cancel claim taskana task and verify updated TaskState to be 'READY'
      this.taskService.cancelClaim(taskanaTaskId);
      Task taskWithCancelledClaim = this.taskService.getTask(taskanaTaskId);
      assertEquals(taskWithCancelledClaim.getState(), TaskState.READY);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify that the assignee for camunda task is no longer set
      boolean noAssigneeSet =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null);
      assertTrue(noAssigneeSet);
    }
  }

  @WithAccessId(
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  public void claim_of_taskana_task_after_cancel_claim_should_claim_task_in_camunda_again()
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
      assertEquals(1, taskanaTasks.size());
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertEquals(taskanaTaskExternalId, camundaTaskId);
      String taskanaTaskId = taskanaTasks.get(0).getId();

      // verify that TaskState of taskana task is 'READY' first
      Task task = this.taskService.getTask(taskanaTaskId);
      assertEquals(task.getState(), TaskState.READY);

      // claim task in taskana and verify updated TaskState to be 'CLAIMED'
      this.taskService.claim(taskanaTaskId);
      Task updatedTask = this.taskService.getTask(taskanaTaskId);
      assertEquals(updatedTask.getState(), TaskState.CLAIMED);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task
      boolean assigneeSetSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertTrue(assigneeSetSuccessfully);

      // cancel claim taskana task and verify updated TaskState to be 'READY'
      this.taskService.cancelClaim(taskanaTaskId);
      Task taskWithCancelledClaim = this.taskService.getTask(taskanaTaskId);
      assertEquals(taskWithCancelledClaim.getState(), TaskState.READY);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify that the assignee for camunda task is no longer set
      boolean noAssigneeSet =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null);
      assertTrue(noAssigneeSet);

      // claim task in taskana and verify updated TaskState to be 'CLAIMED' again
      this.taskService.claim(taskanaTaskId);
      Task updatedTaskAfterAnotherClaim = this.taskService.getTask(taskanaTaskId);
      assertEquals(updatedTaskAfterAnotherClaim.getState(), TaskState.CLAIMED);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task again
      boolean assigneeSetSuccessfullyAgain =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertTrue(assigneeSetSuccessfullyAgain);
    }
  }
}
