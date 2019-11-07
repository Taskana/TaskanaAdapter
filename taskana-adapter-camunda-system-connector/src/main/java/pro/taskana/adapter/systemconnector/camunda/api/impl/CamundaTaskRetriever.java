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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import pro.taskana.adapter.systemconnector.api.ReferencedTask;

@Component
public class CamundaTaskRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskRetriever.class);

    @Autowired
    private RestTemplate restTemplate;

    public List<ReferencedTask> retrieveCamundaTasksStartedAfter(String camundaSystemTaskEventUrl,
        Instant createdAfter) {

        LOGGER.debug("entry to retrieveActiveCamundaTasks. createdAfter = {} ",
            createdAfter);

        String requestUrl = camundaSystemTaskEventUrl + CamundaSystemConnectorImpl.URL_OUTBOX_REST_PATH
            + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_CREATE_EVENTS_STARTED_AFTER;
        // Instant is in UTC time, Camunda uses local time. Need to adjust ...
        Date date = java.sql.Timestamp.valueOf(createdAfter.atZone(ZoneId.systemDefault()).toLocalDateTime());
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        requestUrl += formatter.format(date);
        LOGGER.debug("retrieving active camunda tasks with url {}", requestUrl);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        LOGGER.debug("### retrieveActiveCamundaTasks url {} ", requestUrl);

        ResponseEntity<ReferencedTask[]> responseEntity = restTemplate.exchange(
            requestUrl, HttpMethod.GET, new HttpEntity<Object>(headers),
            ReferencedTask[].class);

        ReferencedTask[] tasks = responseEntity.getBody();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exit from retrieveCamundaTasksStartedAfter. Retrieved Tasks: {}", Arrays.toString(tasks));
        }
        return Arrays.asList(tasks);
    }

    public List<ReferencedTask> retrieveFinishedCamundaTasks(String camundaSystemURL, Instant finishedAfter) {
        LOGGER.debug("entry to retrieveFinishedCamundaTasks. CamundSystemURL = {}, finishedAfter = {} ",
            camundaSystemURL, finishedAfter);
        String requestUrl = camundaSystemURL + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_HISTORIC_TASKS;
        String requestBody;
        if (finishedAfter == null) {
            requestBody = "{ \"finished\" : \"true\"}";
        } else {
            // Instant is in UTC time, Camunda uses local time. Need to adjust ...
            Date date = java.sql.Timestamp.valueOf(finishedAfter.atZone(ZoneId.systemDefault()).toLocalDateTime());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
            requestBody = "{ \"finished\" : \"true\", \"finishedAfter\": \"" + formatter.format(date) + "\"}";
            LOGGER.debug("retrieving finished camunda tasks with request body {}", requestBody);
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

        LOGGER.debug("retrieveFinishedCamundaTasks postw {}, body = {}", requestUrl, requestBody);

        ReferencedTask[] tasks = restTemplate.postForEntity(requestUrl, entity, ReferencedTask[].class).getBody();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exit from retrieveFinishedCamundaTasks. Retrieved Tasks: {}", Arrays.toString(tasks));
        }
        return Arrays.asList(tasks);
    }
}
