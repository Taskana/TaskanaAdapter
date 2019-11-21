package pro.taskana.adapter.systemconnector.camunda.api.impl;

import java.util.ArrayList;
import java.util.Arrays;
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

    public List<ReferencedTask> retrieveNewStartedCamundaTasks(String camundaSystemTaskEventUrl) {

        LOGGER.debug("entry to retrieveNewStartedCamundaTasks");

        String requestUrl = camundaSystemTaskEventUrl + CamundaSystemConnectorImpl.URL_OUTBOX_REST_PATH
            + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_CREATE_EVENTS;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        LOGGER.debug("retrieveNewStartedCamundaTasks url {} ", requestUrl);

        ResponseEntity<ReferencedTask[]> responseEntity = restTemplate.exchange(
            requestUrl, HttpMethod.GET, new HttpEntity<Object>(headers),
            ReferencedTask[].class);

        ReferencedTask[] tasks = responseEntity.getBody();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exit from retrieveNewStartedCamundaTasks. Retrieved Tasks: {}", Arrays.toString(tasks));
        }

        return tasks == null ? new ArrayList<>() : Arrays.asList(tasks);
    }

    public List<ReferencedTask> retrieveTerminatedCamundaTasks(String camundaSystemURL) {
        LOGGER.debug("entry to retrieveFinishedCamundaTasks. CamundSystemURL = {}", camundaSystemURL);
        // new implementation via outbox required
        ReferencedTask[] tasks = new ReferencedTask[] {};
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exit from retrieveFinishedCamundaTasks. Retrieved Tasks: {}", Arrays.toString(tasks));
        }
        return Arrays.asList(tasks);
    }
}
