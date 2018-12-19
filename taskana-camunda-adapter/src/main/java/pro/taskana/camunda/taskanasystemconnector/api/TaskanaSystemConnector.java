package pro.taskana.camunda.taskanasystemconnector.api;

import java.time.Instant;
import java.util.List;

import pro.taskana.Task;
import pro.taskana.camunda.camundasystemconnector.api.CamundaTask;
import pro.taskana.camunda.exceptions.TaskConversionFailedException;
import pro.taskana.camunda.exceptions.TaskCreationFailedException;

/**
 * The interface that must be implemented by a SystemConnector to a taskana system.
 * @author bbr
 *
 */
public interface TaskanaSystemConnector {
    /**
     * retrieve completed taskana tasks.
     * @param completedAfter  The instant after which the tasks have been completed.
     * @return
     */
    List<CamundaTask> retrieveCompletedTaskanaTasks(Instant completedAfter);

    /**
     * create a task in taskana on behalf of a camunda task.
     * @param camundaTask                   The camunda task for which the taskana task is to be created.
     * @throws TaskCreationFailedExceptioin if the attempt to create a taskana task failed.
     */
    void createTaskanaTask(Task taskanaTask) throws TaskCreationFailedException;

    /**
     * Convert a camunda task into a Taskana task.
     * @param camundaTask   the camunda task that is to be converted.
     * @return              the taskana task that will be created started on behalf of the camunda task.
     */
    Task convertToTaskanaTask(CamundaTask camundaTask) throws TaskConversionFailedException;

    /**
     * Convert a taskana task into a camunda task.
     * @param task  the taskana task that was executed on behalf of a camunda task.
     * @return      the camunda task for which the taskana task was executed.
     */
    CamundaTask convertToCamundaTask(Task task);

}
