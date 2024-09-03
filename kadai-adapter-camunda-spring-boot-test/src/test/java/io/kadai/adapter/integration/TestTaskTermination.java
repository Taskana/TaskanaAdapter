package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.task.api.models.TaskSummary;
import java.util.List;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

/** Test class to test the completion of camunda tasks upon termination of kadai tasks. */
@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
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
  void should_CompleteCamundaTask_When_TerminatingKadaiTask() throws Exception {

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

      taskService.terminateTask(kadaiTasks.get(0).getId());

      Thread.sleep(1000 + (long) (this.jobExecutor.getMaxWait() * 1.2));

      // check if camunda task got completed and therefore doesn't exist anymore
      boolean taskRetrievalSuccessful =
          this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
      assertThat(taskRetrievalSuccessful).isFalse();
    }
  }
}
