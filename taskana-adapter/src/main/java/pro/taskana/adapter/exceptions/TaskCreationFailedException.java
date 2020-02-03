package pro.taskana.adapter.exceptions;


import pro.taskana.common.api.exceptions.TaskanaException;

/**
 * This exception is thrown when the adapter failed to create a task in taskana.
 *
 * @author bbr
 */
public class TaskCreationFailedException extends TaskanaException {

  private static final long serialVersionUID = 1L;

  public TaskCreationFailedException(String msg) {
    super(msg);
  }

  public TaskCreationFailedException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
