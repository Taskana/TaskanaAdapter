package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CamundaTaskEventErrorHandler {

  public static final int EVENT_STORE_ERROR_COLUMN_LIMIT = 1000;
  public static final String CAUSE_TREE_CUTOFF_TEXT = "...";
  public static final int COMMA_LENGTH = 1;
  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskEventErrorHandler.class);
  @Autowired HttpHeaderProvider httpHeaderProvider;
  @Autowired private RestTemplate restTemplate;

  public void decreaseRemainingRetriesAndLogErrorForReferencedTask(
      ReferencedTask referencedTask, Exception e, String camundaSystemTaskEventUrl) {

    LOGGER.debug(
        "entry to decreaseRemainingRetriesAndLogErrorForReferencedTasks, CamundaSystemURL = {}",
        camundaSystemTaskEventUrl);

    final String decreaseRemainingRetriesUrl =
        String.format(
            CamundaSystemConnectorImpl.URL_CAMUNDA_EVENT_DECREASE_REMAINING_RETRIES,
            Integer.valueOf(referencedTask.getOutboxEventId()));
    final String requestUrl = camundaSystemTaskEventUrl + decreaseRemainingRetriesUrl;

    JSONObject errorLog = createErrorLog(e);

    String failedTaskEventIdAndErrorLog =
        String.format(
            "{\"taskEventId\":%s,\"errorLog\":%s}", referencedTask.getOutboxEventId(), errorLog);

    LOGGER.debug("decreaseRemainingRetriesAndLogError Events url {} ", requestUrl);

    decreaseRemainingRetriesAndLogError(requestUrl, failedTaskEventIdAndErrorLog);

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("exit from decreaseRemainingRetriesAndLogErrorForReferencedTasks.");
    }
  }

  public void unlockEvent(String eventId, String camundaSystemTaskEventUrl) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("entry to unlockEvent, CamundaSystemURL = {}", camundaSystemTaskEventUrl);
    }
    final String unlockEventUrl =
        String.format(
            CamundaSystemConnectorImpl.URL_CAMUNDA_UNLOCK_EVENT, Integer.valueOf(eventId));
    final String requestUrl = camundaSystemTaskEventUrl + unlockEventUrl;

    LOGGER.debug("decreaseRemainingRetriesAndLogError Events url {} ", requestUrl);

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

    HttpEntity<String> request = new HttpEntity<>(headers);
    try {
      restTemplate.postForObject(requestUrl, request, String.class);
    } catch (Exception e) {
      LOGGER.error("Caught exception while trying to unlock event", e);
    }
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("exit from decreaseRemainingRetriesAndLogErrorForReferencedTasks.");
    }
  }

  private static JSONObject createErrorLog(Exception e) {
    JSONObject errorLog =
        new JSONObject()
            .put(
                "exception",
                new JSONObject()
                    .put("name", e.getClass().getName())
                    .put("message", e.getMessage()));

    JSONArray causeTree = new JSONArray();
    errorLog.put("cause", causeTree);
    // we don't want to compute the errorLog String length in every iteration (due to performance).
    // Therefore, we "count" the length manually.
    int errorLogStringLength = errorLog.toString().length();
    Throwable exceptionCause = e.getCause();
    while (exceptionCause != null) {
      JSONObject exceptionCauseJson =
          new JSONObject()
              .put("name", exceptionCause.getClass().getName())
              .put("message", exceptionCause.getMessage());
      int newErrorLogStringLength =
          errorLogStringLength + COMMA_LENGTH + exceptionCauseJson.toString().length();
      if (newErrorLogStringLength
          > EVENT_STORE_ERROR_COLUMN_LIMIT - COMMA_LENGTH - CAUSE_TREE_CUTOFF_TEXT.length() - 2) {
        causeTree.put(CAUSE_TREE_CUTOFF_TEXT);
        break;
      }
      causeTree.put(exceptionCauseJson);
      errorLogStringLength = newErrorLogStringLength;
      exceptionCause = exceptionCause.getCause();
    }

    return errorLog;
  }

  private void decreaseRemainingRetriesAndLogError(
      String requestUrl, String failedTaskEventIdAndErrorLog) {

    HttpHeaders headers = httpHeaderProvider.getHttpHeadersForOutboxRestApi();

    HttpEntity<String> request = new HttpEntity<>(failedTaskEventIdAndErrorLog, headers);
    try {
      restTemplate.postForObject(requestUrl, request, String.class);
    } catch (Exception e) {
      LOGGER.error("Caught exception while trying to decrease remaining retries and log error", e);
    }
  }
}
