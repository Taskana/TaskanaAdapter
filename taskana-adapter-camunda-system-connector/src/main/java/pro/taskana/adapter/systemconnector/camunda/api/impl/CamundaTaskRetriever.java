package pro.taskana.adapter.systemconnector.camunda.api.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import pro.taskana.adapter.camunda.outbox.rest.resource.CamundaTaskEventResource;

@Component
public class CamundaTaskRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskRetriever.class);

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    ObjectMapper objectMapper;

    public List<ReferencedTask> retrieveActiveCamundaTasks(String camundaSystemTaskEventUrl)  {

        LOGGER.debug("### entry to retrieveActiveCamundaTasks. createdAfter = {} ###");

        CamundaTaskEventResource[] camundaTaskEventResources = getCamundaTaskEventResources(camundaSystemTaskEventUrl);

        List<ReferencedTask> referencedTasks = getReferencedTasksFromCamundaTaskEventResources(camundaTaskEventResources);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exit from retrieveActiveCamundaTasks. Retrieved Tasks: {}", referencedTasks);
        }
        return referencedTasks;
    }

    private CamundaTaskEventResource[] getCamundaTaskEventResources(String camundaSystemTaskEventUrl) {

        String requestUrl = camundaSystemTaskEventUrl + CamundaSystemConnectorImpl.URL_OUTBOX_REST_PATH
                + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_CREATE_EVENTS;

        LOGGER.debug("### retrieving active camunda task event resources with url {} ###", requestUrl);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        LOGGER.debug("retrieveNewStartedCamundaTasks url {} ", requestUrl);

        ResponseEntity<CamundaTaskEventResource[]> responseEntity = restTemplate.exchange(
                requestUrl, HttpMethod.GET, new HttpEntity<Object>(headers),
                CamundaTaskEventResource[].class);

        CamundaTaskEventResource[] camundaTaskEventResources = responseEntity.getBody();

        return camundaTaskEventResources;
    }
    private List<ReferencedTask> getReferencedTasksFromCamundaTaskEventResources(CamundaTaskEventResource[] camundaTaskEventResources) {

        List<ReferencedTask> referencedTasks = new ArrayList<>();

        for (CamundaTaskEventResource camundaTaskEventResource : camundaTaskEventResources){

            String referencedTaskJson = camundaTaskEventResource.getPayload();
            camundaTaskEventResource.getId();

            try {

                ReferencedTask referencedTask = objectMapper.readValue(referencedTaskJson, ReferencedTask.class);
                referencedTask.setCreationEventId(String.valueOf(camundaTaskEventResource.getId()));
                referencedTasks.add(referencedTask);

            }catch(IOException e){
                LOGGER.warn("Caught {} while trying to create ReferencedTasks out of CamundaTaskEventResources");
            }
        }
        return referencedTasks;
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
