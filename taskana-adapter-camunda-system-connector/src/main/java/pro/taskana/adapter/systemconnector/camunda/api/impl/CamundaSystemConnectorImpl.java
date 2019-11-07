package pro.taskana.adapter.systemconnector.camunda.api.impl;

import java.time.Instant;
import java.util.List;

import pro.taskana.adapter.configuration.AdapterSpringContextProvider;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.systemconnector.api.SystemResponse;
import pro.taskana.adapter.systemconnector.camunda.config.CamundaSystemUrls;

/**
 * Sample Implementation of SystemConnector.
 * @author bbr
 *
 */
public class CamundaSystemConnectorImpl implements SystemConnector {
    
    static final String URL_GET_CAMUNDA_TASKS = "/task/";
    static final String URL_GET_CAMUNDA_HISTORIC_TASKS = "/history/task/";
    static final String URL_GET_CAMUNDA_VARIABLES = "/variables/";
    static final String URL_OUTBOX_REST_PATH = "/rest/outbox";

    static final String URL_GET_CAMUNDA_START_EVENTS = "/getCreateEvents?startedAfter=";
    static final String URL_DELETE_CAMUNDA_EVENTS = "/delete?ids=";

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
    public List<ReferencedTask> retrieveReferencedTasksStartedAfter(Instant createdAfter) {
        return taskRetriever.retrieveCamundaTasksStartedAfter(camundaSystemURL.getSystemTaskEventUrl(), createdAfter);
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
        String variables = variableRetriever.retrieveVariables(taskId, camundaSystemURL.getSystemRestUrl());
        return variables;
    }

    @Override
    public List<ReferencedTask> retrieveFinishedTasks(Instant finishedAfter) {
        return taskRetriever.retrieveFinishedCamundaTasks(camundaSystemURL.getSystemTaskEventUrl(), finishedAfter);
    }

    @Override
    public void taskanaTasksHaveBeenCreatedFor(List<ReferencedTask> referencedTasks) {
        taskEventCleaner.taskanaTasksHaveBeenCreatedFor(referencedTasks, camundaSystemURL.getSystemTaskEventUrl());
    }

}
