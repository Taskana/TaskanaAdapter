package pro.taskana.camunda.camundasystemconnector.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import pro.taskana.camunda.camundasystemconnector.api.CamundaResponse;
import pro.taskana.camunda.camundasystemconnector.api.CamundaTask;
import pro.taskana.exceptions.SystemException;

public class CamundaTaskCompleter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskCompleter.class);
    
    @Autowired
    private RestTemplate restTemplate;
    
    public CamundaResponse completeCamundaTask(String camundaHost, CamundaTask camundaTask) {
        String url = camundaHost + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS + camundaTask.getId() + CamundaSystemConnectorImpl.COMPLETE_TASK;
        String requestBody;
        if (camundaTask.getOutputVariables() == null) {
            requestBody = CamundaSystemConnectorImpl.EMPTY_REQUEST_BODY;
        } else {
            requestBody = CamundaSystemConnectorImpl.BODY_SET_CAMUNDA_VARIABLES + camundaTask.getOutputVariables() + "}";
        }
        LOGGER.info("completing camunda task {}  with request body {}",camundaTask.getId(), requestBody);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            return new CamundaResponse(responseEntity.getStatusCode(), null);
        } catch(HttpStatusCodeException e) {
            throw new SystemException("caught HttpStatusCodeException " + e.getStatusCode() + " on the attempt to complete Camunda Task " + camundaTask.getId(), e.getMostSpecificCause() );
        } 
        
    }          

}
