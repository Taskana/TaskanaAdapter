package pro.taskana.adapter.systemconnector.api;

import java.time.Instant;
import java.util.List;

/**
 * This is the interface, a System Connector has to implement.
 * @author bbr
 *
 */
public interface SystemConnector {
    /**
     * Retrieve General tasks that were started after a specified instant.
     *
     * @param createdAfter  the instant after which the tasks were created.
     *
     * @return a list of general tasks that were created after the createdAfter instant.
     */
    List<GeneralTask> retrieveGeneralTasks(Instant createdAfter);

    /**
     * Get the variables of a general task.
     *
     * @param taskId    the Id of the general task.
     * @return the variables of the general task.
     */
    String retrieveTaskVariables(String taskId);

    /**
     * Instruct the external system to complete a human task.
     *
     * @param task  the task to be completed.
     *
     * @return the response from the external system.
     */
    SystemResponse completeGeneralTask(GeneralTask task);

    /**
     * Get the URL of the external system this connector connects to.
     *
     * @return the URL of the connected external system.
     */
    String getSystemURL();

}
