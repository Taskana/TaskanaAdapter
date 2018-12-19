package pro.taskana.camunda.camundasystemconnector.api;

import java.time.Instant;
import java.util.List;

/**
 * This is the interface, a System Connector has to implement.
 * @author bbr
 *
 */
public interface CamundaSystemConnector {
    /**
     * Retrieve Camunda tasks that were started after a specified instant.
     *
     * @param createdAfter  the instant after which the tasks were created.
     *
     * @return a list of camunda tasks that were created after the createdAfter instant.
     */
    List<CamundaTask> retrieveCamundaTasks(Instant createdAfter);

    /**
     * Instruct Camunda to complete a human task.
     *
     * @param taskId  the Id of the task that is to be completed.
     *
     * @return the response from camunda.
     */
    CamundaResponse completeCamundaTask(String taskId);

    /**
     * Get the URL of the camunda system this connector connects to.
     *
     * @return the URL of the connected camunda system.
     */
    String getCamundaSystemURL();
}
