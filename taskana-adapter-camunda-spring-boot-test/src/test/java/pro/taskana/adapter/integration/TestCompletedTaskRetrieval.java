package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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

  @WithAccessId(
      user = "teamlead_1",
      groups = {"admin"})
  @Test
  void should_CompleteCamundaTask_When_CompleteTaskanaTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      // retrieve and check taskanaTaskId
      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertEquals(1, taskanaTasks.size());
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertEquals(taskanaTaskExternalId, camundaTaskId);
      String taskanaTaskId = taskanaTasks.get(0).getId();

      // claim and complete taskanaTask and wait
      this.taskService.claim(taskanaTaskId);
      this.taskService.completeTask(taskanaTaskId);
      Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

      // assert camunda task was completed; it should no longer exists as an active task but in the
      // history
      boolean taskRetrievalSuccessful =
          this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
      assertFalse(taskRetrievalSuccessful);
      boolean taskRetrievalFromHistorySuccessful =
          this.camundaProcessengineRequester.getTaskFromHistoryFromTaskId(camundaTaskId);
      assertTrue(taskRetrievalFromHistorySuccessful);
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_SetAssigneeAndCompleteCamundaTask_When_ForceCompleteTaskanaTask() throws Exception {
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {

      // retrieve and check taskanaTaskId
      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertEquals(1, taskanaTasks.size());
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertEquals(taskanaTaskExternalId, camundaTaskId);
      String taskanaTaskId = taskanaTasks.get(0).getId();

      // verify that assignee is not yet set for camunda task
      boolean assigneeNotYetSet =
          this.camundaProcessengineRequester.isCorrectAssignee(camundaTaskId, null);
      assertTrue(assigneeNotYetSet);

      // force complete taskanaTask and wait
      this.taskService.forceCompleteTask(taskanaTaskId);

      Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

      // verify that assignee got set with forced completion
      Task taskanaTask = this.taskService.getTask(taskanaTaskId);
      boolean assigneeUpdatedSuccessfully =
          this.camundaProcessengineRequester.isCorrectAssigneeFromHistory(
              camundaTaskId, taskanaTask.getOwner());
      assertTrue(assigneeUpdatedSuccessfully);
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CompleteTaskanaTask_When_CompleteCamundaTask() throws Exception {
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

      // complete camunda task and wait
      boolean camundaTaskCompletionSucessful =
          this.camundaProcessengineRequester.completeTaskWithId(camundaTaskId);
      assertTrue(camundaTaskCompletionSucessful);
      Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

      // assert taskana task was completed and still exists
      taskanaTasks = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertEquals(1, taskanaTasks.size());
      Instant taskanaTaskCompletion = taskanaTasks.get(0).getCompleted();
      Instant taskanaTaskCreation = taskanaTasks.get(0).getCreated();
      assertFalse(taskanaTaskCompletion == null);
      assertEquals(1, taskanaTaskCompletion.compareTo(taskanaTaskCreation));
    }
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
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check taskanaTaskId
    List<TaskSummary> taskanaTasks =
        this.taskService.createTaskQuery().externalIdIn(camundaTaskIds.get(0)).list();
    assertThat(taskanaTasks).hasSize(1);
    String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
    assertThat(taskanaTaskExternalId).isEqualTo(camundaTaskIds.get(0));

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
            + "\"valueInfo\":{\"objectTypeName\":\"pro.taskana.impl.ProcessVariableTestObject\","
            + "\"serializationDataFormat\":\"application/json\"}}");
    newProcessVariables.put(
        "camunda:attribute2",
        "{\"valueInfo\":{\"objectTypeName\":\"java.lang.Boolean\"},"
            + "\"type\":\"boolean\",\"value\":true}");
    newProcessVariables.put(
        "attribute3",
        "{\"valueInfo\":{\"objectTypeName\":\"java.lang.Integer\"},"
            + "\"type\":\"integer\",\"value\":5}");

    Task taskanaTask = this.taskService.getTask(taskanaTasks.get(0).getId());
    taskanaTask.setCustomAttributeMap(newProcessVariables);

    // update the task to set the process variables
    this.taskService.updateTask(taskanaTask);

    // complete task with setting process variables in camunda
    // and wait for adapter to create next task from
    // next user task in camunda
    taskanaTask = this.taskService.forceCompleteTask(taskanaTask.getId());

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    List<String> newCamundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check taskanaTaskId
    taskanaTasks = this.taskService.createTaskQuery().externalIdIn(newCamundaTaskIds.get(0)).list();
    assertThat(taskanaTasks).hasSize(1);
    taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
    assertThat(taskanaTaskExternalId).isEqualTo(newCamundaTaskIds.get(0));

    // retrieve the new created task from the new user task in camunda
    Task taskanaTask2 = this.taskService.getTask(taskanaTasks.get(0).getId());

    // make sure the task actually got completed
    assertThat(taskanaTask.getId()).isNotEqualTo(taskanaTask2.getId());

    // make sure that the prefixed process variables were set and successfully transfered back over
    // the outbox
    assertThat(taskanaTask2.getCustomAttributeMap()).hasSize(2);
    assertThat(
        taskanaTask.getCustomAttributeMap().get("camunda:attribute1"),
        SameJSONAs.sameJSONAs(taskanaTask2.getCustomAttributeMap().get("camunda:attribute1")));

    this.taskService.forceCompleteTask(taskanaTask2.getId());

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    newCamundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check taskanaTaskId
    taskanaTasks = this.taskService.createTaskQuery().externalIdIn(newCamundaTaskIds.get(0)).list();
    assertThat(taskanaTasks).hasSize(1);
    taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
    assertThat(taskanaTaskExternalId).isEqualTo(newCamundaTaskIds.get(0));

    // retrieve the new created task from the new user task in camunda
    Task taskanaTask3 = this.taskService.getTask(taskanaTasks.get(0).getId());

    assertThat(taskanaTask2.getId()).isNotEqualTo(taskanaTask3.getId());

    // make sure that the process variables were set and transfered successfully over the outbox
    assertThat(taskanaTask2.getCustomAttributeMap())
        .isEqualTo(taskanaTask3.getCustomAttributeMap());
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
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check taskanaTaskId
    List<TaskSummary> taskanaTasks =
        this.taskService.createTaskQuery().externalIdIn(camundaTaskIds.get(0)).list();
    assertThat(taskanaTasks).hasSize(1);
    String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
    assertThat(taskanaTaskExternalId).isEqualTo(camundaTaskIds.get(0));

    Task taskanaTask = this.taskService.getTask(taskanaTasks.get(0).getId());

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

    Map<String, String> updatedProcessVariables = new HashMap<>();

    updatedProcessVariables.put("camunda:attribute1", updatedExistingComplexProcessVariable);
    updatedProcessVariables.put("camunda:attribute3", updatedExistingPrimitiveProcessVariable);

    taskanaTask.setCustomAttributeMap(updatedProcessVariables);

    // update the task to update the process variables
    this.taskService.updateTask(taskanaTask);

    // complete task with setting process variables in camunda
    // and wait for adapter to create next askana task from
    // next user task in camunda
    this.taskService.forceCompleteTask(taskanaTask.getId());

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    List<String> newCamundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check taskanaTaskId
    taskanaTasks = this.taskService.createTaskQuery().externalIdIn(newCamundaTaskIds.get(0)).list();
    assertThat(taskanaTasks).hasSize(1);
    taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
    assertThat(taskanaTaskExternalId).isEqualTo(newCamundaTaskIds.get(0));

    // retrieve the newly created task from the new user task in camunda
    Task taskanaTask2 = this.taskService.getTask(taskanaTasks.get(0).getId());

    // make sure the task actually got completed
    assertThat(taskanaTask.getId()).isNotEqualTo(taskanaTask2.getId());

    // make sure that the process variables were updated and transfered over the outbox
    assertThat(
        updatedExistingComplexProcessVariable,
        SameJSONAs.sameJSONAs(taskanaTask2.getCustomAttributeMap().get("camunda:attribute1")));
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

      // retrieve and check taskanaTaskId
      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(taskanaTasks).hasSize(1);
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertThat(taskanaTaskExternalId).isEqualTo(camundaTaskId);

      // delete camunda process without notifying the listeners
      boolean camundaProcessCancellationSucessful =
          this.camundaProcessengineRequester.deleteProcessInstanceWithId(processInstanceId, true);
      assertThat(camundaProcessCancellationSucessful).isTrue();

      // complete task in taskana and verify updated TaskState to be 'COMPLETED'
      String taskanaTaskId = taskanaTasks.get(0).getId();
      this.taskService.forceCompleteTask(taskanaTaskId);
      TaskState updatedTaskState = this.taskService.getTask(taskanaTaskId).getState();
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
