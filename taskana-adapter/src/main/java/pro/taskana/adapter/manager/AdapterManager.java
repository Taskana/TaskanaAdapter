package pro.taskana.adapter.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.systemconnector.spi.SystemConnectorProvider;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.adapter.taskanaconnector.spi.TaskanaConnectorProvider;
import pro.taskana.adapter.util.Assert;

/**
 * Scheduler for receiving referenced tasks, completing Taskana tasks and cleaning adapter tables.
 */
@Component
public class AdapterManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdapterManager.class);
  private boolean isInitialized = false;

  private Map<String, SystemConnector> systemConnectors;
  private List<TaskanaConnector> taskanaConnectors;

  public Map<String, SystemConnector> getSystemConnectors() {
    return systemConnectors;
  }

  public TaskanaConnector getTaskanaConnector() {
    Assert.assertion(taskanaConnectors.size() == 1, "taskanaConnectors.size() == 1");
    return taskanaConnectors.get(0);
  }

  public boolean isInitialized() {
    return isInitialized;
  }

  public void init() {
    if (isInitialized) {
      return;
    }
    LOGGER.debug("initAdapterInfrastructure called ");
    initTaskanaConnectors();
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

  private void initTaskanaConnectors() {
    taskanaConnectors = new ArrayList<>();
    ServiceLoader<TaskanaConnectorProvider> loader =
        ServiceLoader.load(TaskanaConnectorProvider.class);
    for (TaskanaConnectorProvider provider : loader) {
      List<TaskanaConnector> connectors = provider.create();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info("initialized taskana connectors {} ", connectors);
      }
      taskanaConnectors.addAll(connectors);
    }
  }
}
