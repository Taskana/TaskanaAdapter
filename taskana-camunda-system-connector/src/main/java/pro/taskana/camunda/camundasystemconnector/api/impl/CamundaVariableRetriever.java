package pro.taskana.camunda.camundasystemconnector.api.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class CamundaVariableRetriever {

    private static final Logger LOGGER = LoggerFactory.getLogger(CamundaTaskRetriever.class);

    @Autowired
    private RestTemplate restTemplate;

    public String retrieveTaskVariables(String taskId, String camundaSystemURL) {
        LOGGER.debug("entry to retrieveTaskVariables.  taskId = {}, CamundSystemURL = {} ",taskId, camundaSystemURL );
        String requestUrl = camundaSystemURL + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_TASKS 
                            + taskId + CamundaSystemConnectorImpl.URL_GET_CAMUNDA_VARIABLES;
        ResponseEntity<String> result = restTemplate.getForEntity(requestUrl, String.class);
        LOGGER.debug("exit from retrieveTaskVariables.  taskId = {}, variables = {} ",taskId, result.getBody() );
    
        return result.getBody();
    }    


}
