package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.taskana.utils.AwaitilityUtils.getTaskSummary;
import static pro.taskana.utils.AwaitilityUtils.verifyAssigneeForCamundaTask;
import static pro.taskana.utils.AwaitilityUtils.verifyLogMessage;

import io.github.logrecorder.api.LogRecord;
import io.github.logrecorder.junit5.RecordLoggers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaUtilRequester;
import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.task.api.TaskState;
import pro.taskana.task.api.models.TaskSummary;

/**
 * Test class to test the synchronisation of assignees when tasks get claimed in camunda or taskana.
 */
@SpringBootTest(
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class TestTaskClaim extends AbsIntegrationTest {

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_ClaimUnclaimedCamundaTask_When_ClaimTaskanaTask() throws Exception {

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
    // verify that no assignee for camunda task is set yet
    assertThat(this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null)).isTrue();

    String taskanaTaskId = taskanaTaskSummary.getId();

    // verify that TaskState of taskana task is 'READY' first
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.READY);

    // claim task in taskana and verify updated TaskState to be 'CLAIMED'
    this.taskService.claim(taskanaTaskId);
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.CLAIMED);

    // verify updated assignee for camunda task
    verifyAssigneeForCamundaTask(
        adapterClaimPollingInterval,
        () -> this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1"));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_ClaimAlreadyClaimedCamundaTaska_When_ClaimTaskanaTask() throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_assignee_set", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    TaskSummary taskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTaskSummary.getExternalId());

    // verify that an assignee for camunda task is already set
    assertThat(this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null)).isFalse();

    String taskanaTaskId = taskanaTaskSummary.getId();

    // verify that TaskState of taskana task is 'READY' first
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.READY);

    // claim task in taskana and verify updated TaskState to be 'CLAIMED'
    this.taskService.claim(taskanaTaskId);
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.CLAIMED);

    // verify updated assignee for camunda task
    verifyAssigneeForCamundaTask(
        adapterClaimPollingInterval,
        () -> this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1"));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CancelClaimCamundaTask_When_CancelClaimTaskanaTask() throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    TaskSummary taskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTaskSummary.getExternalId());

    String taskanaTaskId = taskanaTaskSummary.getId();

    // verify that TaskState of taskana task is 'READY' first
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.READY);

    // claim task in taskana and verify updated TaskState to be 'CLAIMED'
    this.taskService.claim(taskanaTaskId);
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.CLAIMED);

    // verify updated assignee for camunda task
    verifyAssigneeForCamundaTask(
        adapterClaimPollingInterval,
        () -> this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1"));

    // cancel claim taskana task and verify updated TaskState to be 'READY'
    this.taskService.cancelClaim(taskanaTaskId);
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.READY);

    // verify that the assignee for camunda task is no longer set
    verifyAssigneeForCamundaTask(
        adapterClaimPollingInterval,
        () -> this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_ClaimCamundaTaskAgain_When_ClaimTaskanaTaskAfterCancelClaim() throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    TaskSummary taskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTaskSummary.getExternalId());

    String taskanaTaskId = taskanaTaskSummary.getId();

    // verify that TaskState of taskana task is 'READY' first
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.READY);

    // claim task in taskana and verify updated TaskState to be 'CLAIMED'
    this.taskService.claim(taskanaTaskId);
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.CLAIMED);

    // verify updated assignee for camunda task
    verifyAssigneeForCamundaTask(
        adapterClaimPollingInterval,
        () -> this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1"));

    // cancel claim taskana task and verify updated TaskState to be 'READY'
    this.taskService.cancelClaim(taskanaTaskId);
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.READY);

    // verify that the assignee for camunda task is no longer set
    verifyAssigneeForCamundaTask(
        adapterClaimPollingInterval,
        () -> this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null));

    // claim task in taskana and verify updated TaskState to be 'CLAIMED' again
    this.taskService.claim(taskanaTaskId);
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.CLAIMED);

    // verify updated assignee for camunda task again
    verifyAssigneeForCamundaTask(
        adapterClaimPollingInterval,
        () -> this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1"));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  @RecordLoggers(CamundaUtilRequester.class)
  void should_ReventLoopFromScheduledMethod_When_TryingToClaimNotAnymoreExistingCamundaTask(
      LogRecord log
  ) throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    TaskSummary taskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTaskSummary.getExternalId());

    String taskanaTaskId = taskanaTaskSummary.getId();

    // verify that TaskState of taskana task is 'READY' first
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.READY);

    // delete camunda process without notifying the listeners
    boolean camundaProcessCancellationSucessful =
        this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, true);
    assertThat(camundaProcessCancellationSucessful).isTrue();

    // claim task in taskana and verify updated TaskState to be 'CLAIMED'
    this.taskService.claim(taskanaTaskId);
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.CLAIMED);

    // wait for the adapter to claim the not anymore existing camunda task
    // verify that the CamundaUtilRequester log contains 1 entry for
    // the failed try to claim the not existing camunda task
    verifyLogMessage(
        adapterTaskPollingInterval,
        log,
        "Camunda Task " + camundaTaskId + " is not existing. Returning silently"
    );
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  @RecordLoggers(CamundaUtilRequester.class)
  void should_PreventLoopFromScheduledMethod_When_TryingToCancelClaimNotAnymoreExistingCamundaTask(
      LogRecord log
  ) throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    TaskSummary taskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTaskSummary.getExternalId());

    String taskanaTaskId = taskanaTaskSummary.getId();

    // verify that TaskState of taskana task is 'READY' first
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.READY);

    this.taskService.claim(taskanaTaskId);
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.CLAIMED);

    verifyAssigneeForCamundaTask(
        adapterClaimPollingInterval,
        () -> this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "teamlead_1"));

    // delete camunda process without notifying the listeners
    boolean camundaProcessCancellationSucessful =
        this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, true);
    assertThat(camundaProcessCancellationSucessful).isTrue();

    // cancel claim taskana task and verify updated TaskState to be 'READY'
    this.taskService.cancelClaim(taskanaTaskId);
    assertThat(this.taskService.getTask(taskanaTaskId).getState()).isEqualTo(TaskState.READY);

    // wait for the adapter to claim the not anymore existing camunda task
    // verify that the CamundaUtilRequester log contains 1 entry for
    // the failed try to claim the not existing camunda task
    verifyLogMessage(
        adapterTaskPollingInterval,
        log,
        "Camunda Task " + camundaTaskId + " is not existing. Returning silently"
    );
  }
}
