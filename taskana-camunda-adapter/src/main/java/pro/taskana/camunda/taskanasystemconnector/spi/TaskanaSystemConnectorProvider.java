package pro.taskana.camunda.taskanasystemconnector.spi;

import java.util.List;

import pro.taskana.camunda.taskanasystemconnector.api.TaskanaSystemConnector;

/**
 * The interface, a Provider for TaskanaSystemConnectors must implement.
 * @author bbr
 *
 */
public interface TaskanaSystemConnectorProvider {
    List<TaskanaSystemConnector> create();
}
