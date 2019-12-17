package pro.taskana.adapter.integration;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;


/**
 * Class to assist with building requests against the Camunda REST API.
 *
 * @author Ben Fuernrohr
 */
public class CamundaProcessengineRequester {

    private final String processEngineKey;

    private static final String BASIC_ENGINE_PATH = "/rest/engine/";

    private static final String PROCESS_DEFINITION_KEY_PATH = "/process-definition/key/";

    private static final String PROCESS_DEFINITION_START_PATH = "/start";

    private static final String TASK_PATH = "/task";

    private static final String COMPLETE_TASK_PATH = "/complete";

    private static final String PROCESS_INSTANCE_PATH = "/process-instance";

    private static final String HISTORY_PATH = "/history";

    private final TestRestTemplate restTemplate;

    /**
     * Constructor for setting up a requester for a process engine with a key other than "default".
     *
     * @param processEngineKey
     *            the key of the camunda process engine to be called.
     * @param restTemplate
     *            the {@link TestRestTemplate} to be used for the REST calls.
     */
    public CamundaProcessengineRequester(String processEngineKey, TestRestTemplate restTemplate) {
        this.processEngineKey = processEngineKey;
        this.restTemplate = restTemplate;
    }

    /**
     * Default constructor to use the default process engine with its key "default".
     *
     * @param restTemplate
     *            the {@link TestRestTemplate} to be used for the REST calls.
     */
    public CamundaProcessengineRequester(TestRestTemplate restTemplate) {
        this.processEngineKey = "default";
        this.restTemplate = restTemplate;
    }

    /**
     * Starts an instance of the process with the given key in the process engine and returns its id. Requires a process
     * model of the given key to be already deployed within the process engine.
     *
     * @param processKey
     *            the key of the process to be started.
     * @param variables
     *            the variables passed at process start.
     * @return the internal id of the process instance.
     * @throws JSONException
     *             in case of JSON problems
     */
    public String startCamundaProcessAndReturnId(String processKey, String variables) throws JSONException {
        String url = BASIC_ENGINE_PATH + this.processEngineKey + PROCESS_DEFINITION_KEY_PATH + processKey
            + PROCESS_DEFINITION_START_PATH;
        HttpEntity<String> requestEntity = prepareEntityFromBody("{" + variables + "}");

        ResponseEntity<String> answer = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        JSONObject processJSON = new JSONObject(answer.getBody());
        String processId = (String) processJSON.get("id");
        return processId;
    }

    /**
     * Retrieves the current tasks active in the current executions of a given process instance. Since process instances
     * may have several executions due to AND-Splits, several tasks may be active in a given process instance.
     *
     * @param processInstanceId
     *            the id of the process instance for which tasks are to be retrieved.
     * @return a list of tasks currently active in the process instance. May be empty.
     * @throws JSONException
     *             in case of JSON problems
     */
    public List<String> getTaskIdsFromProcessInstanceId(String processInstanceId) throws JSONException {
        List<String> returnList = new ArrayList<String>();

        String url = BASIC_ENGINE_PATH + this.processEngineKey + TASK_PATH;
        HttpEntity<String> requestEntity = prepareEntityFromBody("{}");

        ResponseEntity<String> answer = this.restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        JSONArray tasklistJSON = new JSONArray(answer.getBody());
        for (int i = 0; i < tasklistJSON.length(); i++) {
            JSONObject taskJSON = (JSONObject) tasklistJSON.get(i);
            String taskId = (String) taskJSON.get("id");
            String taskProcessInstanceId = (String) taskJSON.get("processInstanceId");
            if (processInstanceId.equals(taskProcessInstanceId)) {
                returnList.add(taskId);
            }
        }
        return returnList;
    }

    /**
     * Completes the camunda task with the given Id. Returns true if successful.
     *
     * @param camundaTaskId
     *            the id of the task to be completed.
     * @return true if completion was successful. False on failure.
     * @throws JSONException
     *             in case of JSON problems
     */
    public boolean completeTaskWithId(String camundaTaskId) throws JSONException {
        String url = BASIC_ENGINE_PATH + this.processEngineKey + TASK_PATH + "/" + camundaTaskId + COMPLETE_TASK_PATH;
        HttpEntity<String> requestEntity = this.prepareEntityFromBody("{}");
        ResponseEntity<String> answer = this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
        if (answer.getStatusCode().equals(HttpStatus.NO_CONTENT)) {
            // successful requests give no answer with a body
            return true;
        } else {
            JSONObject answerJSON = new JSONObject(answer.getBody());
            String answerMessage = (String) answerJSON.get("message");
            if (answerMessage.contains("Cannot complete task")) {
                return false;
            }
            return false;
        }
    }

    /**
     * Retrieves the camunda task with the given id. Returns {@code true} if successful.
     *
     * @param camundaTaskId
     *            the id of the task to be retrieved.
     * @return {@code true} if retrieval was successful, {@code false} if not.
     * @throws JSONException
     *             in case of JSON problems
     */
    public boolean getTaskFromTaskId(String camundaTaskId) throws JSONException {
        String url = BASIC_ENGINE_PATH + this.processEngineKey + TASK_PATH + "/" + camundaTaskId;
        HttpEntity<String> requestEntity = prepareEntityFromBody("{}");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        JSONObject taskRetrievalAnswerJSON = new JSONObject(response.getBody());
        return !(HttpStatus.NOT_FOUND.equals(response.getStatusCode())
            && ((String) taskRetrievalAnswerJSON.get("message")).contains("No matching task with id")
            && "InvalidRequestException".equals(taskRetrievalAnswerJSON.get("type")));
    }

    /**
     * Retrieves the camunda task with the given id from camundas task history. Returns {@code true} if successful.
     *
     * @param camundaTaskId
     *            the id of the task to be retrieved.
     * @return {@code true} if retrieval was successful, {@code false} if not.
     * @throws JSONException
     *             in case of JSON problems
     */
    public boolean getTaskFromHistoryFromTaskId(String camundaTaskId) throws JSONException {
        String url = BASIC_ENGINE_PATH + this.processEngineKey + HISTORY_PATH + TASK_PATH + "/?taskId=" + camundaTaskId;
        HttpEntity<String> requestEntity = prepareEntityFromBody("{}");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        // no task found will only show in empty body
        JSONArray taskHistoryRetrievalAnswerJSON = new JSONArray(response.getBody());
        if (taskHistoryRetrievalAnswerJSON.length() == 0) {
            return false;
        } else {
            String historyTaskId = (String) ((JSONObject) taskHistoryRetrievalAnswerJSON.get(0)).get("id");
            if (camundaTaskId.contentEquals(historyTaskId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCorrectAssigneeFromHistory(String camundaTaskId, String assignee) throws JSONException {
        String url = BASIC_ENGINE_PATH + this.processEngineKey + HISTORY_PATH + TASK_PATH + "/?taskId=" + camundaTaskId;
        HttpEntity<String> requestEntity = prepareEntityFromBody("{}");
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        // no task found will only show in empty body
        JSONArray taskHistoryRetrievalAnswerJSON = new JSONArray(response.getBody());
        if (taskHistoryRetrievalAnswerJSON.length() == 0) {
            return false;
        } else {
            String camundaTaskAssignee = (String) ((JSONObject) taskHistoryRetrievalAnswerJSON.get(0)).get("assignee");
            if (assignee.equals(camundaTaskAssignee)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Deletes the camunda-process-instance with the given Id. Returns true if successful.
     *
     * @param processInstanceId
     *            the id of the process-instance to be delete.
     * @return true if deletion was successful. False on failure.
     * @throws JSONException
     *             in case of JSON problems
     */
    public boolean deleteProcessInstanceWithId(String processInstanceId) throws JSONException {
        String url = BASIC_ENGINE_PATH + this.processEngineKey + PROCESS_INSTANCE_PATH + "/" + processInstanceId;
        HttpEntity<String> requestEntity = prepareEntityFromBody("{}");
        ResponseEntity<String> answer = this.restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
        if (HttpStatus.NO_CONTENT.equals(answer.getStatusCode())) {
            return true;
        } else {
            JSONObject answerJSON = new JSONObject(answer.getBody());
            String answerMessage = (String) answerJSON.get("message");
            if (answerMessage.contains("Process instance with id") && answerMessage.contains("does not exist")) {
                return false;
            }
        }
        return false;
    }

    /**
     * Helper method to create an HttpEntity from a provided body in JSON-format.
     *
     * @param jsonBody
     *            the body of the HttpEntity
     * @return the created HttpEntity
     *
     */
    private HttpEntity<String> prepareEntityFromBody(String jsonBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<String>(jsonBody, headers);
    }

    /**
     * Determines if a provided assignee equals the assignee of a camunda task.
     *
     * @param assigneeValueToVerify
     *            a String of the assignee to verify
     * @param camundaTaskId
     *            the ID of the camunda task which will be checked for its assignee
     * @return true if the provided assignee equals the assignee of the camunda task, otherwise false
     *
     */
    public boolean isCorrectAssignee(String camundaTaskId, String assigneeValueToVerify) {

        String requestUrl = BASIC_ENGINE_PATH + this.processEngineKey + TASK_PATH + "/" + camundaTaskId;
        HttpEntity<String> requestEntity = prepareEntityFromBody("{}");
        ResponseEntity<String> responseEntity = restTemplate.exchange(requestUrl, HttpMethod.GET, requestEntity, String.class);
        JSONObject taskRetrievalAnswerJSON = new JSONObject(responseEntity.getBody());

        if (!taskRetrievalAnswerJSON.get("assignee").equals(null)) {

            String assignee = taskRetrievalAnswerJSON.getString("assignee");

            if (assignee.equals(assigneeValueToVerify)) {
                return true;
            }

        } else if (assigneeValueToVerify == null) {
            return true;
        }

        return false;
    }

}
