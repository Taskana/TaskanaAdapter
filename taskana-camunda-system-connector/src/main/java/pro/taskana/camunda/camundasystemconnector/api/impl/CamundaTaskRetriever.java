package pro.taskana.camunda.camundasystemconnector.api.impl;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import pro.taskana.camunda.camundasystemconnector.api.CamundaTask;
import pro.taskana.camunda.scheduler.Scheduler;

@Component
public class CamundaTaskRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskRetriever.class);

    private RestTemplate restTemplate;

    public CamundaTaskRetriever(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public List<CamundaTask> retrieveCamundaTasks(String camundaSystemURL, Instant createdAfter) {
        String requestUrl = camundaSystemURL + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS ;
        String requestBody;
        if (createdAfter == null) {
            requestBody = CamundaSystemConnectorImpl.EMPTY_REQUEST_BODY;
        } else {
            Date date = Date.from(createdAfter);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            requestBody = "{\"createdAfter\": \"" + formatter.format(date) + "\"}";
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        CamundaTask[] tasks = restTemplate.postForEntity(requestUrl, entity, CamundaTask[].class).getBody();
        return Arrays.asList(tasks);
    }    
}
