package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaUtilRequester;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.TaskState;
import io.kadai.task.api.models.Task;
import io.kadai.task.api.models.TaskSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * Test class to test the synchronisation of assignees when tasks get claimed in camunda or kadai.
 */
@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class TestTaskClaim extends AbsIntegrationTest {

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_ClaimUnclaimedCamundaTask_When_ClaimKadaiTask() throws Exception {

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
      assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskId);
      String kadaiTaskId = kadaiTasks.get(0).getId();

      // verify that no assignee for camunda task is set yet
      boolean noAssigneeSet =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null);
      assertThat(noAssigneeSet).isTrue();

      // verify that TaskState of kadai task is 'READY' first
      Task task = this.taskService.getTask(kadaiTaskId);
      assertThat(task.getState()).isEqualTo(TaskState.READY);

      // claim task in kadai and verify updated TaskState to be 'CLAIMED'
      this.taskService.claim(kadaiTaskId);
      Task updatedTask = this.taskService.getTask(kadaiTaskId);
      assertThat(updatedTask.getState()).isEqualTo(TaskState.CLAIMED);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task
      boolean assigneeSetSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertThat(assigneeSetSuccessfully).isTrue();
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_ClaimAlreadyClaimedCamundaTaska_When_ClaimKadaiTask() throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_assignee_set", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {

      // retrieve and check kadaiTaskId
      List<TaskSummary> kadaiTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
      assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskId);
      String kadaiTaskId = kadaiTasks.get(0).getId();

      // verify that an assignee for camunda task is already set
      boolean noAssigneeSet =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null);
      assertThat(noAssigneeSet).isFalse();

      // verify that TaskState of kadai task is 'READY' first
      Task task = this.taskService.getTask(kadaiTaskId);
      assertThat(task.getState()).isEqualTo(TaskState.READY);

      // claim task in kadai and verify updated TaskState to be 'CLAIMED'
      this.taskService.claim(kadaiTaskId);
      Task updatedTask = this.taskService.getTask(kadaiTaskId);
      assertThat(updatedTask.getState()).isEqualTo(TaskState.CLAIMED);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task
      boolean assigneeSetSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertThat(assigneeSetSuccessfully).isTrue();
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CancelClaimCamundaTask_When_CancelClaimKadaiTask() throws Exception {

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
      assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskId);
      String kadaiTaskId = kadaiTasks.get(0).getId();

      // verify that TaskState of kadai task is 'READY' first
      Task task = this.taskService.getTask(kadaiTaskId);
      assertThat(task.getState()).isEqualTo(TaskState.READY);

      // claim task in kadai and verify updated TaskState to be 'CLAIMED'
      this.taskService.claim(kadaiTaskId);
      Task updatedTask = this.taskService.getTask(kadaiTaskId);
      assertThat(updatedTask.getState()).isEqualTo(TaskState.CLAIMED);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task
      boolean assigneeSetSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertThat(assigneeSetSuccessfully).isTrue();

      // cancel claim kadai task and verify updated TaskState to be 'READY'
      this.taskService.cancelClaim(kadaiTaskId);
      Task taskWithCancelledClaim = this.taskService.getTask(kadaiTaskId);
      assertThat(taskWithCancelledClaim.getState()).isEqualTo(TaskState.READY);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify that the assignee for camunda task is no longer set
      boolean noAssigneeSet =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null);
      assertThat(noAssigneeSet).isTrue();
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_ClaimCamundaTaskAgain_When_ClaimKadaiTaskAfterCancelClaim() throws Exception {

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
      assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskId);
      String kadaiTaskId = kadaiTasks.get(0).getId();

      // verify that TaskState of kadai task is 'READY' first
      Task task = this.taskService.getTask(kadaiTaskId);
      assertThat(task.getState()).isEqualTo(TaskState.READY);

      // claim task in kadai and verify updated TaskState to be 'CLAIMED'
      this.taskService.claim(kadaiTaskId);
      Task updatedTask = this.taskService.getTask(kadaiTaskId);
      assertThat(updatedTask.getState()).isEqualTo(TaskState.CLAIMED);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task
      boolean assigneeSetSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertThat(assigneeSetSuccessfully).isTrue();

      // cancel claim kadai task and verify updated TaskState to be 'READY'
      this.taskService.cancelClaim(kadaiTaskId);
      Task taskWithCancelledClaim = this.taskService.getTask(kadaiTaskId);
      assertThat(taskWithCancelledClaim.getState()).isEqualTo(TaskState.READY);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify that the assignee for camunda task is no longer set
      boolean noAssigneeSet =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null);
      assertThat(noAssigneeSet).isTrue();

      // claim task in kadai and verify updated TaskState to be 'CLAIMED' again
      this.taskService.claim(kadaiTaskId);
      Task updatedTaskAfterAnotherClaim = this.taskService.getTask(kadaiTaskId);
      assertThat(updatedTaskAfterAnotherClaim.getState()).isEqualTo(TaskState.CLAIMED);

      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task again
      boolean assigneeSetSuccessfullyAgain =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertThat(assigneeSetSuccessfullyAgain).isTrue();
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_ReventLoopFromScheduledMethod_When_TryingToClaimNotAnymoreExistingCamundaTask()
      throws Exception {

    Logger camundaUtilRequesterLogger =
        (Logger) LoggerFactory.getLogger(CamundaUtilRequester.class);

    camundaUtilRequesterLogger.setLevel(Level.DEBUG);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    camundaUtilRequesterLogger.addAppender(listAppender);

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
      assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskId);

      // delete camunda process without notifying the listeners
      boolean camundaProcessCancellationSucessful =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, true);
      assertThat(camundaProcessCancellationSucessful).isTrue();

      // claim task in kadai and verify updated TaskState to be 'CLAIMED'
      String kadaiTaskId = kadaiTasks.get(0).getId();
      this.taskService.claim(kadaiTaskId);
      TaskState updatedTaskState = this.taskService.getTask(kadaiTaskId).getState();
      assertThat(updatedTaskState).isEqualTo(TaskState.CLAIMED);

      // wait for the adapter to claim the not anymore existing camunda task
      Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

      List<ILoggingEvent> logsList = listAppender.list;

      // verify that the CamundaUtilRequester log contains 1 entry for
      // the failed try to claim the not existing camunda task
      assertThat(logsList).hasSize(1);

      String camundaUtilRequesterLogMessage = logsList.get(0).getFormattedMessage();

      assertThat(camundaUtilRequesterLogMessage)
          .isEqualTo("Camunda Task " + camundaTaskId + " is not existing. Returning silently");
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_PreventLoopFromScheduledMethod_When_TryingToCancelClaimNotAnymoreExistingCamundaTask()
      throws Exception {

    Logger camundaUtilRequesterLogger =
        (Logger) LoggerFactory.getLogger(CamundaUtilRequester.class);

    camundaUtilRequesterLogger.setLevel(Level.DEBUG);
    ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
    listAppender.start();
    camundaUtilRequesterLogger.addAppender(listAppender);

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
      assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskId);

      // claim task in kadai and verify updated TaskState to be 'CLAIMED'
      String kadaiTaskId = kadaiTasks.get(0).getId();
      this.taskService.claim(kadaiTaskId);
      TaskState updatedTaskState = this.taskService.getTask(kadaiTaskId).getState();
      assertThat(updatedTaskState).isEqualTo(TaskState.CLAIMED);
      Thread.sleep((long) (this.adapterClaimPollingInterval * 1.2));

      // verify updated assignee for camunda task
      boolean assigneeSetSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1");
      assertThat(assigneeSetSuccessfully).isTrue();

      // delete camunda process without notifying the listeners
      boolean camundaProcessCancellationSucessful =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, true);
      assertThat(camundaProcessCancellationSucessful).isTrue();

      // cancel claim kadai task and verify updated TaskState to be 'READY'
      this.taskService.cancelClaim(kadaiTaskId);
      TaskState taskWithCancelledClaimState = this.taskService.getTask(kadaiTaskId).getState();
      assertThat(taskWithCancelledClaimState).isEqualTo(TaskState.READY);

      // wait for the adapter to try to cancel claim the not anymore existing camunda task
      Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

      List<ILoggingEvent> logsList = listAppender.list;

      // verify that the CamundaUtilRequester log contains 1 entry for
      // the failed try to cancel claim the not existing camunda task
      assertThat(logsList).hasSize(1);

      String camundaUtilRequesterLogMessage = logsList.get(0).getFormattedMessage();

      assertThat(camundaUtilRequesterLogMessage)
          .isEqualTo("Camunda Task " + camundaTaskId + " is not existing. Returning silently");
    }
  }
}
