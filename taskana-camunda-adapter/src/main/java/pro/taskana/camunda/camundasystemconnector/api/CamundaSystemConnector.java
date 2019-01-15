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
     * Get the input variables of a camund task.
     *
     * @param taskId    the Id of the camunda task.
     * @return the input variables of the camunda task.
     */
    String retrieveTaskVariables(String taskId);

    /**
     * Instruct Camunda to complete a human task.
     *
     * @param task  the task to be completed.
     *
     * @return the response from camunda.
     */
    CamundaResponse completeCamundaTask(CamundaTask task);

    /**
     * Get the URL of the camunda system this connector connects to.
     *
     * @return the URL of the connected camunda system.
     */
    String getCamundaSystemURL();

}
