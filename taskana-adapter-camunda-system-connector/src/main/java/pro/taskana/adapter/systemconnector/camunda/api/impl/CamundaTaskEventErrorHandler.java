package pro.taskana.adapter.systemconnector.camunda.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import pro.taskana.adapter.systemconnector.api.ReferencedTask;

@Component
public class CamundaTaskEventErrorHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskEventErrorHandler.class);
  @Autowired HttpHeaderProvider httpHeaderProvider;
  @Autowired private RestTemplate restTemplate;

  public void decreaseRemainingRetriesAndLogErrorForReferencedTask(
      ReferencedTask referencedTask, Exception e, String camundaSystemTaskEventUrl) {

    LOGGER.debug(
        "entry to decreaseRemainingRetriesAndLogErrorForReferencedTasks, CamundSystemURL = {}",
        camundaSystemTaskEventUrl);

    String decreaseRemainingRetriesUrl =
        String.format(
            CamundaSystemConnectorImpl.URL_CAMUNDA_EVENT_DECREASE_REMAINING_RETRIES,
            Integer.valueOf(referencedTask.getOutboxEventId()));
    String requestUrl = camundaSystemTaskEventUrl + decreaseRemainingRetriesUrl;

    String failedTaskEventIdAndErrorLog =
        "{\"taskEventId\":"
            + referencedTask.getOutboxEventId()
            + ",\"errorLog\":\""
            + referencedTask.getId()
            + ":"
            + e.getCause()
            + "\"}";

    LOGGER.debug("decreaseRemainingRetriesAndLogError Events url {} ", requestUrl);

    decreaseRemainingRetriesAndLogError(requestUrl, failedTaskEventIdAndErrorLog);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("exit from decreaseRemainingRetriesAndLogErrorForReferencedTasks.");
    }
  }

  private void decreaseRemainingRetriesAndLogError(
      String requestUrl, String failedTaskEventIdAndErrorLog) {

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

    HttpEntity<String> request = new HttpEntity<>(failedTaskEventIdAndErrorLog, headers);

    restTemplate.postForObject(requestUrl, request, String.class);
  }
}
