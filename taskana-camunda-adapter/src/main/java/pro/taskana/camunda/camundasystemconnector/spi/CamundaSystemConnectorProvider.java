package pro.taskana.camunda.camundasystemconnector.spi;

import java.util.List;

import pro.taskana.camunda.camundasystemconnector.api.CamundaSystemConnector;

/**
 * The interface for the Camunda SystemConnector provider.
 * @author bbr
 *
 */
public interface CamundaSystemConnectorProvider {

    /**
     * create a list of CamundaSystemConnector objects to access multiple camunda systems.
     *
     * @return a list of ISystemConnector objects
     */
    List<CamundaSystemConnector> create();
}
