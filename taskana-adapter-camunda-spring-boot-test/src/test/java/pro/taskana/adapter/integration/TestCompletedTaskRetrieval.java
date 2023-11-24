package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static pro.taskana.utils.AwaitilityUtils.checkCamundaTaskIsCompleted;
import static pro.taskana.utils.AwaitilityUtils.getCamundaTaskId;
import static pro.taskana.utils.AwaitilityUtils.getDuration;
import static pro.taskana.utils.AwaitilityUtils.getTaskSummary;
import static pro.taskana.utils.AwaitilityUtils.verifyLogMessage;
import static pro.taskana.utils.ResourceUtils.getResourcesAsString;

import io.github.logrecorder.api.LogRecord;
import io.github.logrecorder.junit5.RecordLoggers;
import java.util.List;
import java.util.Map;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaUtilRequester;
import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.task.api.TaskState;
import pro.taskana.task.api.models.Task;
import pro.taskana.task.api.models.TaskSummary;
import uk.co.datumedge.hamcrest.json.SameJSONAs;

/**
 * Test class to test the completion of camunda tasks upon completion of taskana tasks and vice
 * versa.
 */
@SpringBootTest(
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class TestCompletedTaskRetrieval extends AbsIntegrationTest {

  @Autowired private JobExecutor jobExecutor;

  @WithAccessId(
      user = "teamlead_1",
      groups = {"admin"})
  @Test
  void should_CompleteCamundaTask_When_CompleteTaskanaTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    // retrieve and check taskanaTaskId
    TaskSummary taskanaTask =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTask.getExternalId());

    String taskanaTaskId = taskanaTask.getId();

    // claim and complete taskanaTask and wait
    this.taskService.claim(taskanaTaskId);
    this.taskService.completeTask(taskanaTaskId);

    // assert camunda task was completed; it should no longer exists as an active task
    // but in the history
    checkCamundaTaskIsCompleted(jobExecutor, camundaProcessengineRequester, camundaTaskId);
    await()
        .atMost(getDuration(jobExecutor.getMaxWait()))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(
            () -> this.camundaProcessengineRequester.getTaskFromHistoryFromTaskId(camundaTaskId),
            is(true));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_SetAssigneeAndCompleteCamundaTask_When_ForceCompleteTaskanaTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    // retrieve and check taskanaTaskId
    TaskSummary taskanaTask =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTask.getExternalId());

    String taskanaTaskId = taskanaTask.getId();

    // verify that assignee is not yet set for camunda task
    assertThat(this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null)).isTrue();

    // force complete taskanaTask and wait
    this.taskService.forceCompleteTask(taskanaTaskId);

    // verify that assignee got set with forced completion
    await()
        .atMost(getDuration(jobExecutor.getMaxWait()))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(
            () ->
                this.camundaProcessengineRequester.isCorrectAssigneeFromHistory(
                    camundaTaskId, this.taskService.getTask(taskanaTaskId).getOwner()),
            is(true));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CompleteTaskanaTask_When_CompleteCamundaTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    String camundaTaskId =
        this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId)
            .get(0);

    // retrieve and check taskanaTaskId
    TaskSummary taskanaTask =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list());

    assertThat(camundaTaskId).isEqualTo(taskanaTask.getExternalId());

    // complete camunda task and wait
    boolean camundaTaskCompletionSucessful =
        this.camundaProcessengineRequester.completeTaskWithId(camundaTaskId);
    assertThat(camundaTaskCompletionSucessful).isTrue();

    TaskSummary completedTaskanaTask =
        await()
            .atMost(getDuration(adapterCompletionPollingInterval))
            .with()
            .pollInterval(ONE_HUNDRED_MILLISECONDS)
            .until(
                () -> {
                  List<TaskSummary> completedTasks =
                      this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
                  if (!completedTasks.isEmpty()
                      && completedTasks.get(0).getState() == TaskState.COMPLETED) {
                    return completedTasks.get(0);
                  } else {
                    return null;
                  }
                },
                notNullValue());
    assertThat(completedTaskanaTask.getCompleted()).isAfter(completedTaskanaTask.getCreated());
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_SetVariablesInCamunda_When_CompleteTaskanaTaskWithTheseNewProcessVariables()
      throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_multiple_user_tasks_process", "");
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

    // create map for new process variables and set new process variables in it
    Map<String, String> newProcessVariables =
        Map.of(
            "camunda:attribute1",
            getResourcesAsString(this.getClass(), "process-variable-camunda-attribute1.json"),
            "camunda:attribute2",
            getResourcesAsString(this.getClass(), "process-variable-camunda-attribute2.json"),
            "attribute3",
            getResourcesAsString(this.getClass(), "process-variable-attribute3.json"));

    Task taskanaTask = this.taskService.getTask(taskanaTaskId);
    taskanaTask.setCustomAttributeMap(newProcessVariables);

    // update the task to set the process variables
    this.taskService.updateTask(taskanaTask);

    // complete task with setting process variables in camunda
    // and wait for adapter to create next task from
    // next user task in camunda
    taskanaTask = this.taskService.forceCompleteTask(taskanaTaskId);

    String updatedCamundaTaskId =
        getCamundaTaskId(
            adapterCompletionPollingInterval,
            () -> {
              List<String> updatedCamundaTaskIds =
                  this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(
                      processInstanceId);
              if (!updatedCamundaTaskIds.isEmpty()
                  && !camundaTaskId.contentEquals(updatedCamundaTaskIds.get(0))) {
                return updatedCamundaTaskIds.get(0);
              } else {
                return null;
              }
            });

    // retrieve and check taskanaTaskId
    TaskSummary updatedTaskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(updatedCamundaTaskId).list());
    assertThat(updatedCamundaTaskId).isEqualTo(updatedTaskanaTaskSummary.getExternalId());

    String updatedTaskanaTaskId = updatedTaskanaTaskSummary.getId();

    // retrieve the new created task from the new user task in camunda
    Task updatedTaskanaTask = this.taskService.getTask(updatedTaskanaTaskId);

    // make sure the task actually got completed
    assertThat(taskanaTaskId).isNotEqualTo(updatedTaskanaTaskId);

    // make sure that the prefixed process variables were set and successfully transfered back over
    // the outbox
    assertThat(updatedTaskanaTask.getCustomAttributeMap()).hasSize(2);
    assertThat(
        taskanaTask.getCustomAttributeMap().get("camunda:attribute1"),
        SameJSONAs.sameJSONAs(
            updatedTaskanaTask.getCustomAttributeMap().get("camunda:attribute1")));

    this.taskService.forceCompleteTask(updatedTaskanaTaskId);

    String completedCamundaTaskId =
        getCamundaTaskId(
            adapterCompletionPollingInterval,
            () -> {
              List<String> completedCamundaTaskIds =
                  this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(
                      processInstanceId);
              if (!completedCamundaTaskIds.isEmpty()
                  && !updatedCamundaTaskId.contentEquals(completedCamundaTaskIds.get(0))) {
                return completedCamundaTaskIds.get(0);
              } else {
                return null;
              }
            });

    // retrieve and check taskanaTaskId
    TaskSummary completedTaskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(completedCamundaTaskId).list());
    assertThat(completedCamundaTaskId).isEqualTo(completedTaskanaTaskSummary.getExternalId());

    String completedTaskanaTaskId = completedTaskanaTaskSummary.getId();

    // retrieve the new created task from the new user task in camunda
    Task completedTaskanaTask = this.taskService.getTask(completedTaskanaTaskId);

    assertThat(updatedTaskanaTaskId).isNotEqualTo(completedTaskanaTaskId);

    // make sure that the process variables were set and transfered successfully over the outbox
    assertThat(updatedTaskanaTask.getCustomAttributeMap())
        .isEqualTo(completedTaskanaTask.getCustomAttributeMap());
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_UpdateCamundaVariables_When_CompleteTaskanaTaskWithTheseUpdatedProcessVariables()
      throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_with_complex_variables_process", "");
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

    Task taskanaTask = this.taskService.getTask(taskanaTaskId);

    String alreadyExistingComplexProcessVariable =
        taskanaTask.getCustomAttributeMap().get("camunda:attribute1");

    String updatedExistingComplexProcessVariable =
        alreadyExistingComplexProcessVariable
            .replaceAll("\\\\\"doubleField\\\\\":1.1", "\\\\\"doubleField\\\\\":5.55")
            .replaceAll(
                "\\\\\"dateFieldObjectTwo\\\\\":\\\\\"1970-01-01 13:12:11\\\\\"",
                "\\\\\"dateFieldObjectTwo\\\\\":null");

    String alreadyExistingPrimitiveProcessVariable =
        taskanaTask.getCustomAttributeMap().get("camunda:attribute3");

    String updatedExistingPrimitiveProcessVariable =
        alreadyExistingPrimitiveProcessVariable.replaceAll("\"value\":true", "\"value\":false");

    Map<String, String> updatedProcessVariables =
        Map.of(
            "camunda:attribute1", updatedExistingComplexProcessVariable,
            "camunda:attribute3", updatedExistingPrimitiveProcessVariable);

    taskanaTask.setCustomAttributeMap(updatedProcessVariables);

    // update the task to update the process variables
    this.taskService.updateTask(taskanaTask);

    // complete task with setting process variables in camunda
    // and wait for adapter to create next askana task from
    // next user task in camunda
    this.taskService.forceCompleteTask(taskanaTaskId);

    String completedCamundaTaskId =
        getCamundaTaskId(
            adapterCompletionPollingInterval,
            () -> {
              List<String> completedCamundaTaskIds =
                  this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(
                      processInstanceId);
              if (!completedCamundaTaskIds.isEmpty()
                  && !camundaTaskId.contentEquals(completedCamundaTaskIds.get(0))) {
                return completedCamundaTaskIds.get(0);
              } else {
                return null;
              }
            });
    assertThat(completedCamundaTaskId).isNotEqualTo(camundaTaskId);

    // retrieve and check taskanaTaskId
    TaskSummary completedTaskanaTaskSummary =
        getTaskSummary(
            adapterTaskPollingInterval,
            () -> this.taskService.createTaskQuery().externalIdIn(completedCamundaTaskId).list());
    assertThat(completedCamundaTaskId).isEqualTo(completedTaskanaTaskSummary.getExternalId());

    String completedTaskanaTaskId = completedTaskanaTaskSummary.getId();

    // make sure the task actually got completed
    assertThat(taskanaTaskId).isNotEqualTo(completedTaskanaTaskId);

    // retrieve the newly created task from the new user task in camunda
    Task completedTaskanaTask = this.taskService.getTask(completedTaskanaTaskId);

    // make sure that the process variables were updated and transfered over the outbox
    assertThat(
        updatedExistingComplexProcessVariable,
        SameJSONAs.sameJSONAs(
            completedTaskanaTask.getCustomAttributeMap().get("camunda:attribute1")));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  @RecordLoggers(CamundaUtilRequester.class)
  void should_PreventLoopFromScheduledMethod_When_TryingToCompleteNoLongerExistingCamundaTask(
      LogRecord log) throws Exception {
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

    String taskanaTaskId = taskanaTaskSummary.getId();

    // delete camunda process without notifying the listeners
    boolean camundaProcessCancellationSucessful =
        this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, true);
    assertThat(camundaProcessCancellationSucessful).isTrue();

    // complete task in taskana and verify updated TaskState to be 'COMPLETED'
    this.taskService.forceCompleteTask(taskanaTaskId);
    TaskState updatedTaskState = this.taskService.getTask(taskanaTaskId).getState();
    assertThat(updatedTaskState).isEqualTo(TaskState.COMPLETED);

    // wait for the adapter to claim the not anymore existing camunda task
    verifyLogMessage(
        adapterTaskPollingInterval,
        log,
        "Camunda Task " + camundaTaskId + " is not existing. Returning silently");
  }
}
