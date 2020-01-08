package pro.taskana.adapter.taskanaconnector.api;

import java.util.List;

import pro.taskana.CallbackState;
import pro.taskana.Task;
import pro.taskana.adapter.exceptions.TaskCreationFailedException;
import pro.taskana.adapter.exceptions.TaskTerminationFailedException;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;

/**
 * The interface that must be implemented by a SystemConnector to a taskana system.
 *
 * @author bbr
 */
public interface TaskanaConnector {

  /**
   * retrieve completed taskana tasks.
   *
   * @return a list of completed taskana tasks
   */
  List<ReferencedTask> retrieveCompletedTaskanaTasksAsReferencedTasks();

  /**
   * retrieve claimed taskana tasks.
   *
   * @return a list of claimed taskana tasks
   */
  List<ReferencedTask> retrieveClaimedTaskanaTasksAsReferencedTasks();

  /**
   * retrieve forcefully unclaimed taskana tasks.
   *
   * @return a list of cancelled taskana tasks
   */
  List<ReferencedTask> retrieveCancelledClaimTaskanaTasksAsReferencedTasks();

  /**
   * With this call the Adapter notifies the TaskanaConnector that the CallbackState of a list of
   * ReferencedTasks needs to be modified due to completion or claim of tasks of Taskana Tasks.
   * Depending on the Implementation of the System Connector, it may ignore this call.
   *
   * @param referencedTasks List of ReferencedTasks that have been completed on the external system
   * @param desiredCallbackState the CallbackState that needs to be set for the list of
   *     referencedTasks
   */
  void changeReferencedTaskCallbackState(
      List<ReferencedTask> referencedTasks, CallbackState desiredCallbackState);

  /**
   * create a task in taskana on behalf of an external task.
   *
   * @param taskanaTask The taskana task to be created.
   * @throws TaskCreationFailedException if the attempt to create a taskana task failed.
   */
  void createTaskanaTask(Task taskanaTask) throws TaskCreationFailedException;

  /**
   * Convert a referenced task to a Taskana task.
   *
   * @param referencedTask the referenced task that is to be converted.
   * @return the taskana task that will be created started on behalf of the referenced task.
   */
  Task convertToTaskanaTask(ReferencedTask referencedTask);

  /**
   * Convert a taskana task into a referenced task.
   *
   * @param task the taskana task that was executed on behalf of a referenced task.
   * @return the referenced task for which the taskana task was executed.
   */
  ReferencedTask convertToReferencedTask(Task task);

  /**
   * terminate taskana task that runs on behalf of an external task.
   *
   * @param referencedTask The external task on behalf of which the taskana task is running.
   * @throws TaskTerminationFailedException if the attempt to terminate a taskana task failed.
   */
  void terminateTaskanaTask(ReferencedTask referencedTask) throws TaskTerminationFailedException;
}
