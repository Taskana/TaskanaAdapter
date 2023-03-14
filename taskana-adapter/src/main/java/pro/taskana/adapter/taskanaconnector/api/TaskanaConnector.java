package pro.taskana.adapter.taskanaconnector.api;

import java.util.List;
import pro.taskana.adapter.exceptions.TaskCreationFailedException;
import pro.taskana.adapter.exceptions.TaskTerminationFailedException;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.task.api.CallbackState;
import pro.taskana.task.api.models.Task;

/** The interface that must be implemented by a SystemConnector to a TASKANA system. */
public interface TaskanaConnector {

  /**
   * retrieve finished TASKANA tasks.
   *
   * @return a list of finished TASKANA tasks
   */
  List<ReferencedTask> retrieveFinishedTaskanaTasksAsReferencedTasks();

  /**
   * retrieve claimed TASKANA tasks.
   *
   * @return a list of claimed TASKANA tasks
   */
  List<ReferencedTask> retrieveClaimedTaskanaTasksAsReferencedTasks();

  /**
   * retrieve cancelled claim TASKANA tasks.
   *
   * @return a list of cancelled claim TASKANA tasks
   */
  List<ReferencedTask> retrieveCancelledClaimTaskanaTasksAsReferencedTasks();

  /**
   * With this call the Adapter notifies the TaskanaConnector that the CallbackState of a list of
   * ReferencedTasks needs to be modified due to completion or claim of tasks of TASKANA Tasks.
   * Depending on the Implementation of the System Connector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks that have been completed on the external system
   * @param desiredCallbackState the CallbackState that needs to be set for the list of
   *     referencedTasks
   */
  void changeTaskCallbackState(
      List<ReferencedTask> referencedTasks, CallbackState desiredCallbackState);

  /**
   * create a task in TASKANA on behalf of an external task.
   *
   * @param taskanaTask The TASKANA task to be created.
   * @throws TaskCreationFailedException if the attempt to create a TASKANA task failed.
   */
  void createTaskanaTask(Task taskanaTask) throws TaskCreationFailedException;

  /**
   * Convert a ReferencedTask to a TASKANA task.
   *
   * @param referencedTask the ReferencedTask that is to be converted.
   * @return the TASKANA task that will be created started on behalf of the ReferencedTask.
   */
  Task convertToTaskanaTask(ReferencedTask referencedTask);

  /**
   * Convert a TASKANA task into a ReferencedTask.
   *
   * @param task the TASKANA task that was executed on behalf of a ReferencedTask.
   * @return the ReferencedTask for which the TASKANA task was executed.
   */
  ReferencedTask convertToReferencedTask(Task task);

  /**
   * terminate TASKANA task that runs on behalf of an external task.
   *
   * @param referencedTask The external task on behalf of which the TASKANA task is running.
   * @throws TaskTerminationFailedException if the attempt to terminate a TASKANA task failed.
   */
  void terminateTaskanaTask(ReferencedTask referencedTask) throws TaskTerminationFailedException;
}
