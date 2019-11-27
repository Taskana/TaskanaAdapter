package pro.taskana.adapter.systemconnector.camunda.api.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;

import pro.taskana.adapter.configuration.AdapterSpringContextProvider;
import pro.taskana.adapter.exceptions.ReferencedTaskDoesNotExistInExternalSystemException;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.systemconnector.api.SystemResponse;
import pro.taskana.adapter.systemconnector.camunda.config.CamundaSystemUrls;

/**
 * Sample Implementation of SystemConnector.
 *
 * @author bbr
 */
public class CamundaSystemConnectorImpl implements SystemConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaSystemConnectorImpl.class);

    static final String URL_GET_CAMUNDA_TASKS = "/task/";
    static final String URL_GET_CAMUNDA_HISTORIC_TASKS = "/history/task/";
    static final String URL_GET_CAMUNDA_VARIABLES = "/variables/";
    static final String URL_OUTBOX_REST_PATH = "/taskana-outbox/rest/";

    static final String URL_GET_CAMUNDA_CREATE_EVENTS = "events?type=create";
    static final String URL_GET_CAMUNDA_COMPLETE_EVENTS = "events?type=complete&type=delete";
    static final String URL_DELETE_CAMUNDA_EVENTS = "events/delete";

    static final String BODY_SET_CAMUNDA_VARIABLES = "{\"variables\":";
    static final String COMPLETE_TASK = "/complete/";
    static final String EMPTY_REQUEST_BODY = "{}";

    private CamundaSystemUrls.SystemURLInfo camundaSystemURL;

    private CamundaTaskRetriever taskRetriever;

    private CamundaTaskCompleter taskCompleter;

    private CamundaVariableRetriever variableRetriever;

    private CamundaTaskEventCleaner taskEventCleaner;

    public CamundaSystemConnectorImpl(CamundaSystemUrls.SystemURLInfo camundaSystemURL) {
        this.camundaSystemURL = camundaSystemURL;
        taskRetriever = AdapterSpringContextProvider.getBean(CamundaTaskRetriever.class);
        variableRetriever = AdapterSpringContextProvider.getBean(CamundaVariableRetriever.class);
        taskCompleter = AdapterSpringContextProvider.getBean(CamundaTaskCompleter.class);
        taskEventCleaner = AdapterSpringContextProvider.getBean(CamundaTaskEventCleaner.class);
    }

    @Override
    public List<ReferencedTask> retrieveNewStartedReferencedTasks() {
        return taskRetriever.retrieveNewStartedCamundaTasks(camundaSystemURL.getSystemTaskEventUrl());
    }

    @Override
    public SystemResponse completeReferencedTask(ReferencedTask camundaTask) {
        return taskCompleter.completeCamundaTask(camundaSystemURL, camundaTask);
    }

    @Override
    public String getSystemURL() {
        return camundaSystemURL.getSystemRestUrl();
    }

    @Override
    public String retrieveVariables(String taskId) {
        try {
            return variableRetriever.retrieveVariables(taskId, camundaSystemURL.getSystemRestUrl());
        } catch (HttpStatusCodeException e) {
            if (e.getStatusCode().value() == 500) {
                LOGGER.debug("Attempted to retrieve variables of non existing task {}.", taskId);
                throw new ReferencedTaskDoesNotExistInExternalSystemException(e.getMessage());
            } else {
                LOGGER.warn("While attempting to retrieve variables for task {} caught ", taskId, e);
                throw e;
            }
        }
    }

    @Override
    public List<ReferencedTask> retrieveTerminatedTasks() {
        return taskRetriever.retrieveTerminatedCamundaTasks(camundaSystemURL.getSystemTaskEventUrl());
    }

    @Override
    public void taskanaTasksHaveBeenCreatedForNewReferencedTasks(List<ReferencedTask> referencedTasks) {
        taskEventCleaner.cleanEventsForReferencedTasks(referencedTasks,
            camundaSystemURL.getSystemTaskEventUrl());
    }

    @Override
    public void taskanaTasksHaveBeenCompletedForTerminatedReferencedTasks(List<ReferencedTask> referencedTasks) {
        taskEventCleaner.cleanEventsForReferencedTasks(referencedTasks,
            camundaSystemURL.getSystemTaskEventUrl());
    }

}
