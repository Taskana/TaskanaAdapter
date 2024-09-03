package io.kadai.adapter.systemconnector.api;

import java.util.List;

/** This is the interface, a System Connector has to implement. */
public interface SystemConnector {

  /**
   * Retrieve ReferencedTasks that were started within the last polling interval.
   *
   * @return a list of created ReferencedTasks that don't have an associated KADAI task yet.
   */
  List<ReferencedTask> retrieveNewStartedReferencedTasks();

  /**
   * With this call the Adapter notifies the SystemConnector that a list of KADAI tasks has been
   * created. Depending on the Implementation of the SystemConnector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks for which KADAI tasks have been created.
   */
  void kadaiTasksHaveBeenCreatedForNewReferencedTasks(List<ReferencedTask> referencedTasks);

  /**
   * Retrieve ReferencedTasks that were finished.
   *
   * @return a list of ReferencedTasks that were finished
   */
  List<ReferencedTask> retrieveFinishedReferencedTasks();

  /**
   * With this call the Adapter notifies the SystemConnector that a list of KADAI tasks has been
   * terminated. The rationale for this action is that ReferencedTasks in the external system were
   * finished. Depending on the Implementation of the SystemConnector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks for which KADAI Tasks have been terminated.
   */
  void kadaiTasksHaveBeenTerminatedForFinishedReferencedTasks(List<ReferencedTask> referencedTasks);

  /**
   * Get the variables of the ReferencedTask.
   *
   * @param taskId the Id of the ReferencedTask.
   * @return the variables of the ReferencedTask.
   */
  String retrieveReferencedTaskVariables(String taskId);

  /**
   * Instruct the external system to complete a task.
   *
   * @param task the task to be completed.
   * @return the response from the external system.
   */
  SystemResponse completeReferencedTask(ReferencedTask task);

  /**
   * Instruct the external system to claim a task.
   *
   * @param task the task to be claimed.
   * @return the response from the external system.
   */
  SystemResponse claimReferencedTask(ReferencedTask task);

  /**
   * Instruct the external system to claim a task.
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

  /**
   * Get the system identifier of the external system this connector connects to.
   *
   * @return the system identifier of the connected external system.
   */
  String getSystemIdentifier();

  /**
   * With this call the Adapter notifies the SystemConnector that a KADAI task has failed to be
   * created. Depending on the implementation of the SystemConnector, it may ignore this call.
   *
   * @param referencedTask The ReferencedTasks for which the KADAI task failed to be created
   * @param e exception
   */
  void kadaiTaskFailedToBeCreatedForNewReferencedTask(ReferencedTask referencedTask, Exception e);

  /**
   * Instruct the external system to unlock the event.
   *
   * @param eventId the id of the event that needs to be unlocked
   */
  void unlockEvent(String eventId);
}
