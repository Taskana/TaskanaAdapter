package pro.taskana.adapter.exceptions;

import pro.taskana.exceptions.TaskanaException;

/**
 * This exception is thrown when the adapter failed to terminate a task in taskana.
 * @author bbr
 *
 */
public class TaskTerminationFailedException extends TaskanaException {
    public TaskTerminationFailedException(String msg) {
        super(msg);
    }

    public TaskTerminationFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }

    private static final long serialVersionUID = 1L;


}
