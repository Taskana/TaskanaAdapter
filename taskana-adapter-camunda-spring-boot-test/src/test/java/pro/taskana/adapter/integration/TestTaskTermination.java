package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.taskana.utils.AwaitilityUtils.getTaskSummary;

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
import pro.taskana.task.api.models.TaskSummary;
import pro.taskana.utils.AwaitilityUtils;

/** Test class to test the completion of camunda tasks upon termination of taskana tasks. */
@SpringBootTest(
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class TestTaskTermination extends AbsIntegrationTest {

  @Autowired private JobExecutor jobExecutor;

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CompleteCamundaTask_When_TerminatingTaskanaTask() throws Exception {

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

    taskService.terminateTask(taskanaTaskSummary.getId());

    // check if camunda task got completed and therefore doesn't exist anymore
    AwaitilityUtils.checkCamundaTaskIsCompleted(
        jobExecutor, camundaProcessengineRequester, camundaTaskId);
  }
}
