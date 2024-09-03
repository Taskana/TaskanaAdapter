package io.kadai.adapter.systemconnector.spi;

import io.kadai.adapter.systemconnector.api.SystemConnector;
import java.util.List;

/** The interface for the SystemConnector provider. */
public interface SystemConnectorProvider {

  /**
   * create a list of SystemConnector objects to access multiple external systems.
   *
   * @return a list of SystemConnector objects
   */
  List<SystemConnector> create();
}
