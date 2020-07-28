package pro.taskana.adapter.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.security.JaasExtension;
import pro.taskana.security.WithAccessId;
import pro.taskana.task.api.models.Task;
import pro.taskana.task.api.models.TaskSummary;

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
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  void completion_of_taskana_task_should_complete_camunda_task() throws Exception {
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
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  void forced_completion_of_taskana_task_should_set_assignee_and_complete_camunda_task()
      throws Exception {
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
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  void completion_of_camunda_task_should_complete_taskana_task() throws Exception {
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
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  void completion_of_taskana_task_with_new_process_variables_should_set_these_variables_in_camunda()
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
    assertEquals(1, taskanaTasks.size());
    String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
    assertEquals(taskanaTaskExternalId, camundaTaskIds.get(0));

    Task taskanaTask = this.taskService.getTask(taskanaTasks.get(0).getId());

    // create map for new process variables and set new process variables in it
    Map<String, String> newProcessVariables = new HashMap<>();

    String newProcessVariablesJson =
        "{\"attribute1\":{\"type\":\"Object\",\"value\":"
            + "\"{\\\"stringField\\\":\\\"\\\\fForm feed \\\\b Backspace \\\\t Tab"
            + " \\\\\\\\Backslash "
            + "\\\\n newLine \\\\r Carriage return \\\\\\\" DoubleQuote\\\",\\\"intField\\\":1,"
            + "\\\"doubleField\\\":1.1,\\\"booleanField\\\":false,"
            + "\\\"processVariableTestObjectTwoField\\\":"
            + "{\\\"stringFieldObjectTwo\\\":\\\"stringValueObjectTwo\\\","
            + "\\\"intFieldObjectTwo\\\":2,\\\"doubleFieldObjectTwo\\\":2.2,"
            + "\\\"booleanFieldObjectTwo\\\":true,\\\"dateFieldObjectTwo\\\":null}}\","
            + "\"valueInfo\":{\"objectTypeName\":\"pro.taskana.impl.ProcessVariableTestObject\","
            + "\"serializationDataFormat\":\"application/json\"}},"
            + "\"attribute2\":{\"type\":\"Integer\",\"value\":5,"
            + "\"valueInfo\":{\"objectTypeName\":\"java.lang.Integer\"}},"
            + "\"attribute3\":{\"type\":\"Boolean\",\"value\":true,"
            + "\"valueInfo\":{\"objectTypeName\":\"java.lang.Boolean\"}}}";

    newProcessVariables.put("referenced_task_variables", newProcessVariablesJson);

    taskanaTask.setCustomAttributes(newProcessVariables);

    // update the task to set the process variables
    this.taskService.updateTask(taskanaTask);

    // complete task with setting process variables in camunda
    // and wait for adapter to create next task from
    // next user task in camunda
    this.taskService.forceCompleteTask(taskanaTask.getId());

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    List<String> newCamundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check taskanaTaskId
    taskanaTasks = this.taskService.createTaskQuery().externalIdIn(newCamundaTaskIds.get(0)).list();
    assertEquals(1, taskanaTasks.size());
    taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
    assertEquals(taskanaTaskExternalId, newCamundaTaskIds.get(0));

    // retrieve the new created task from the new user task in camunda
    Task taskanaTask2 = this.taskService.getTask(taskanaTasks.get(0).getId());

    // make sure the task actually got completed
    assertFalse(taskanaTask.getId().equals(taskanaTask2.getId()));

    // make sure that the process variables were set and transfered successfully over the outbox
    assertTrue(taskanaTask.getCustomAttributes().equals(taskanaTask2.getCustomAttributes()));
  }

  @WithAccessId(
      userName = "teamlead_1",
      groupNames = {"admin"})
  @Test
  void completion_of_taskana_task_with_updated_process_variables_should_update_camunda_variables()
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
    assertEquals(1, taskanaTasks.size());
    String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
    assertEquals(taskanaTaskExternalId, camundaTaskIds.get(0));

    Task taskanaTask = this.taskService.getTask(taskanaTasks.get(0).getId());

    String alreadyExistingProcessVariables =
        "{\"attribute1\":{\"type\":\"Object\",\"value\":"
            + "\"{\\\"stringField\\\":\\\"\\\\fForm feed \\\\b Backspace \\\\t Tab"
            + " \\\\\\\\Backslash "
            + "\\\\n newLine \\\\r Carriage return \\\\\\\" DoubleQuote\\\",\\\"intField\\\":1,"
            + "\\\"doubleField\\\":1.1,\\\"booleanField\\\":false,"
            + "\\\"processVariableTestObjectTwoField\\\":"
            + "{\\\"stringFieldObjectTwo\\\":\\\"stringValueObjectTwo\\\","
            + "\\\"intFieldObjectTwo\\\":2,\\\"doubleFieldObjectTwo\\\":2.2,"
            + "\\\"booleanFieldObjectTwo\\\":true,"
            + "\\\"dateFieldObjectTwo\\\":\\\"1970-01-01 13:12:11\\\"}}\","
            + "\"valueInfo\":{\"objectTypeName\":\"pro.taskana.impl.ProcessVariableTestObject\","
            + "\"serializationDataFormat\":\"application/json\"}},"
            + "\"attribute2\":{\"type\":\"Integer\",\"value\":5,"
            + "\"valueInfo\":{\"objectTypeName\":\"java.lang.Integer\"}},"
            + "\"attribute3\":{\"type\":\"Boolean\",\"value\":true,"
            + "\"valueInfo\":{\"objectTypeName\":\"java.lang.Boolean\"}}}";

    // check that existing process variables are already set
    assertTrue(
        taskanaTask
            .getCustomAttributes()
            .get("referenced_task_variables")
            .equals(alreadyExistingProcessVariables));

    // create map for updated process variables and set new process variables in it
    Map<String, String> updatedProcessVariables = new HashMap<>();

    // update some values
    String updatedProcessVariablesJson =
        alreadyExistingProcessVariables
            .replaceAll("\\\\\"doubleField\\\\\":1.1", "\\\\\"doubleField\\\\\":5.55")
            .replaceAll(
                "\\\\\"dateFieldObjectTwo\\\\\":\\\\\"1970-01-01 13:12:11\\\\\"",
                "\\\\\"dateFieldObjectTwo\\\\\":null");

    updatedProcessVariables.put("referenced_task_variables", updatedProcessVariablesJson);

    taskanaTask.setCustomAttributes(updatedProcessVariables);

    // update the task to update the process variables
    this.taskService.updateTask(taskanaTask);

    // complete task with setting process variables in camunda
    // and wait for adapter to create next task from
    // next user task in camunda
    this.taskService.forceCompleteTask(taskanaTask.getId());

    Thread.sleep((long) (this.adapterCompletionPollingInterval * 1.2));

    List<String> newCamundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    // retrieve and check taskanaTaskId
    taskanaTasks = this.taskService.createTaskQuery().externalIdIn(newCamundaTaskIds.get(0)).list();
    assertEquals(1, taskanaTasks.size());
    taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
    assertEquals(taskanaTaskExternalId, newCamundaTaskIds.get(0));

    // retrieve the newly created task from the new user task in camunda
    Task taskanaTask2 = this.taskService.getTask(taskanaTasks.get(0).getId());

    // make sure the task actually got completed
    assertFalse(taskanaTask.getId().equals(taskanaTask2.getId()));

    // make sure that the process variables were updated and transfered over the outbox
    assertTrue(
        !alreadyExistingProcessVariables.equals(taskanaTask2.getCustomAttributes())
            && updatedProcessVariables.equals(taskanaTask2.getCustomAttributes()));
  }
}
