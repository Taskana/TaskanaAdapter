package pro.taskana.adapter.systemconnector.camunda.api.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.adapter.configuration.AdapterSpringContextProvider;
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
    static final String URL_OUTBOX_REST_PATH = "/taskana-outbox/rest";

    static final String URL_GET_CAMUNDA_CREATE_EVENTS = "/events?type=create";
    static final String URL_GET_CAMUNDA_COMPLETE_EVENTS = "/events?type=complete&type=delete";
    static final String URL_DELETE_CAMUNDA_EVENTS = "/events/delete";

    static final String BODY_SET_CAMUNDA_VARIABLES = "{\"variables\":";
    static final String LOCAL_VARIABLE_PATH = "/localVariables";
    static final String EMPTY_REQUEST_BODY = "{}";

    static final String COMPLETE_TASK = "/complete/";
    static final String SET_ASSIGNEE = "/assignee/";
    static final String BODY_SET_ASSIGNEE = "{\"userId\":";
    static final String UNCLAIM_TASK = "/unclaim/";


    private CamundaSystemUrls.SystemURLInfo camundaSystemURL;

    private CamundaTaskRetriever taskRetriever;

    private CamundaTaskCompleter taskCompleter;

    private CamundaTaskClaimer taskClaimer;

    private CamundaTaskClaimCanceler taskClaimCanceler;

    private CamundaTaskEventCleaner taskEventCleaner;

    public CamundaSystemConnectorImpl(CamundaSystemUrls.SystemURLInfo camundaSystemURL) {
        this.camundaSystemURL = camundaSystemURL;
        taskRetriever = AdapterSpringContextProvider.getBean(CamundaTaskRetriever.class);
        taskCompleter = AdapterSpringContextProvider.getBean(CamundaTaskCompleter.class);
        taskClaimer = AdapterSpringContextProvider.getBean(CamundaTaskClaimer.class);
        taskClaimCanceler = AdapterSpringContextProvider.getBean(CamundaTaskClaimCanceler.class);
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
    public SystemResponse claimReferencedTask(ReferencedTask camundaTask) {
        return taskClaimer.claimCamundaTask(camundaSystemURL, camundaTask);
    }

    @Override
    public SystemResponse cancelClaimReferencedTask(ReferencedTask camundaTask) {
        return taskClaimCanceler.cancelClaimOfCamundaTask(camundaSystemURL, camundaTask);
    }

    @Override
    public String getSystemURL() {
        return camundaSystemURL.getSystemRestUrl();
    }


    @Override
    public String retrieveVariables(String taskId) {
        return null;
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
