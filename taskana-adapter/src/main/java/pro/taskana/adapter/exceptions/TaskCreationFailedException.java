package pro.taskana.adapter.exceptions;

import pro.taskana.common.api.exceptions.TaskanaException;

/** This exception is thrown when the adapter failed to create a task in taskana. */
public class TaskCreationFailedException extends TaskanaException {

  public TaskCreationFailedException(String msg) {
    super(msg);
  }

  public TaskCreationFailedException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
