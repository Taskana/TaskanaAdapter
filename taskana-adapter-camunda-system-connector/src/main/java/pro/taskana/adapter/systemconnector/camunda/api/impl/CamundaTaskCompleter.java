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

import pro.taskana.adapter.systemconnector.api.SystemResponse;
import pro.taskana.adapter.systemconnector.api.GeneralTask;
import pro.taskana.exceptions.SystemException;

public class CamundaTaskCompleter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskCompleter.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    public SystemResponse completeCamundaTask(String camundaHost, GeneralTask camundaTask) {
        String url = camundaHost + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS + camundaTask.getId() + CamundaSystemConnectorImpl.COMPLETE_TASK;
        String requestBody;
        if (camundaTask.getVariables() == null) {
            requestBody = CamundaSystemConnectorImpl.EMPTY_REQUEST_BODY;
        } else {
            requestBody = CamundaSystemConnectorImpl.BODY_SET_CAMUNDA_VARIABLES + camundaTask.getVariables() + "}";
        }
        LOGGER.info("completing camunda task {}  with request body {}",camundaTask.getId(), requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            return new SystemResponse(responseEntity.getStatusCode(), null);
        } catch(HttpStatusCodeException e) {
            throw new SystemException("caught HttpStatusCodeException " + e.getStatusCode() + " on the attempt to complete Camunda Task " + camundaTask.getId(), e.getMostSpecificCause() );
        } 
        
    }          

}
