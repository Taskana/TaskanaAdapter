package io.kadai.adapter.systemconnector.camunda.spi.impl;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.adapter.systemconnector.camunda.api.impl.CamundaSystemConnectorImpl;
import io.kadai.adapter.systemconnector.camunda.config.CamundaSystemUrls;
import io.kadai.adapter.systemconnector.spi.SystemConnectorProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements SystemConnectorProvider for camunda.
 *
 * @author bbr
 */
public class SystemConnectorCamundaProviderImpl implements SystemConnectorProvider {

  @Override
  public List<SystemConnector> create() {
    // note: this class is created by ServiceLoader, not by Spring. Therefore it is no bean and we
    // must
    // retrieve the Spring-generated Bean for camundaSystemUrls programatically. Only this bean has
    // the properties
    // resolved.
    // In order for this bean to be retrievable, the SpringContextProvider must already be
    // initialized.
    // This is assured via the
    // @DependsOn(value= {"adapterSpringContextProvider"}) annotation of
    // CamundaSystemConnectorConfiguration

    CamundaSystemUrls camundaSystemUrls =
        AdapterSpringContextProvider.getBean(CamundaSystemUrls.class);

    List<SystemConnector> result = new ArrayList<>();
    for (CamundaSystemUrls.SystemUrlInfo camundaSystemUrl : camundaSystemUrls.getUrls()) {
      result.add(new CamundaSystemConnectorImpl(camundaSystemUrl));
    }

    return result;
  }
}
