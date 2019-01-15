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
        // note: this class is created by ServiceLoader, not by Spring. Therefore it is no bean and we must
        // retrieve the Spring-generated Bean for camundaSystemURLs programatically. Only this bean has the properties resolved.
        // In order for this bean to be retrievable, the SpringContextProvider must already be initialized.
        // This is assured via the
        // @DependsOn(value= {"springContextProvider"}) annotation of CamundaSystemConnectorConfiguration

        CamundaSystemUrls camundaSystemURLs = SpringContextProvider.getBean(CamundaSystemUrls.class);

        List<CamundaSystemConnector> result = new ArrayList<>();
        for (String camundaSystemURL : camundaSystemURLs.getUrls()) {
            result.add(new CamundaSystemConnectorImpl(camundaSystemURL));
        }

        return result;
    }
}
