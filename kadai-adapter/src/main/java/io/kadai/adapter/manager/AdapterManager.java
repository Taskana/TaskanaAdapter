package io.kadai.adapter.manager;

import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import io.kadai.adapter.kadaiconnector.spi.KadaiConnectorProvider;
import io.kadai.adapter.systemconnector.api.SystemConnector;
import io.kadai.adapter.systemconnector.spi.SystemConnectorProvider;
import io.kadai.adapter.util.Assert;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Scheduler for receiving referenced tasks, completing Kadai tasks and cleaning adapter tables. */
@Component
public class AdapterManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdapterManager.class);
  private boolean isInitialized = false;

  private Map<String, SystemConnector> systemConnectors;
  private List<KadaiConnector> kadaiConnectors;

  public Map<String, SystemConnector> getSystemConnectors() {
    return systemConnectors;
  }

  public KadaiConnector getKadaiConnector() {
    Assert.assertion(kadaiConnectors.size() == 1, "kadaiConnectors.size() == 1");
    return kadaiConnectors.get(0);
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  public void init() {
    if (isInitialized) {
      return;
    }
    LOGGER.debug("initAdapterInfrastructure called ");
    initKadaiConnectors();
    initSystemConnectors();
    isInitialized = true;
  }

  private void initSystemConnectors() {
    systemConnectors = new HashMap<>();
    LOGGER.info("initializing system connectors ");

    ServiceLoader<SystemConnectorProvider> loader =
        ServiceLoader.load(SystemConnectorProvider.class);
    for (SystemConnectorProvider provider : loader) {
      List<SystemConnector> connectors = provider.create();
      for (SystemConnector conn : connectors) {
        systemConnectors.put(conn.getSystemUrl(), conn);
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(
              "initialized system connectors {} for system_url {}", conn, conn.getSystemUrl());
        }
      }
    }
  }

  private void initKadaiConnectors() {
    kadaiConnectors = new ArrayList<>();
    ServiceLoader<KadaiConnectorProvider> loader = ServiceLoader.load(KadaiConnectorProvider.class);
    for (KadaiConnectorProvider provider : loader) {
      List<KadaiConnector> connectors = provider.create();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("initialized kadai connectors {} ", connectors);
      }
      kadaiConnectors.addAll(connectors);
    }
  }
}
