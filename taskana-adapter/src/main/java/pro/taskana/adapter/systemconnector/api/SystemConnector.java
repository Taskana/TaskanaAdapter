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
     * Retrieve referenced tasks that were started after a specified instant.
     *
     * @param createdAfter  the instant after which the tasks were created.
     *
     * @return a list of referenced tasks that were created after the createdAfter instant.
     */
    List<ReferencedTask> retrieveReferencedTasksStartedAfter(Instant createdAfter);

    /**
     * Retrieve referenced tasks that were finished after a specified instant.
     *
     * @param finishedAfter  the instant after which the tasks finished.
     *
     * @return a list of referenced tasks that were finished after the finishedAfter instant.
     */
    List<ReferencedTask> retrieveFinishedTasks(Instant finishedAfter);

    /**
     * Get the variables of the process the referenced task belongs to.
     *
     * @param taskId    the Id of the referenced task.
     * @return the variables of the referenced task's process.
     */
    String retrieveVariables(String taskId);

    /**
     * Instruct the external system to complete a human task.
     *
     * @param task  the task to be completed.
     *
     * @return the response from the external system.
     */
    SystemResponse completeReferencedTask(ReferencedTask task);

    /**
     * Get the URL of the external system this connector connects to.
     *
     * @return the URL of the connected external system.
     */
    String getSystemURL();

}
