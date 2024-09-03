package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;

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
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import uk.co.datumedge.hamcrest.json.SameJSONAs;

/**
 * Test class to test the completion of camunda tasks upon completion of kadai tasks and vice
 * versa.
 */
@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
class TestCompletedTaskRetrieval extends AbsIntegrationTest {

  @WithAccessId(
      user = "teamlead_1",
      groups = {"admin"})
  @Test
  void should_CompleteCamundaTask_When_CompleteKadaiTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check kadaiTaskId
      List<TaskSummary> kadaiTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
      assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskId);
      String kadaiTaskId = kadaiTasks.get(0).getId();

      // claim and complete kadaiTask and wait
      this.taskService.claim(kadaiTaskId);
      this.taskService.completeTask(kadaiTaskId);
      Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

      // assert camunda task was completed; it should no longer exists as an active task but in the
      // history
      boolean taskRetrievalSuccessful =
          this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
      assertThat(taskRetrievalSuccessful).isFalse();
      boolean taskRetrievalFromHistorySuccessful =
          this.camundaProcessengineRequester.getTaskFromHistoryFromTaskId(camundaTaskId);
      assertThat(taskRetrievalFromHistorySuccessful).isTrue();
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_SetAssigneeAndCompleteCamundaTask_When_ForceCompleteKadaiTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {

      // retrieve and check kadaiTaskId
      List<TaskSummary> kadaiTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
      assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskId);
      String kadaiTaskId = kadaiTasks.get(0).getId();

      // verify that assignee is not yet set for camunda task
      boolean assigneeNotYetSet =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null);
      assertThat(assigneeNotYetSet).isTrue();

      // force complete kadaiTask and wait
      this.taskService.forceCompleteTask(kadaiTaskId);

      Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

      // verify that assignee got set with forced completion
      Task kadaiTask = this.taskService.getTask(kadaiTaskId);
      boolean assigneeUpdatedSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssigneeFromHistory(
              camundaTaskId, kadaiTask.getOwner());
      assertThat(assigneeUpdatedSuccessfully).isTrue();
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CompleteKadaiTask_When_CompleteCamundaTask() throws Exception {
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

      // complete camunda task and wait
      boolean camundaTaskCompletionSucessful =
          this.camundaProcessengineRequester.completeTaskWithId(camundaTaskId);
      assertThat(camundaTaskCompletionSucessful).isTrue();
      Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

      // assert kadai task was completed and still exists
      kadaiTasks = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(kadaiTasks).hasSize(1);
      Instant kadaiTaskCompletion = kadaiTasks.get(0).getCompleted();
      Instant kadaiTaskCreation = kadaiTasks.get(0).getCreated();
      assertThat(kadaiTaskCompletion).isNotNull();
      assertThat(kadaiTaskCompletion).isAfter(kadaiTaskCreation);
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_SetVariablesInCamunda_When_CompleteKadaiTaskWithTheseNewProcessVariables()
      throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_multiple_user_tasks_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check kadaiTaskId
    List<TaskSummary> kadaiTasks =
        this.taskService.createTaskQuery().externalIdIn(camundaTaskIds.get(0)).list();
    assertThat(kadaiTasks).hasSize(1);
    String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
    assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskIds.get(0));

    // create map for new process variables and set new process variables in it
    Map<String, String> newProcessVariables = new HashMap<>();

    newProcessVariables.put(
        "camunda:attribute1",
        "{\"type\":\"object\","
            + "\"value\":\"{\\\"stringField\\\":\\\"\\\\fForm feed \\\\b Backspace \\\\t Tab "
            + "\\\\\\\\Backslash \\\\n newLine \\\\r Carriage return \\\\\\\" DoubleQuote\\\","
            + "\\\"intField\\\":1,\\\"doubleField\\\":1.1,\\\"booleanField\\\":false,"
            + "\\\"processVariableTestObjectTwoField\\\":"
            + "[{\\\"stringFieldObjectTwo\\\":\\\"stringValueObjectTwo\\\","
            + "\\\"intFieldObjectTwo\\\":2,\\\"doubleFieldObjectTwo\\\":2.2,"
            + "\\\"booleanFieldObjectTwo\\\":true,\\\"dateFieldObjectTwo\\\":null}]}\","
            + "\"valueInfo\":{\"objectTypeName\":\"io.kadai.impl.ProcessVariableTestObject\","
            + "\"serializationDataFormat\":\"application/json\"}}");
    newProcessVariables.put(
        "camunda:attribute2",
        "{\"valueInfo\":{\"objectTypeName\":\"java.lang.Boolean\"},"
            + "\"type\":\"boolean\",\"value\":true}");
    newProcessVariables.put(
        "attribute3",
        "{\"valueInfo\":{\"objectTypeName\":\"java.lang.Integer\"},"
            + "\"type\":\"integer\",\"value\":5}");

    Task kadaiTask = this.taskService.getTask(kadaiTasks.get(0).getId());
    kadaiTask.setCustomAttributeMap(newProcessVariables);

    // update the task to set the process variables
    this.taskService.updateTask(kadaiTask);

    // complete task with setting process variables in camunda
    // and wait for adapter to create next task from
    // next user task in camunda
    kadaiTask = this.taskService.forceCompleteTask(kadaiTask.getId());

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    List<String> newCamundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check kadaiTaskId
    kadaiTasks = this.taskService.createTaskQuery().externalIdIn(newCamundaTaskIds.get(0)).list();
    assertThat(kadaiTasks).hasSize(1);
    kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
    assertThat(kadaiTaskExternalId).isEqualTo(newCamundaTaskIds.get(0));

    // retrieve the new created task from the new user task in camunda
    Task kadaiTask2 = this.taskService.getTask(kadaiTasks.get(0).getId());

    // make sure the task actually got completed
    assertThat(kadaiTask.getId()).isNotEqualTo(kadaiTask2.getId());

    // make sure that the prefixed process variables were set and successfully transfered back over
    // the outbox
    assertThat(kadaiTask2.getCustomAttributeMap()).hasSize(2);
    assertThat(
        kadaiTask.getCustomAttributeMap().get("camunda:attribute1"),
        SameJSONAs.sameJSONAs(kadaiTask2.getCustomAttributeMap().get("camunda:attribute1")));

    this.taskService.forceCompleteTask(kadaiTask2.getId());

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    newCamundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check kadaiTaskId
    kadaiTasks = this.taskService.createTaskQuery().externalIdIn(newCamundaTaskIds.get(0)).list();
    assertThat(kadaiTasks).hasSize(1);
    kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
    assertThat(kadaiTaskExternalId).isEqualTo(newCamundaTaskIds.get(0));

    // retrieve the new created task from the new user task in camunda
    Task kadaiTask3 = this.taskService.getTask(kadaiTasks.get(0).getId());

    assertThat(kadaiTask2.getId()).isNotEqualTo(kadaiTask3.getId());

    // make sure that the process variables were set and transfered successfully over the outbox
    assertThat(kadaiTask2.getCustomAttributeMap())
        .isEqualTo(kadaiTask3.getCustomAttributeMap());
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_UpdateCamundaVariables_When_CompleteKadaiTaskWithTheseUpdatedProcessVariables()
      throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_with_complex_variables_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check kadaiTaskId
    List<TaskSummary> kadaiTasks =
        this.taskService.createTaskQuery().externalIdIn(camundaTaskIds.get(0)).list();
    assertThat(kadaiTasks).hasSize(1);
    String kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
    assertThat(kadaiTaskExternalId).isEqualTo(camundaTaskIds.get(0));

    Task kadaiTask = this.taskService.getTask(kadaiTasks.get(0).getId());

    String alreadyExistingComplexProcessVariable =
        kadaiTask.getCustomAttributeMap().get("camunda:attribute1");

    String updatedExistingComplexProcessVariable =
        alreadyExistingComplexProcessVariable
            .replaceAll("\\\\\"doubleField\\\\\":1.1", "\\\\\"doubleField\\\\\":5.55")
            .replaceAll(
                "\\\\\"dateFieldObjectTwo\\\\\":\\\\\"1970-01-01 13:12:11\\\\\"",
                "\\\\\"dateFieldObjectTwo\\\\\":null");

    String alreadyExistingPrimitiveProcessVariable =
        kadaiTask.getCustomAttributeMap().get("camunda:attribute3");

    String updatedExistingPrimitiveProcessVariable =
        alreadyExistingPrimitiveProcessVariable.replaceAll("\"value\":true", "\"value\":false");

    Map<String, String> updatedProcessVariables = new HashMap<>();

    updatedProcessVariables.put("camunda:attribute1", updatedExistingComplexProcessVariable);
    updatedProcessVariables.put("camunda:attribute3", updatedExistingPrimitiveProcessVariable);

    kadaiTask.setCustomAttributeMap(updatedProcessVariables);

    // update the task to update the process variables
    this.taskService.updateTask(kadaiTask);

    // complete task with setting process variables in camunda
    // and wait for adapter to create next askana task from
    // next user task in camunda
    this.taskService.forceCompleteTask(kadaiTask.getId());

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    List<String> newCamundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check kadaiTaskId
    kadaiTasks = this.taskService.createTaskQuery().externalIdIn(newCamundaTaskIds.get(0)).list();
    assertThat(kadaiTasks).hasSize(1);
    kadaiTaskExternalId = kadaiTasks.get(0).getExternalId();
    assertThat(kadaiTaskExternalId).isEqualTo(newCamundaTaskIds.get(0));

    // retrieve the newly created task from the new user task in camunda
    Task kadaiTask2 = this.taskService.getTask(kadaiTasks.get(0).getId());

    // make sure the task actually got completed
    assertThat(kadaiTask.getId()).isNotEqualTo(kadaiTask2.getId());

    // make sure that the process variables were updated and transfered over the outbox
    assertThat(
        updatedExistingComplexProcessVariable,
        SameJSONAs.sameJSONAs(kadaiTask2.getCustomAttributeMap().get("camunda:attribute1")));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_PreventLoopFromScheduledMethod_When_TryingToCompleteNoLongerExistingCamundaTask()
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

      // complete task in kadai and verify updated TaskState to be 'COMPLETED'
      String kadaiTaskId = kadaiTasks.get(0).getId();
      this.taskService.forceCompleteTask(kadaiTaskId);
      TaskState updatedTaskState = this.taskService.getTask(kadaiTaskId).getState();
      assertThat(updatedTaskState).isEqualTo(TaskState.COMPLETED);

      // wait for the adapter to try to complete the not anymore existing camunda task
      Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

      List<ILoggingEvent> logsList = listAppender.list;

      // verify that the CamundaUtilRequester log contains 1 entry for
      // the failed try to complete the not existing camunda task
      assertThat(logsList).hasSize(1);

      String camundaUtilRequesterLogMessage = logsList.get(0).getFormattedMessage();

      assertThat(camundaUtilRequesterLogMessage)
          .isEqualTo("Camunda Task " + camundaTaskId + " is not existing. Returning silently");
    }
  }
}
