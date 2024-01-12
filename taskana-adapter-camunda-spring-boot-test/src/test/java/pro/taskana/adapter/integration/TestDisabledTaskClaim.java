package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.taskana.utils.AwaitilityUtils.getTaskSummary;
import static pro.taskana.utils.AwaitilityUtils.verifyLogMessage;

import io.github.logrecorder.api.LogRecord;
import io.github.logrecorder.junit5.RecordLoggers;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskClaimCanceler;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskClaimer;
import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.task.api.TaskState;
import pro.taskana.task.api.models.Task;
import pro.taskana.task.api.models.TaskSummary;

@SpringBootTest(
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration
@ExtendWith(JaasExtension.class)
// This Test must be executed at the beginning, so that the logger test is working as expected
@Order(1)
class TestDisabledTaskClaim extends AbsIntegrationTest {

  @Autowired CamundaTaskClaimer camundaTaskClaimer;
  @Autowired CamundaTaskClaimCanceler camundaTaskClaimCanceler;

  @Value("${taskana.adapter.camunda.claiming.enabled}")
  private boolean claimingEnabled;

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  @RecordLoggers({CamundaTaskClaimer.class, CamundaTaskClaimCanceler.class})
  void should_NotClaimOrCancelClaimCamundaTask_When_CamundaClaimingDisabled(LogRecord log)
      throws Exception {
    setClaimingEnabled(false);

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_with_assignee_process", "");
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

    // verify that assignee is already set
    boolean assigneeAlreadySet =
        this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "someAssignee");
    assertThat(assigneeAlreadySet).isTrue();

    // verify that TaskState of taskana task is 'READY' first
    Task task = this.taskService.getTask(taskanaTaskId);
    assertThat(task.getState()).isEqualTo(TaskState.READY);

    // claim task in taskana and verify updated TaskState to be 'CLAIMED'
    this.taskService.claim(taskanaTaskId);
    Task updatedTask = this.taskService.getTask(taskanaTaskId);
    assertThat(updatedTask.getState()).isEqualTo(TaskState.CLAIMED);

    verifyLogMessage(
        adapterClaimPollingInterval,
        log,
        "Synchronizing claim of tasks in TASKANA to Camunda is set to false");

    // verify assignee for camunda task did not get updated
    boolean assigneeNotUpdated =
        this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "someAssignee");
    assertThat(assigneeNotUpdated).isTrue();

    // cancel claim TASKANA task
    taskService.forceCancelClaim(task.getId());

    verifyLogMessage(
        adapterCancelledClaimPollingInterval,
        log,
        "Synchronizing CancelClaim of Tasks in TASKANA to Camunda is set to false");

    // verify assignee for camunda task did not get updated
    assigneeNotUpdated =
        this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "someAssignee");
    assertThat(assigneeNotUpdated).isTrue();

    setClaimingEnabled(claimingEnabled);
  }

  private void setClaimingEnabled(boolean claimingEnbaled) throws Exception {

    Field claimingEnabled = camundaTaskClaimer.getClass().getDeclaredField("claimingEnabled");
    claimingEnabled.setAccessible(true);
    claimingEnabled.setBoolean(camundaTaskClaimer, claimingEnbaled);
    Field cancelClaimingEnabled =
        camundaTaskClaimCanceler.getClass().getDeclaredField("claimingEnabled");
    cancelClaimingEnabled.setAccessible(true);
    cancelClaimingEnabled.setBoolean(camundaTaskClaimCanceler, claimingEnbaled);
  }
}
