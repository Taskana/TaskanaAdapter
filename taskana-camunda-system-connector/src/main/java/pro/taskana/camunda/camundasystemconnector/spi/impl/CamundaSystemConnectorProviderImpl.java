package pro.taskana.camunda.camundasystemconnector.spi.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.camunda.camundasystemconnector.api.CamundaSystemConnector;
import pro.taskana.camunda.camundasystemconnector.api.impl.CamundaSystemConnectorImpl;
import pro.taskana.camunda.camundasystemconnector.config.CamundaSystemUrls;
import pro.taskana.camunda.camundasystemconnector.spi.CamundaSystemConnectorProvider;
import pro.taskana.camunda.configuration.SpringContextProvider;

public class CamundaSystemConnectorProviderImpl implements CamundaSystemConnectorProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(CamundaSystemConnectorProviderImpl.class);
       
    @Override
    public List<CamundaSystemConnector> create() {
    	CamundaSystemUrls camundaSystemURLs = SpringContextProvider.getBean(CamundaSystemUrls.class);
    	LOGGER.error("create() called, camundaSystemURLs = {}, exc={}", camundaSystemURLs, new Exception("debug"));
        List<CamundaSystemConnector> result = new ArrayList<>();
        for (String camundaSystemURL : camundaSystemURLs.getUrls()) {
            result.add( new CamundaSystemConnectorImpl(camundaSystemURL));            
        }
        
        return result;
    }

    public CamundaSystemConnectorProviderImpl() {
    	LOGGER.error("CamundaSystemConnectorProviderImpl.ctor called. exc = {}", new Exception("debug"));
    }
    
}
