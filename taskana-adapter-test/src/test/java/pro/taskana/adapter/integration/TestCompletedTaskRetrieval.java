package pro.taskana.adapter.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.List;

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

import pro.taskana.TaskSummary;
import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.exceptions.InvalidOwnerException;
import pro.taskana.exceptions.InvalidStateException;
import pro.taskana.exceptions.NotAuthorizedException;
import pro.taskana.exceptions.TaskNotFoundException;
import pro.taskana.security.JAASRunner;
import pro.taskana.security.WithAccessId;

/**
 * Test class to test the completion of camunda tasks upon completion of taskana tasks and vice versa.
 *
 * @author Ben Fuernrohr
 */
@SpringBootTest(classes = TaskanaAdapterTestApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@RunWith(JAASRunner.class)
@ContextConfiguration
public class TestCompletedTaskRetrieval extends AbsIntegrationTest {

    @WithAccessId(
        userName = "teamlead_1",
        groupNames = {"admin"})
    @Test
    public void completion_of_taskana_task_should_complete_camunda_task() throws TaskNotFoundException,
        NotAuthorizedException, JSONException, InterruptedException, InvalidOwnerException, InvalidStateException {
        String processInstanceId = this.camundaProcessengineRequester
            .startCamundaProcessAndReturnId("simple_user_task_process", "");
        List<String> camundaTaskIds = this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId);

        Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

        for (String camundaTaskId : camundaTaskIds) {
            // retrieve and check taskanaTaskId
            List<TaskSummary> taskanaTasks = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
            assertEquals(1, taskanaTasks.size());
            String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
            assertEquals(taskanaTaskExternalId, camundaTaskId);
            String taskanaTaskId = taskanaTasks.get(0).getTaskId();

            // claim and complete taskanaTask and wait
            this.taskService.claim(taskanaTaskId);
            this.taskService.completeTask(taskanaTaskId);
            Thread.sleep(this.adapterCompletionPollingInterval);

            // assert camunda task was completed; it should no longer exists as an active task but in the history
            boolean taskRetrievalSuccessful = this.camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId);
            assertFalse(taskRetrievalSuccessful);
            boolean taskRetrievalFromHistorySuccessful = this.camundaProcessengineRequester
                .getTaskFromHistoryFromTaskId(camundaTaskId);
            assertTrue(taskRetrievalFromHistorySuccessful);
        }
    }

    @WithAccessId(
        userName = "teamlead_1",
        groupNames = {"admin"})
    @Test
    public void completion_of_camunda_task_should_complete_taskana_task() throws JSONException, InterruptedException {
        String processInstanceId = this.camundaProcessengineRequester
            .startCamundaProcessAndReturnId("simple_user_task_process", "");
        List<String> camundaTaskIds = this.camundaProcessengineRequester
            .getTaskIdsFromProcessInstanceId(processInstanceId);

        Thread.sleep((long) (this.adapterTaskPollingInterval * 1.2));

        for (String camundaTaskId : camundaTaskIds) {
            // retrieve and check taskanaTaskId
            List<TaskSummary> taskanaTasks = this.taskService.createTaskQuery().externalIdIn(camundaTaskId).list();
            assertEquals(1, taskanaTasks.size());
            String taskanaTaskExternalId = taskanaTasks.get(0).getExternalId();
            assertEquals(taskanaTaskExternalId, camundaTaskId);

            // complete camunda task and wait
            boolean camundaTaskCompletionSucessful = this.camundaProcessengineRequester
                .completeTaskWithId(camundaTaskId);
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
}
