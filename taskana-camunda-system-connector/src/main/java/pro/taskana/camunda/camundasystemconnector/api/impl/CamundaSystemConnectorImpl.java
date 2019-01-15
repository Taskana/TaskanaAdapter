package pro.taskana.camunda.camundasystemconnector.api.impl;

import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import pro.taskana.camunda.camundasystemconnector.api.CamundaResponse;
import pro.taskana.camunda.camundasystemconnector.api.CamundaSystemConnector;
import pro.taskana.camunda.camundasystemconnector.api.CamundaTask;
import pro.taskana.camunda.configuration.SpringContextProvider;

/**
 * Sample Implementation of CamundaSystemConnector.
 * @author bbr
 *
 */
public class CamundaSystemConnectorImpl implements CamundaSystemConnector {
    
    static final String URL_GET_CAMUNDA_TASKS = "/task/";
    static final String URL_GET_CAMUNDA_VARIABLES = "/form-variables/";
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
    public List<CamundaTask> retrieveCamundaTasks(Instant createdAfter) {
        return taskRetriever.retrieveCamundaTasks(camundaSystemURL, createdAfter);
    }

    @Override
    public CamundaResponse completeCamundaTask(CamundaTask camundaTask) {
        return taskCompleter.completeCamundaTask(camundaSystemURL, camundaTask);
    }

    @Override
    public String getCamundaSystemURL() {
        return camundaSystemURL;
    }

    @Override
    public String retrieveTaskVariables(String taskId) {
        String variables = variableRetriever.retrieveTaskVariables(taskId, camundaSystemURL);
        return variables;
    }


}
