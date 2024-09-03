package io.kadai.adapter.kadaiconnector.api;

import io.kadai.adapter.exceptions.TaskCreationFailedException;
import io.kadai.adapter.exceptions.TaskTerminationFailedException;
import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.task.api.CallbackState;
import io.kadai.task.api.models.Task;
import java.util.List;

/** The interface that must be implemented by a SystemConnector to a KADAI system. */
public interface KadaiConnector {

  /**
   * retrieve finished KADAI tasks.
   *
   * @return a list of finished KADAI tasks
   */
  List<ReferencedTask> retrieveFinishedKadaiTasksAsReferencedTasks();

  /**
   * retrieve claimed KADAI tasks.
   *
   * @return a list of claimed KADAI tasks
   */
  List<ReferencedTask> retrieveClaimedKadaiTasksAsReferencedTasks();

  /**
   * retrieve cancelled claim KADAI tasks.
   *
   * @return a list of cancelled claim KADAI tasks
   */
  List<ReferencedTask> retrieveCancelledClaimKadaiTasksAsReferencedTasks();

  /**
   * With this call the Adapter notifies the KadaiConnector that the CallbackState of a list of
   * ReferencedTasks needs to be modified due to completion or claim of tasks of KADAI Tasks.
   * Depending on the Implementation of the System Connector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks that have been completed on the external system
   * @param desiredCallbackState the CallbackState that needs to be set for the list of
   *     referencedTasks
   */
  void changeTaskCallbackState(
      List<ReferencedTask> referencedTasks, CallbackState desiredCallbackState);

  /**
   * create a task in KADAI on behalf of an external task.
   *
   * @param kadaiTask The KADAI task to be created.
   * @throws TaskCreationFailedException if the attempt to create a KADAI task failed.
   */
  void createKadaiTask(Task kadaiTask) throws TaskCreationFailedException;

  /**
   * Convert a ReferencedTask to a KADAI task.
   *
   * @param referencedTask the ReferencedTask that is to be converted.
   * @return the KADAI task that will be created started on behalf of the ReferencedTask.
   */
  Task convertToKadaiTask(ReferencedTask referencedTask);

  /**
   * Convert a KADAI task into a ReferencedTask.
   *
   * @param task the KADAI task that was executed on behalf of a ReferencedTask.
   * @return the ReferencedTask for which the KADAI task was executed.
   */
  ReferencedTask convertToReferencedTask(Task task);

  /**
   * terminate KADAI task that runs on behalf of an external task.
   *
   * @param referencedTask The external task on behalf of which the KADAI task is running.
   * @throws TaskTerminationFailedException if the attempt to terminate a KADAI task failed.
   */
  void terminateKadaiTask(ReferencedTask referencedTask) throws TaskTerminationFailedException;
}
