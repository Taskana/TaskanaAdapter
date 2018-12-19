package pro.taskana.camunda.exceptions;

import pro.taskana.exceptions.TaskanaException;

/**
 * This exception is thrown when an attempt to convert a camunda task into a taskana task fails.
 * @author bbr
 *
 */
public class TaskConversionFailedException extends TaskanaException {
    public TaskConversionFailedException(String msg) {
        super(msg);
    }

    public TaskConversionFailedException(String msg, Throwable cause) {
        super(msg, cause);
    }

    private static final long serialVersionUID = 1L;


}
