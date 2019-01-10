package pro.taskana.camunda.camundasystemconnector.api.impl;

import java.time.Instant;
import java.util.List;

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
    static final String COMPLETE_TASK = "/complete/";
    static final String EMPTY_REQUEST_BODY = "{}";

    private String camundaSystemURL;
    
    private CamundaTaskRetriever taskRetriever;
       
    public CamundaSystemConnectorImpl(String camundaSystemURL) {
        this.camundaSystemURL = camundaSystemURL;
        taskRetriever = SpringContextProvider.getBean(CamundaTaskRetriever.class);
    }
    
    @Override
    public List<CamundaTask> retrieveCamundaTasks(Instant createdAfter) {
        return taskRetriever.retrieveCamundaTasks(camundaSystemURL, createdAfter);
    }

    @Override
    public CamundaResponse completeCamundaTask(String taskId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCamundaSystemURL() {
        return camundaSystemURL;
    }


}
