package pro.taskana.adapter.systemconnector.api;

import java.util.List;

/**
 * This is the interface, a System Connector has to implement.
 *
 * @author bbr
 */
public interface SystemConnector {

  /**
   * Retrieve referenced tasks that were started since the last polling interval.
   *
   * @return a list of referenced tasks that were created and have not yet an associated taskana
   *     task.
   */
  List<ReferencedTask> retrieveNewStartedReferencedTasks();

  /**
   * With this call the Adapter notifies the SystemConnector that a list of Taskana Tasks has been
   * created. Depending on the Implementation of the System Connector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks for which Taskana Tasks have been created.
   */
  void taskanaTasksHaveBeenCreatedForNewReferencedTasks(List<ReferencedTask> referencedTasks);

  /**
   * Retrieve referenced tasks that were finished.
   *
   * @return a list of referenced tasks that were finished after the finishedAfter instant.
   */
  List<ReferencedTask> retrieveTerminatedTasks();

  /**
   * With this call the Adapter notifies the SystemConnector that a list of Taskana Tasks has been
   * terminated. The rationale for this action is that referenced tasks in the external system were
   * terminated. Depending on the Implementation of the System Connector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks for which Taskana Tasks have been terminated.
   */
  void taskanaTasksHaveBeenCompletedForTerminatedReferencedTasks(
      List<ReferencedTask> referencedTasks);

  /**
   * Get the variables of the process the referenced task belongs to.
   *
   * @param taskId the Id of the referenced task.
   * @return the variables of the referenced task's process.
   */
  String retrieveVariables(String taskId);

  /**
   * Instruct the external system to complete a human task.
   *
   * @param task the task to be completed.
   * @return the response from the external system.
   */
  SystemResponse completeReferencedTask(ReferencedTask task);

  /**
   * Instruct the external system to claim a human task.
   *
   * @param task the task to be claimed.
   * @return the response from the external system.
   */
  SystemResponse claimReferencedTask(ReferencedTask task);

  /**
   * Instruct the external system to claim a human task.
   *
   * @param task the task to cancel the claim on.
   * @return the response from the external system.
   */
  SystemResponse cancelClaimReferencedTask(ReferencedTask task);

  /**
   * Get the URL of the external system this connector connects to.
   *
   * @return the URL of the connected external system.
   */
  String getSystemUrl();
}
