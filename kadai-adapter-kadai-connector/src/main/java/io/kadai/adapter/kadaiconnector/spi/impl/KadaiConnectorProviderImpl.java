package io.kadai.adapter.kadaiconnector.spi.impl;

import io.kadai.adapter.configuration.AdapterSpringContextProvider;
import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.kadaiconnector.spi.KadaiConnectorProvider;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The implementation of KadaiConnectorProvider. */
public class KadaiConnectorProviderImpl implements KadaiConnectorProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(KadaiConnectorProviderImpl.class);

  @Override
  public List<KadaiConnector> create() {
    // note: this class is created by ServiceLoader, not by Spring. Therefore it is no bean and we
    // must
    // retrieve the Spring-generated Bean for kadaiSystemConnector programmatically.
    // Only this bean has the correct injected properties.
    // In order for this bean to be retrievable, the SpringContextProvider must already be
    // initialized.
    // This is assured via the
    // @DependsOn(value= {"adapterSpringContextProvider"}) annotation of
    // KadaiSystemConnectorConfiguration

    List<KadaiConnector> result = new ArrayList<>();
    KadaiConnector kadaiSystemConnector =
        AdapterSpringContextProvider.getBean(KadaiConnector.class);
    LOGGER.info("retrieved kadaiSystemConnector {} ", kadaiSystemConnector);
    result.add(kadaiSystemConnector);
    return result;
  }
}
