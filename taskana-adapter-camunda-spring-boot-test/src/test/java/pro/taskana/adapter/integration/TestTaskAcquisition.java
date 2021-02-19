package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import uk.co.datumedge.hamcrest.json.SameJSONAs;

import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.common.api.exceptions.NotAuthorizedException;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.task.api.exceptions.TaskNotFoundException;
import pro.taskana.task.api.models.Task;
import pro.taskana.task.api.models.TaskSummary;

/** Test class to test the conversion of tasks generated by Camunda BPM to Taskana tasks. */
@SpringBootTest(
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
@SuppressWarnings("checkstyle:LineLength")
class TestTaskAcquisition extends AbsIntegrationTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestTaskAcquisition.class);

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  public void
      task_with_complex_variables_should_result_in_taskanaTask_with_those_variables_in_custom_attributes()
          throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_with_complex_variables_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    String expectedComplexProcessVariable =
        "{\"type\":\"object\","
            + "\"value\":\""
            + "{\\\"stringField\\\":\\\"\\\\fForm feed \\\\b Backspace \\\\t Tab"
            + " \\\\\\\\Backslash \\\\n newLine \\\\r Carriage return \\\\\\\" DoubleQuote\\\","
            + "\\\"intField\\\":1,\\\"doubleField\\\":1.1,\\\"booleanField\\\":false,"
            + "\\\"processVariableTestObjectTwoField\\\":["
            + "{\\\"stringFieldObjectTwo\\\":\\\"stringValueObjectTwo\\\","
            + "\\\"intFieldObjectTwo\\\":2,\\\"doubleFieldObjectTwo\\\":2.2,"
            + "\\\"booleanFieldObjectTwo\\\":true,"
            + "\\\"dateFieldObjectTwo\\\":\\\"1970-01-01 13:12:11\\\"}]}\","
            + "\"valueInfo\":{\"objectTypeName\":\"pro.taskana.impl.ProcessVariableTestObject\","
            + "\"serializationDataFormat\":\"application/json\"}}";

    String expectedPrimitiveProcessVariable1 =
        "{\"type\":\"integer\",\"value\":5," + "\"valueInfo\":null}";

    String expectedPrimitiveProcessVariable2 =
        "{\"type\":\"boolean\",\"value\":true," + "\"valueInfo\":null}";

    camundaTaskIds.forEach(
        camundaTaskId -> {
          Map<String, String> customAttributes =
              retrieveCustomAttributesFromNewTaskanaTask(camundaTaskId);

          assertThat(
              expectedComplexProcessVariable,
              SameJSONAs.sameJSONAs(customAttributes.get("camunda:attribute1")));
          assertThat(
              expectedPrimitiveProcessVariable1,
              SameJSONAs.sameJSONAs(customAttributes.get("camunda:attribute2")));
          assertThat(
              expectedPrimitiveProcessVariable2,
              SameJSONAs.sameJSONAs(customAttributes.get("camunda:attribute3")));
        });
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void user_task_process_instance_started_in_camunda_via_rest_should_result_in_taskanaTask()
      throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(taskanaTasks).hasSize(1);
      TaskSummary taskanaTaskSummary = taskanaTasks.get(0);
      String taskanaTaskExternalId = taskanaTaskSummary.getExternalId();
      assertThat(taskanaTaskExternalId).isEqualTo(camundaTaskId);
      String businessProcessId = taskanaTaskSummary.getBusinessProcessId();
      assertThat(processInstanceId).isEqualTo(businessProcessId);
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void
      multiple_user_task_process_instances_started_in_camunda_via_rest_should_result_in_multiple_taskanaTasks()
          throws Exception {

    int numberOfProcesses = 10;
    List<List<String>> camundaTaskIdsList = new ArrayList<>();
    for (int i = 0; i < numberOfProcesses; i++) {
      String processInstanceId =
          this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
              "simple_user_task_process", "");
      camundaTaskIdsList.add(
          this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId));
    }
    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (List<String> camundaTaskIds : camundaTaskIdsList) {
      for (String camundaTaskId : camundaTaskIds) {
        List<TaskSummary> taskanaTasks =
            this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
        assertThat(taskanaTasks).hasSize(1);
        String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
        assertThat(taskanaTaskExternalId).isEqualTo(camundaTaskId);
      }
    }
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void
      task_with_primitive_variables_should_result_in_taskanaTask_with_those_variables_in_custom_attributes()
          throws Exception {

    String variables =
        "\"variables\": {\"amount\": {\"value\":555, "
            + "\"type\":\"long\"},\"item\": {\"value\": \"item-xyz\"}}";
    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process", variables);
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    String expectedPrimitiveVariable1 = "{\"type\":\"long\",\"value\":555,\"valueInfo\":null}";

    String expectedPrimitiveVariable2 =
        "{\"type\":\"string\",\"value\":\"item-xyz\",\"valueInfo\":null}";

    camundaTaskIds.forEach(
        camundaTaskId -> {
          Map<String, String> customAttributes =
              retrieveCustomAttributesFromNewTaskanaTask(camundaTaskId);
          assertThat(
              expectedPrimitiveVariable1,
              SameJSONAs.sameJSONAs(customAttributes.get("camunda:amount")));
          assertThat(
              expectedPrimitiveVariable2,
              SameJSONAs.sameJSONAs(customAttributes.get("camunda:item")));
        });
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void
      task_with_big_complex_variables_should_result_in_taskanaTask_with_those_variables_in_custom_attributes()
          throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_with_big_complex_variables_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    List<TaskSummary> taskanaTasks =
        this.taskService.createTaskQuery().externalIdIn(camundaTaskIds.get(0)).list();
    assertThat(taskanaTasks).hasSize(1);

    TaskSummary taskanaTaskSummary = taskanaTasks.get(0);
    String taskanaTaskExternalId = taskanaTaskSummary.getExternalId();
    assertThat(taskanaTaskExternalId).isEqualTo(camundaTaskIds.get(0));

    Task taskanaTask = this.taskService.getTask(taskanaTaskSummary.getId());
    Map<String, String> taskanaTaskCustomAttributes = taskanaTask.getCustomAttributeMap();
    String variablesKeyString = "camunda:attribute1";
    String taskanaVariablesString = taskanaTaskCustomAttributes.get(variablesKeyString);

    assertTrue(taskanaVariablesString.length() > 1500000);
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void
      task_with_complex_variables_from_parent_execution_should_result_in_taskanaTasks_with_those_variables_in_custom_attributes()
          throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_multiple_user_tasks_with_complex_variables_process", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    Thread.sleep(this.adapterTaskPollingInterval);

    assertThat(camundaTaskIds).hasSize(3);

    // complete first 3 parallel tasks, one of which starts another task after completion that will
    // be checked for the process variables
    camundaTaskIds.forEach(
        camundaTaskId -> this.camundaProcessengineRequester.completeTaskWithId(camundaTaskId));

    camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(1);

    Thread.sleep(this.adapterTaskPollingInterval);

    String expectedComplexProcessVariable =
        "{\"type\":\"object\","
            + "\"value\":\""
            + "{\\\"stringField\\\":\\\"\\\\fForm feed \\\\b Backspace \\\\t Tab"
            + " \\\\\\\\Backslash \\\\n newLine \\\\r Carriage return \\\\\\\" DoubleQuote\\\","
            + "\\\"intField\\\":1,\\\"doubleField\\\":1.1,\\\"booleanField\\\":false,"
            + "\\\"processVariableTestObjectTwoField\\\":["
            + "{\\\"stringFieldObjectTwo\\\":\\\"stringValueObjectTwo\\\","
            + "\\\"intFieldObjectTwo\\\":2,\\\"doubleFieldObjectTwo\\\":2.2,"
            + "\\\"booleanFieldObjectTwo\\\":true,"
            + "\\\"dateFieldObjectTwo\\\":\\\"1970-01-01 13:12:11\\\"}]}\","
            + "\"valueInfo\":{\"objectTypeName\":\"pro.taskana.impl.ProcessVariableTestObject\","
            + "\"serializationDataFormat\":\"application/json\"}}";

    String expectedPrimitiveProcessVariable1 =
        "{\"type\":\"integer\",\"value\":5," + "\"valueInfo\":null}";

    String expectedPrimitiveProcessVariable2 =
        "{\"type\":\"boolean\",\"value\":true," + "\"valueInfo\":null}";
    camundaTaskIds.forEach(
        camundaTaskId -> {
          Map<String, String> customAttributes =
              retrieveCustomAttributesFromNewTaskanaTask(camundaTaskId);

          assertThat(
              expectedComplexProcessVariable,
              SameJSONAs.sameJSONAs(customAttributes.get("camunda:attribute1")));
          assertThat(
              expectedPrimitiveProcessVariable1,
              SameJSONAs.sameJSONAs(customAttributes.get("camunda:attribute2")));
          assertThat(
              expectedPrimitiveProcessVariable2,
              SameJSONAs.sameJSONAs(customAttributes.get("camunda:attribute3")));
        });
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void process_instance_with_multiple_executions_should_result_in_multiple_taskanaTasks()
      throws Exception {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_multiple_execution_process", "");
    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);
    assertThat(camundaTaskIds).hasSize(3);

    Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

    for (String camundaTaskId : camundaTaskIds) {
      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(taskanaTasks).hasSize(1);
      String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
      assertThat(taskanaTaskExternalId).isEqualTo(camundaTaskId);
    }
  }

  private Map<String, String> retrieveCustomAttributesFromNewTaskanaTask(String camundaTaskId) {

    Map<String, String> customAttributes = new HashMap<>();
    try {

      List<TaskSummary> taskanaTasks =
          this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
      assertThat(taskanaTasks).hasSize(1);
      TaskSummary taskanaTaskSummary = taskanaTasks.get(0);
      String taskanaTaskExternalId = taskanaTaskSummary.getExternalId();
      assertThat(taskanaTaskExternalId).isEqualTo(camundaTaskId);

      // get the actual task instead of summary to access custom attributes
      Task taskanaTask = this.taskService.getTask(taskanaTaskSummary.getId());

      customAttributes = taskanaTask.getCustomAttributeMap();
      return customAttributes;

    } catch (TaskNotFoundException | NotAuthorizedException e) {
      LOGGER.info(
          "Caught Exception while trying to retrieve custom attributes from new taskana task", e);
    }
    return customAttributes;
  }
}
