package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

import pro.taskana.TaskState;
import pro.taskana.TaskSummary;
import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.exceptions.InvalidOwnerException;
import pro.taskana.exceptions.InvalidStateException;
import pro.taskana.exceptions.NotAuthorizedException;
import pro.taskana.exceptions.TaskNotFoundException;
import pro.taskana.security.JaasRunner;
import pro.taskana.security.WithAccessId;

/**
 * Test class to test the cancellation of camunda tasks upon cancellation of taskana tasks and vice
 * versa.
 *
 * @author Ben Fuernrohr
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
      assertEquals(1, taskanaTasks.size());
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertEquals(taskanaTaskExternalId, camundaTaskId);
      String taskanaTaskId = taskanaTasks.get(0).getTaskId();

      // complete taskana-task and wait
      this.taskService.claim(taskanaTaskId);
      this.taskService.completeTask(taskanaTaskId);
      try {
        this.taskService.deleteTask(taskanaTaskId);
        fail("expected an InvalidStateExcpetion but no Exception was thrown");
      } catch (InvalidStateException e) {
        assertTrue(
            e.getMessage().endsWith("cannot be deleted because its callback is not yet processed"));
      }
      Thread.sleep((long) (this.adapterCancelPollingInterval * 1.2));

      // assert camunda task was deleted
      boolean taskRetrievalSuccessful =
          this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
      assertFalse(taskRetrievalSuccessful);

      // attempt to delete process instance, should fail because process instance should already be
      // deleted in
      // response
      boolean processInstanceDeletionSuccessful =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId);
      assertFalse(processInstanceDeletionSuccessful);
    }
  }

  @WithAccessId(
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  public void deletion_of_camunda_process_instance_should_complete_taskana_task()
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
      assertEquals(1, taskanaTasks.size());
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertEquals(taskanaTaskExternalId, camundaTaskId);

      // delete camunda process instance and wait
      boolean camundaProcessCancellationSucessful =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId);
      assertTrue(camundaProcessCancellationSucessful);

      // assert deletion was successful by attempting to delete again
      boolean camundaProcessCancellationSucessful2 =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId);
      assertFalse(camundaProcessCancellationSucessful2);
      Thread.sleep((long) (this.adapterCancelPollingInterval * 1.2));

      // assert taskana task was completed but still exists
      taskanaTasks = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertEquals(1, taskanaTasks.size());
      Instant taskanaTaskCompletion = taskanaTasks.get(0).getCompleted();
      Instant taskanaTaskCreation = taskanaTasks.get(0).getCreated();
      TaskState taskanaTaskState = taskanaTasks.get(0).getState();
      assertTrue(TaskState.COMPLETED.equals(taskanaTaskState));
      assertFalse(taskanaTaskCompletion == null);
      assertEquals(1, taskanaTaskCompletion.compareTo(taskanaTaskCreation));
    }
  }

  @WithAccessId(
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  public void interruption_of_camunda_task_by_timer_should_complete_taskana_task()
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
      assertEquals(1, taskanaTasks.size());
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertEquals(taskanaTaskExternalId, camundaTaskId);

      // wait for the camunda task to be interrupted by the timer event (1 second), then the camunda
      // job poll.
      // Assert it was interrupted.
      Thread.sleep(1000 + (long) (this.jobExecutor.getMaxWait() * 1.2));
      boolean taskRetrievalSuccessful =
          this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
      assertFalse(taskRetrievalSuccessful);

      // wait for the adapter to register the interruption
      Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

      // assert taskana task was completed but still exists
      taskanaTasks = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertEquals(1, taskanaTasks.size());
      Instant taskanaTaskCompletion = taskanaTasks.get(0).getCompleted();
      Instant taskanaTaskCreation = taskanaTasks.get(0).getCreated();
      TaskState taskanaTaskState = taskanaTasks.get(0).getState();
      assertNotNull(taskanaTaskCompletion);
      assertEquals(1, taskanaTaskCompletion.compareTo(taskanaTaskCreation));
      assertEquals(TaskState.COMPLETED, taskanaTaskState);
    }
  }
}
