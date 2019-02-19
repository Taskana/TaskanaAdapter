package pro.taskana.adapter.systemconnector.camunda.api.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import pro.taskana.adapter.configuration.SpringContextProvider;
import pro.taskana.adapter.systemconnector.api.SystemResponse;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;

/**
 * Sample Implementation of SystemConnector.
 * @author bbr
 *
 */
public class CamundaSystemConnectorImpl implements SystemConnector {
    
    static final String URL_GET_CAMUNDA_TASKS = "/task/";
    static final String URL_GET_CAMUNDA_HISTORIC_TASKS = "/history/task/";
    static final String URL_GET_CAMUNDA_VARIABLES = "/variables/";
    static final String BODY_SET_CAMUNDA_VARIABLES = "{\"variables\":";
    static final String COMPLETE_TASK = "/complete/";
    static final String EMPTY_REQUEST_BODY = "{}";

    private String camundaSystemURL;
        
    private CamundaTaskRetriever taskRetriever;
        
    private CamundaTaskCompleter taskCompleter;
    
    private CamundaVariableRetriever variableRetriever;
       
    public CamundaSystemConnectorImpl(String camundaSystemURL) {
        this.camundaSystemURL = camundaSystemURL;
        taskRetriever = SpringContextProvider.getBean(CamundaTaskRetriever.class);
        variableRetriever = SpringContextProvider.getBean(CamundaVariableRetriever.class);
        taskCompleter = SpringContextProvider.getBean(CamundaTaskCompleter.class);
    }
    
    @Override
    public List<ReferencedTask> retrieveReferencedTasksStartedAfter(Instant createdAfter) {
        return taskRetriever.retrieveCamundaTasksStartedAfter(camundaSystemURL, createdAfter);
    }

    @Override
    public SystemResponse completeReferencedTask(ReferencedTask camundaTask) {
        return taskCompleter.completeCamundaTask(camundaSystemURL, camundaTask);
    }

    @Override
    public String getSystemURL() {
        return camundaSystemURL;
    }

    @Override
    public String retrieveVariables(String taskId) {
        String variables = variableRetriever.retrieveVariables(taskId, camundaSystemURL);
        return variables;
    }

    @Override
    public List<ReferencedTask> retrieveFinishedTasks(Instant finishedAfter) {
        return taskRetriever.retrieveFinishedCamundaTasks(camundaSystemURL, finishedAfter);
    }


}
