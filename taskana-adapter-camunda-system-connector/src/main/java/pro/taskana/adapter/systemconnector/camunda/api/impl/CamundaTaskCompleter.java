package pro.taskana.adapter.systemconnector.camunda.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemResponse;
import pro.taskana.adapter.systemconnector.camunda.config.CamundaSystemUrls;
import pro.taskana.exceptions.SystemException;

public class CamundaTaskCompleter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskCompleter.class);

    @Autowired
    private RestTemplate restTemplate;

    public SystemResponse completeCamundaTask(CamundaSystemUrls.SystemURLInfo camundaSystemUrlInfo,
        ReferencedTask camundaTask) {
        String url = camundaSystemUrlInfo.getSystemRestUrl() + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS
            + "/" + camundaTask.getId() + CamundaSystemConnectorImpl.COMPLETE_TASK;
        String requestBody;
        if (camundaTask.getVariables() == null) {
            requestBody = CamundaSystemConnectorImpl.EMPTY_REQUEST_BODY;
        } else {
            requestBody = CamundaSystemConnectorImpl.BODY_SET_CAMUNDA_VARIABLES + camundaTask.getVariables() + "}";
        }
        LOGGER.debug("completing camunda task {}  with request body {}", camundaTask.getId(), requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            LOGGER.debug("completed camunda task {}. Status code = {}", camundaTask.getId(),
                responseEntity.getStatusCode());

            return new SystemResponse(responseEntity.getStatusCode(), null);

        } catch (HttpStatusCodeException e) {

            LOGGER.info("tried to complete camunda task {} and caught Status code {}", camundaTask.getId(),
                e.getStatusCode());
            throw new SystemException("caught HttpStatusCodeException " + e.getStatusCode()
                + " on the attempt to complete Camunda Task " + camundaTask.getId(), e.getMostSpecificCause());
        }

    }

}
