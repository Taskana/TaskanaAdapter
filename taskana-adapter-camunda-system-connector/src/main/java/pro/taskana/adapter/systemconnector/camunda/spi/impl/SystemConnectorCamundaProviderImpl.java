package pro.taskana.adapter.systemconnector.camunda.spi.impl;

import java.util.ArrayList;
import java.util.List;

import pro.taskana.adapter.configuration.AdapterSpringContextProvider;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaSystemConnectorImpl;
import pro.taskana.adapter.systemconnector.camunda.config.CamundaSystemUrls;
import pro.taskana.adapter.systemconnector.spi.SystemConnectorProvider;

/**
 * Implements SystemConnectorProvider for camunda.
 *
 * @author bbr
 */
public class SystemConnectorCamundaProviderImpl implements SystemConnectorProvider {

    @Override
    public List<SystemConnector> create() {
        // note: this class is created by ServiceLoader, not by Spring. Therefore it is no bean and we must
        // retrieve the Spring-generated Bean for camundaSystemURLs programatically. Only this bean has the properties
        // resolved.
        // In order for this bean to be retrievable, the SpringContextProvider must already be initialized.
        // This is assured via the
        // @DependsOn(value= {"adapterSpringContextProvider"}) annotation of CamundaSystemConnectorConfiguration

        CamundaSystemUrls camundaSystemURLs = AdapterSpringContextProvider.getBean(CamundaSystemUrls.class);

        List<SystemConnector> result = new ArrayList<>();
        for (CamundaSystemUrls.SystemURLInfo camundaSystemURL : camundaSystemURLs.getUrls()) {
            result.add(new CamundaSystemConnectorImpl(camundaSystemURL));
        }

        return result;
    }
}
