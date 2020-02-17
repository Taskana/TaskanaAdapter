package pro.taskana.adapter.systemconnector.camunda.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemResponse;
import pro.taskana.adapter.systemconnector.camunda.config.CamundaSystemUrls;
import pro.taskana.common.api.exceptions.SystemException;

/**
 * Completes Camunda Tasks via the Camunda REST Api.
 *
 * @author bbr
 */
public class CamundaTaskCompleter {

  private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskCompleter.class);

  private static final String COMPLETED_BY_TASKANA_ADAPTER_LOCAL_VARIABLE =
      "completedByTaskanaAdapter";

  @Autowired private RestTemplate restTemplate;

  public SystemResponse completeCamundaTask(
      CamundaSystemUrls.SystemUrlInfo camundaSystemUrlInfo, ReferencedTask referencedTask) {

    StringBuilder requestUrlBuilder = new StringBuilder();
    try {
      setAssigneeToOwnerOfReferencedTask(camundaSystemUrlInfo, referencedTask, requestUrlBuilder);
      setCompletionByTaskanaAdapterAsLocalVariable(
          camundaSystemUrlInfo, referencedTask, requestUrlBuilder);
      SystemResponse systemResponse =
          performCompletion(camundaSystemUrlInfo, referencedTask, requestUrlBuilder);
      return systemResponse;
    } catch (HttpStatusCodeException e) {
      if (isTaskAlreadyDeleted(camundaSystemUrlInfo, referencedTask, requestUrlBuilder)) {
        return new SystemResponse(HttpStatus.OK, null);
      } else {
        LOGGER.warn("Caught Exception when trying to complete camunda task", e);
        throw e;
      }
    }
  }

  private boolean isTaskAlreadyDeleted(
      CamundaSystemUrls.SystemUrlInfo camundaSystemUrlInfo,
      ReferencedTask camundaTask,
      StringBuilder requestUrlBuilder) {
    requestUrlBuilder.setLength(0);
    String requestUrl =
        requestUrlBuilder
            .append(camundaSystemUrlInfo.getSystemRestUrl())
            .append(CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
            .append(camundaTask.getId())
            .toString();

    HttpEntity<String> requestEntity = prepareEntityFromBody("{}");
    try {
      ResponseEntity<String> response =
          restTemplate.exchange(requestUrl, HttpMethod.GET, requestEntity, String.class);
    } catch (HttpStatusCodeException ex) {
      boolean isAlreadyDeleted = HttpStatus.NOT_FOUND.equals(ex.getStatusCode());
      if (isAlreadyDeleted) {
        LOGGER.debug("Task {} is already deleted. Returning silently", camundaTask.getId());
      }
      return isAlreadyDeleted;
    }
    return false;
  }

  private void setAssigneeToOwnerOfReferencedTask(
      CamundaSystemUrls.SystemUrlInfo camundaSystemUrlInfo,
      ReferencedTask referencedTask,
      StringBuilder requestUrlBuilder) {

    requestUrlBuilder
        .append(camundaSystemUrlInfo.getSystemRestUrl())
        .append(CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
        .append(referencedTask.getId())
        .append(CamundaSystemConnectorImpl.SET_ASSIGNEE);

    String requestBody =
        CamundaSystemConnectorImpl.BODY_SET_ASSIGNEE + "\"" + referencedTask.getAssignee() + "\"}";

    HttpEntity<String> requestEntity = prepareEntityFromBody(requestBody);
    ResponseEntity<String> responseEntity =
        this.restTemplate.exchange(
            requestUrlBuilder.toString(), HttpMethod.POST, requestEntity, String.class);
    LOGGER.debug(
        "Set assignee for camunda task {}. Status code = {}",
        referencedTask.getId(),
        responseEntity.getStatusCode());
  }

  private void setCompletionByTaskanaAdapterAsLocalVariable(
      CamundaSystemUrls.SystemUrlInfo camundaSystemUrlInfo,
      ReferencedTask referencedTask,
      StringBuilder requestUrlBuilder) {

    requestUrlBuilder.setLength(0);

    requestUrlBuilder
        .append(camundaSystemUrlInfo.getSystemRestUrl())
        .append(CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
        .append(referencedTask.getId())
        .append(CamundaSystemConnectorImpl.LOCAL_VARIABLE_PATH)
        .append("/")
        .append(COMPLETED_BY_TASKANA_ADAPTER_LOCAL_VARIABLE);

    HttpEntity<String> requestEntity =
        prepareEntityFromBody("{\"value\" : true, \"type\": \"Boolean\"}");

    ResponseEntity<String> responseEntity =
        this.restTemplate.exchange(
            requestUrlBuilder.toString(), HttpMethod.PUT, requestEntity, String.class);
    LOGGER.debug(
        "Set local Variable \"completedByTaskanaAdapter\" for camunda task {}. Status code = {}",
        referencedTask.getId(),
        responseEntity.getStatusCode());
  }

  private SystemResponse performCompletion(
      CamundaSystemUrls.SystemUrlInfo camundaSystemUrlInfo,
      ReferencedTask camundaTask,
      StringBuilder requestUrlBuilder) {

    requestUrlBuilder.setLength(0);
    requestUrlBuilder
        .append(camundaSystemUrlInfo.getSystemRestUrl())
        .append(CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS)
        .append(camundaTask.getId())
        .append(CamundaSystemConnectorImpl.COMPLETE_TASK);

    String requestBody = prepareRequestBody(camundaTask);

    LOGGER.debug(
        "completing camunda task {}  with request body {}", camundaTask.getId(), requestBody);

    HttpEntity<String> entity = prepareEntityFromBody(requestBody);

    try {
      ResponseEntity<String> responseEntity =
          restTemplate.postForEntity(requestUrlBuilder.toString(), entity, String.class);
      LOGGER.debug(
          "completed camunda task {}. Status code = {}",
          camundaTask.getId(),
          responseEntity.getStatusCode());

      return new SystemResponse(responseEntity.getStatusCode(), null);

    } catch (HttpStatusCodeException e) {
      LOGGER.info(
          "tried to complete camunda task {} and caught Status code {}",
          camundaTask.getId(),
          e.getStatusCode());
      throw new SystemException(
          "caught HttpStatusCodeException "
              + e.getStatusCode()
              + " on the attempt to complete Camunda Task "
              + camundaTask.getId(),
          e.getMostSpecificCause());
    }
  }

  private String prepareRequestBody(ReferencedTask camundaTask) {

    String requestBody;
    if (camundaTask.getVariables() == null) {
      requestBody = CamundaSystemConnectorImpl.EMPTY_REQUEST_BODY;
    } else {
      requestBody =
          CamundaSystemConnectorImpl.BODY_SET_CAMUNDA_VARIABLES + camundaTask.getVariables() + "}";
    }

    return requestBody;
  }

  private HttpEntity<String> prepareEntityFromBody(String requestBody) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(requestBody, headers);
  }
}
