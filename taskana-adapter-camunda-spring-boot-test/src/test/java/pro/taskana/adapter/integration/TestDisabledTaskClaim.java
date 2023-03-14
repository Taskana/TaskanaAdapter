package pro.taskana.adapter.integration;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.lang.reflect.Field;
import java.util.List;
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
class TestDisabledTaskClaim extends AbsIntegrationTest {

  @Autowired CamundaTaskClaimer camundaTaskClaimer;
  @Autowired CamundaTaskClaimCanceler camundaTaskClaimCanceler;

  @Value("${taskana.adapter.camunda.claiming.enabled}")
  private boolean claimingEnabled;

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_NotClaimOrCancelClaimCamundaTask_When_CamundaClaimingDisabled() throws Exception {

    setClaimingEnabled(false);

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_with_assignee_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    // check that one new UserTask was started
    assertThat(camundaTaskIds).hasSize(1);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check external task id of created taskana task
    String camundaTaskId = camundaTaskIds.get(0);
    TaskSummary taskanaTask =
        this.taskService.createTaskQuery().externalIdIn(camundaTaskId).single();

    String taskanaTaskExternalId = taskanaTask.getExternalId();
    assertThat(taskanaTaskExternalId).isEqualTo(camundaTaskId);

    // verify that assignee is already set
    boolean assigneeAlreadySet =
        this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "someAssignee");
    assertThat(assigneeAlreadySet).isTrue();

    // verify that TaskState of taskana task is 'READY' first
    String taskanaTaskId = taskanaTask.getId();
    Task task = this.taskService.getTask(taskanaTaskId);
    assertThat(task.getState()).isEqualTo(TaskState.READY);

    // claim task in taskana and verify updated TaskState to be 'CLAIMED'
    this.taskService.claim(taskanaTaskId);
    Task updatedTask = this.taskService.getTask(taskanaTaskId);
    assertThat(updatedTask.getState()).isEqualTo(TaskState.CLAIMED);

    Thread.sleep((long) (this.adapterClaimPollingInterval * 1.3));
    // verify assignee for camunda task did not get updated
    boolean assigneeNotUpdated =
        this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, "someAssignee");
    assertThat(assigneeNotUpdated).isTrue();

    // cancel claim TASKANA task
    taskService.forceCancelClaim(task.getId());

    Thread.sleep((long) (this.adapterClaimPollingInterval * 1.3));

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
