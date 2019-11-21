package pro.taskana.adapter.systemconnector.camunda.api.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import pro.taskana.adapter.impl.TaskanaTaskStarter;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;

@Component
public class CamundaTaskEventCleaner {

    @Autowired
    private RestTemplate restTemplate;

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskStarter.class);

    public void cleanEventsForReferencedTasks(List<ReferencedTask> referencedTasks,
        String camundaSystemTaskEventUrl) {

        LOGGER.debug("entry to taskanaTasksHaveBeenCreatedForReferencedTasks, CamundSystemURL = {}",
            camundaSystemTaskEventUrl);
        String requestUrl = camundaSystemTaskEventUrl + CamundaSystemConnectorImpl.URL_OUTBOX_REST_PATH
            + CamundaSystemConnectorImpl.URL_DELETE_CAMUNDA_EVENTS;
        LOGGER.debug("cleaning up camunda task events with url {}", requestUrl);

        if (referencedTasks == null || referencedTasks.isEmpty()) {
            return;
        }

        StringBuilder idsBuf = new StringBuilder();
        for (ReferencedTask referencedTask : referencedTasks) {
            idsBuf.append(referencedTask.getCreationEventId().trim());
            idsBuf.append(',');
        }

        String ids = idsBuf.toString().replaceAll(",$", "");
        requestUrl = requestUrl + ids;

        LOGGER.debug("delete Events url {} ", requestUrl);
        restTemplate.delete(requestUrl);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("exit from taskanaTasksHaveBeenCreatedForReferencedTasks.");
        }
    }
}
