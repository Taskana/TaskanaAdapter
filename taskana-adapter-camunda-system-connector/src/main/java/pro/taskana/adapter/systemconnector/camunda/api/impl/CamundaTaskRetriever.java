package pro.taskana.adapter.systemconnector.camunda.api.impl;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
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

import pro.taskana.adapter.scheduler.Scheduler;
import pro.taskana.adapter.systemconnector.api.GeneralTask;

@Component
public class CamundaTaskRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskRetriever.class);

    @Autowired
    private RestTemplate restTemplate;

    public List<GeneralTask> retrieveCamundaTasksStartedAfter(String camundaSystemURL, Instant createdAfter) {
        LOGGER.debug("entry to retrieveActiveCamundaTasks. CamundSystemURL = {}, createdAfter = {} ",camundaSystemURL, createdAfter );
        String requestUrl = camundaSystemURL + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS ;
        String requestBody;
        if (createdAfter == null) {
            requestBody = CamundaSystemConnectorImpl.EMPTY_REQUEST_BODY;
        } else {
            // Instant is in UTC time, Camunda uses local time. Need to adjust ...
            Date date = java.sql.Timestamp.valueOf(createdAfter.atZone(ZoneId.systemDefault()).toLocalDateTime());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            requestBody = "{\"createdAfter\": \"" + formatter.format(date) + "\"}";
            LOGGER.info("retrieving active camunda tasks with request body {}", requestBody);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        LOGGER.debug("retrieveActiveCamundaTasks posts {}, body = {}", requestUrl, requestBody );
        GeneralTask[] tasks = restTemplate.postForEntity(requestUrl, entity, GeneralTask[].class).getBody();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exit from retrieveActiveCamundaTasks. Retrieved Tasks: {}",Arrays.toString(tasks) );
        }
        return Arrays.asList(tasks);
    }
    
    public List<GeneralTask> retrieveFinishedCamundaTasks(String camundaSystemURL, Instant finishedAfter) {
        LOGGER.debug("entry to retrieveFinishedCamundaTasks. CamundSystemURL = {}, finishedAfter = {} ",camundaSystemURL, finishedAfter );
        String requestUrl = camundaSystemURL + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_HISTORIC_TASKS ;
        String requestBody;
        if (finishedAfter == null) {
            requestBody ="{ \"finished\" : \"true\"}";
        } else {
            // Instant is in UTC time, Camunda uses local time. Need to adjust ...
            Date date = java.sql.Timestamp.valueOf(finishedAfter.atZone(ZoneId.systemDefault()).toLocalDateTime());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            requestBody = "{ \"finished\" : \"true\", \"finishedAfter\": \"" + formatter.format(date) + "\"}";
            LOGGER.info("retrieving finished camunda tasks with request body {}", requestBody);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        LOGGER.debug("retrieveFinishedCamundaTasks postw {}, body = {}", requestUrl, requestBody );

        GeneralTask[] tasks = restTemplate.postForEntity(requestUrl, entity, GeneralTask[].class).getBody();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exit from retrieveFinishedCamundaTasks. Retrieved Tasks: {}",Arrays.toString(tasks) );
        }
        return Arrays.asList(tasks);
    }
}
