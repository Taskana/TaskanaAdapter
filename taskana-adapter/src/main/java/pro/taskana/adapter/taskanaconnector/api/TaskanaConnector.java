package pro.taskana.adapter.taskanaconnector.api;

import java.util.List;

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
    List<ReferencedTask> retrieveCompletedTaskanaTasks();

    /**
     * With this call the Adapter notifies the TaskanaConnector that a list of Referenced Tasks has been completed on
     * behalf of completion of Taskana Tasks. Depending on the Implementation of the System Connector, it may ignore
     * this call.
     *
     * @param referencedTasks
     *            List of ReferencedTasks that have been completed on the external system
     */
    void referencedTasksHaveBeenCompleted(List<ReferencedTask> referencedTasks);

    /**
     * create a task in taskana on behalf of an external task.
     *
     * @param taskanaTask
     *            The taskana task to be created.
     * @throws TaskCreationFailedException
     *             if the attempt to create a taskana task failed.
     */
    void createTaskanaTask(Task taskanaTask) throws TaskCreationFailedException;

    /**
     * Convert a referenced task to a Taskana task.
     *
     * @param referencedTask
     *            the referenced task that is to be converted.
     * @return the taskana task that will be created started on behalf of the referenced task.
     */
    Task convertToTaskanaTask(ReferencedTask referencedTask);

    /**
     * Convert a taskana task into a referenced task.
     *
     * @param task
     *            the taskana task that was executed on behalf of a referenced task.
     * @return the referenced task for which the taskana task was executed.
     */
    ReferencedTask convertToReferencedTask(Task task);

    /**
     * terminate taskana task that runs on behalf of an external task.
     *
     * @param referencedTask
     *            The external task on behalf of which the taskana task is running.
     * @throws TaskTerminationFailedException
     *             if the attempt to terminate a taskana task failed.
     */
    void terminateTaskanaTask(ReferencedTask referencedTask) throws TaskTerminationFailedException;

}
