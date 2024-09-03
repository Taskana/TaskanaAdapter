package io.kadai.adapter.kadaiconnector.spi;

import io.kadai.adapter.kadaiconnector.api.KadaiConnector;
import java.util.List;

/** The interface, a Provider for KadaiSystemConnectors must implement. */
public interface KadaiConnectorProvider {
  List<KadaiConnector> create();
}
