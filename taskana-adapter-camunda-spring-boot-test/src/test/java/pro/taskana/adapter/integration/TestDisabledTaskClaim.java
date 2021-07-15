package pro.taskana.adapter.integration;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.jmx.support.RegistrationPolicy;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.task.api.TaskState;
import pro.taskana.task.api.models.Task;
import pro.taskana.task.api.models.TaskSummary;

@SpringBootTest(
    properties = {
      "camunda.bpm.generate-unique-process-engine-name=true",
      "camunda.bpm.generate-unique-process-application-name=true",
      "spring.datasource.generate-unique-name=true",
    },
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ContextConfiguration
@ExtendWith(JaasExtension.class)
@DirtiesContext(classMode = ClassMode.BEFORE_CLASS)
@EnableMBeanExport(registration = RegistrationPolicy.REPLACE_EXISTING)
@TestPropertySource(
    properties = {"taskana.adapter.camunda.claiming.enabled=false", "server.port=10813"})
@Disabled(
    "Sometimes fails due to flaky Spring Boot integration test behavior. "
        + "Test itself is fine but disabled for now ")
class TestDisabledTaskClaim extends AbsIntegrationTest {

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_NotClaimOrCancelClaimCamundaTask_When_CamundaClaimingDisabled() throws Exception {

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
  }
}
