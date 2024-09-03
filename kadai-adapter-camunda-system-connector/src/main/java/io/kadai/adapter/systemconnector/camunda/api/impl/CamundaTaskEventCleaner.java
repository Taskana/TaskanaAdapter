package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Clears events in the Camunda outbox after the corresponding action has been carried out by KADAI.
 */
@Component
public class CamundaTaskEventCleaner {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskEventCleaner.class);
  @Autowired HttpHeaderProvider httpHeaderProvider;
  @Autowired private RestTemplate restTemplate;

  public void cleanEventsForReferencedTasks(
      List<ReferencedTask> referencedTasks, String camundaSystemTaskEventUrl) {

    LOGGER.debug(
        "entry to cleanEventsForReferencedTasks, CamundSystemURL = {}", camundaSystemTaskEventUrl);

    String requestUrl =
        camundaSystemTaskEventUrl + CamundaSystemConnectorImpl.URL_DELETE_CAMUNDA_EVENTS;

    if (referencedTasks == null || referencedTasks.isEmpty()) {
      return;
    }

    String idsOfCamundaTaskEventsToDeleteFromOutbox =
        getIdsOfCamundaTaskEventsToDeleteFromOutbox(referencedTasks);
    LOGGER.debug("delete Events url {} ", requestUrl);

    deleteCamundaTaskEventsFromOutbox(requestUrl, idsOfCamundaTaskEventsToDeleteFromOutbox);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("exit from cleanEventsForReferencedTasks.");
    }
  }

  private void deleteCamundaTaskEventsFromOutbox(
      String requestUrl, String idsOfCamundaTaskEventsToDeleteFromOutbox) {

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

    HttpEntity<String> request =
        new HttpEntity<>(idsOfCamundaTaskEventsToDeleteFromOutbox, headers);
    restTemplate.postForObject(requestUrl, request, String.class);
  }

  private String getIdsOfCamundaTaskEventsToDeleteFromOutbox(List<ReferencedTask> referencedTasks) {

    StringBuilder idsBuf = new StringBuilder();

    idsBuf.append("{\"taskCreationIds\":[");

    for (ReferencedTask referencedTask : referencedTasks) {
      idsBuf.append(referencedTask.getOutboxEventId().trim());
      idsBuf.append(',');
    }
    idsBuf.append("]}");
    return idsBuf.toString().replace(",]", "]");
  }
}
