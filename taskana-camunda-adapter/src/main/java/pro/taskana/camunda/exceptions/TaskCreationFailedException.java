package pro.taskana.camunda.exceptions;

import pro.taskana.exceptions.TaskanaException;

/**
 * This exception is thrown when the adapter failed to create a task in taskana.
 * @author bbr
 *
 */
public class TaskCreationFailedException extends TaskanaException {
    public TaskCreationFailedException(String msg) {
        super(msg);
    }

    public TaskCreationFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }

    private static final long serialVersionUID = 1L;


}
