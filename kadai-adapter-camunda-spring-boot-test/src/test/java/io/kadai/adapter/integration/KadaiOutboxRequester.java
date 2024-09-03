package io.kadai.adapter.integration;

import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import io.kadai.adapter.camunda.outbox.rest.resource.CamundaTaskEventListResource;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import java.util.List;
import org.json.JSONException;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Class to assist with building requests against the TASKNA Outbox REST API. */
public class KadaiOutboxRequester {

  private static final String BASIC_OUTBOX_PATH = "http://localhost:10020/outbox-rest/events";

  private final TestRestTemplate restTemplate;

  private final HttpHeaderProvider httpHeaderProvider;

  public KadaiOutboxRequester(
      TestRestTemplate restTemplate, HttpHeaderProvider httpHeaderProvider) {
    this.restTemplate = restTemplate;
    this.httpHeaderProvider = httpHeaderProvider;
  }

  public boolean deleteFailedEvent(int id) throws JSONException {

    String url = BASIC_OUTBOX_PATH + "/" + id;

    HttpEntity<String> requestEntity =
        httpHeaderProvider.prepareNewEntityForOutboxRestApi("{}");
    ResponseEntity<String> answer =
        this.restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);

    if (HttpStatus.NO_CONTENT.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }

  public boolean deleteAllFailedEvents() throws JSONException {

    String url = BASIC_OUTBOX_PATH + "/delete-failed-events";

    HttpEntity<String> requestEntity =
        httpHeaderProvider.prepareNewEntityForOutboxRestApi("{}");
    ResponseEntity<String> answer =
        this.restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

    if (HttpStatus.NO_CONTENT.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }

  public List<CamundaTaskEvent> getFailedEvents() {

    String url = BASIC_OUTBOX_PATH + "?retries=0";

    HttpEntity<Void> requestEntity = httpHeaderProvider.prepareNewEntityForOutboxRestApi();
    ResponseEntity<CamundaTaskEventListResource> answer =
        this.restTemplate.exchange(
            url, HttpMethod.GET, requestEntity, CamundaTaskEventListResource.class);

    return answer.getBody().getCamundaTaskEvents();
  }

  public List<CamundaTaskEvent> getAllEvents() {

    String url = BASIC_OUTBOX_PATH;

    HttpEntity<Void> requestEntity = httpHeaderProvider.prepareNewEntityForOutboxRestApi();
    ResponseEntity<CamundaTaskEventListResource> answer =
        this.restTemplate.exchange(
            url, HttpMethod.GET, requestEntity, CamundaTaskEventListResource.class);

    return answer.getBody().getCamundaTaskEvents();
  }

  public boolean setRemainingRetries(int id, int newRetries) throws JSONException {

    String url = BASIC_OUTBOX_PATH + "/" + id;

    HttpEntity<String> requestEntity =
        httpHeaderProvider.prepareNewEntityForOutboxRestApi(
            "{\"remainingRetries\":" + newRetries + "}");
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
        httpHeaderProvider.prepareNewEntityForOutboxRestApi(
            "{\"remainingRetries\":" + newRetries + "}");
    ResponseEntity<String> answer =
        this.restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, String.class);

    if (HttpStatus.OK.equals(answer.getStatusCode())) {
      return true;
    }
    return false;
  }
}
