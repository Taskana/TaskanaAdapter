package io.kadai.adapter.systemconnector.camunda.api.impl;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.adapter.systemconnector.api.SystemResponse;
import io.kadai.adapter.systemconnector.camunda.config.CamundaSystemUrls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Cancels claims of tasks in camunda through the camunda REST-API that have a canceled claim in
 * KADAI.
 */
@Component
public class CamundaTaskClaimCanceler {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskClaimCanceler.class);
  @Autowired HttpHeaderProvider httpHeaderProvider;
  @Autowired private RestTemplate restTemplate;

  @Value("${kadai.adapter.camunda.claiming.enabled:false}")
  private boolean claimingEnabled;

  private boolean cancelClaimConfigLogged = false;

  public SystemResponse cancelClaimOfCamundaTask(
      CamundaSystemUrls.SystemUrlInfo camundaSystemUrlInfo, ReferencedTask referencedTask) {

    if (!cancelClaimConfigLogged) {
      LOGGER.info(
          String.format(
              "Synchronizing CancelClaim of Tasks in KADAI to Camunda is set to %b",
              claimingEnabled));
      cancelClaimConfigLogged = true;
    }

    if (claimingEnabled) {

      StringBuilder requestUrlBuilder = new StringBuilder();

      requestUrlBuilder
          .append(camundaSystemUrlInfo.getSystemRestUrl())
          .append(CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
          .append(referencedTask.getId())
          .append(CamundaSystemConnectorImpl.UNCLAIM_TASK);

      String requestBody = "{}";
      HttpHeaders headers = httpHeaderProvider.getHttpHeadersForCamundaRestApi();
      HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

      try {
        ResponseEntity<String> responseEntity =
            restTemplate.postForEntity(requestUrlBuilder.toString(), requestEntity, String.class);
        LOGGER.debug(
            "cancel claimed camunda task {}. Status code = {}",
            referencedTask.getId(),
            responseEntity.getStatusCode());

        return new SystemResponse(responseEntity.getStatusCode(), null);

      } catch (HttpStatusCodeException e) {
        if (CamundaUtilRequester.isTaskNotExisting(
            httpHeaderProvider, restTemplate, camundaSystemUrlInfo, referencedTask.getId())) {
          return new SystemResponse(HttpStatus.OK, null);
        } else {
          LOGGER.warn("Caught Exception when trying to cancel claim camunda task", e);
          throw e;
        }
      }
    }

    return new SystemResponse(HttpStatus.OK, null);
  }
}
