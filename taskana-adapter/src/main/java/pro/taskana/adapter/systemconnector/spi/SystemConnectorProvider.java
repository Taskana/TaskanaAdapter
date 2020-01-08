package pro.taskana.adapter.systemconnector.spi;

import java.util.List;

import pro.taskana.adapter.systemconnector.api.SystemConnector;

/**
 * The interface for the SystemConnector provider.
 *
 * @author bbr
 */
public interface SystemConnectorProvider {

  /**
   * create a list of SystemConnector objects to access multiple external systems.
   *
   * @return a list of SystemConnector objects
   */
  List<SystemConnector> create();
}
