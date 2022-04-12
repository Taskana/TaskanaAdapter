package pro.taskana.adapter.integration;

import java.util.List;
import org.json.JSONException;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import pro.taskana.adapter.camunda.outbox.rest.resource.CamundaTaskEventListResource;

/** Class to assist with building requests against the TASKNA Outbox REST API. */
public class TaskanaOutboxRequester {

  private static final String BASIC_OUTBOX_PATH = "http://localhost:10020/outbox-rest/events";

  private final TestRestTemplate restTemplate;

  public TaskanaOutboxRequester(TestRestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public boolean deleteFailedEvent(int id) throws JSONException {

    String url = BASIC_OUTBOX_PATH + "/" + id;

    HttpEntity<String> requestEntity = prepareEntityFromBody("{}");
    ResponseEntity<String> answer =
        this.restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);

    if (HttpStatus.NO_CONTENT.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }

  public boolean deleteAllFailedEvents() throws JSONException {

    String url = BASIC_OUTBOX_PATH + "/delete-failed-events";

    HttpEntity<String> requestEntity = prepareEntityFromBody("{}");
    ResponseEntity<String> answer =
        this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

    if (HttpStatus.NO_CONTENT.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }

  public List<CamundaTaskEvent> getFailedEvents() {

    String url = BASIC_OUTBOX_PATH + "?retries=0";

    HttpEntity<String> requestEntity = prepareEntityFromBody("{}");
    ResponseEntity<CamundaTaskEventListResource> answer =
        this.restTemplate.exchange(
            url, HttpMethod.GET, requestEntity, CamundaTaskEventListResource.class);

    return answer.getBody().getCamundaTaskEvents();
  }

  public List<CamundaTaskEvent> getAllEvents() {

    String url = BASIC_OUTBOX_PATH;

    HttpEntity<String> requestEntity = prepareEntityFromBody("{}");
    ResponseEntity<CamundaTaskEventListResource> answer =
        this.restTemplate.exchange(
            url, HttpMethod.GET, requestEntity, CamundaTaskEventListResource.class);

    return answer.getBody().getCamundaTaskEvents();
  }

  public boolean setRemainingRetries(int id, int newRetries) throws JSONException {

    String url = BASIC_OUTBOX_PATH + "/" + id;

    HttpEntity<String> requestEntity =
        prepareEntityFromBody("{\"remainingRetries\":" + newRetries + "}");
    ResponseEntity<String> answer =
        this.restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, String.class);

    if (HttpStatus.OK.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }

  public boolean setRemainingRetriesForAll(int newRetries) throws JSONException {

    String url = BASIC_OUTBOX_PATH + "?retries=0";

    HttpEntity<String> requestEntity =
        prepareEntityFromBody("{\"remainingRetries\":" + newRetries + "}");
    ResponseEntity<String> answer =
        this.restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, String.class);

    if (HttpStatus.OK.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }

  /**
   * Helper method to create an HttpEntity from a provided body in JSON-format.
   *
   * @param jsonBody the body of the HttpEntity
   * @return the created HttpEntity
   */
  private HttpEntity<String> prepareEntityFromBody(String jsonBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<String>(jsonBody, headers);
  }
}
