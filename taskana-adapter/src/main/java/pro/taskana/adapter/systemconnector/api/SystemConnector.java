package pro.taskana.adapter.systemconnector.api;

import java.util.List;

/** This is the interface, a System Connector has to implement. */
public interface SystemConnector {

  /**
   * Retrieve ReferencedTasks that were started within the last polling interval.
   *
   * @return a list of created ReferencedTasks that don't have an associated TASKANA task yet.
   */
  List<ReferencedTask> retrieveNewStartedReferencedTasks();

  /**
   * With this call the Adapter notifies the SystemConnector that a list of TASKANA tasks has been
   * created. Depending on the Implementation of the SystemConnector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks for which TASKANA tasks have been created.
   */
  void taskanaTasksHaveBeenCreatedForNewReferencedTasks(List<ReferencedTask> referencedTasks);

  /**
   * Retrieve ReferencedTasks that were finished.
   *
   * @return a list of ReferencedTasks that were finished
   */
  List<ReferencedTask> retrieveFinishedReferencedTasks();

  /**
   * With this call the Adapter notifies the SystemConnector that a list of TASKANA tasks has been
   * terminated. The rationale for this action is that ReferencedTasks in the external system were
   * finished. Depending on the Implementation of the SystemConnector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks for which TASKANA Tasks have been terminated.
   */
  void taskanaTasksHaveBeenTerminatedForFinishedReferencedTasks(
      List<ReferencedTask> referencedTasks);

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
   * With this call the Adapter notifies the SystemConnector that a TASKANA task has failed to be
   * created. Depending on the implementation of the SystemConnector, it may ignore this call.
   *
   * @param referencedTask The ReferencedTasks for which the TASKANA task failed to be created
   * @param e exception
   */
  void taskanaTaskFailedToBeCreatedForNewReferencedTask(ReferencedTask referencedTask, Exception e);

  /**
   * Instruct the external system to unlock the event.
   *
   * @param eventId the id of the event that needs to be unlocked
   */
  void unlockEvent(String eventId);
}
