package pro.taskana.camunda.camundasystemconnector.api.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import pro.taskana.camunda.camundasystemconnector.api.CamundaResponse;

public class CamundaTaskCompleter {
    
    private RestTemplate restTemplate;
    
    public CamundaTaskCompleter(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public CamundaResponse completeCamundaTask(String camundaHost, String camundaTaskId) {
        String url = camundaHost + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS + camundaTaskId + CamundaSystemConnectorImpl.COMPLETE_TASK;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(CamundaSystemConnectorImpl.EMPTY_REQUEST_BODY, headers);
        try {
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, entity, String.class);
            return new CamundaResponse(responseEntity.getStatusCode(), null);
        } catch(HttpStatusCodeException e) {
            return new CamundaResponse(e.getStatusCode(),e.getMostSpecificCause());
        }
        
    }          

}
