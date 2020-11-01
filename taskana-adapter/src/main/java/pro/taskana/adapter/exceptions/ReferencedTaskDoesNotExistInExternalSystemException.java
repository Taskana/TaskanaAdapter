package pro.taskana.adapter.exceptions;

import pro.taskana.common.api.exceptions.TaskanaRuntimeException;

/**
 * This exception is thrown when an attempt is made to create a Taskana task for a referenced task
 * that does not exist.
 */
public class ReferencedTaskDoesNotExistInExternalSystemException extends TaskanaRuntimeException {

  public ReferencedTaskDoesNotExistInExternalSystemException(String msg) {
    super(msg);
  }

  public ReferencedTaskDoesNotExistInExternalSystemException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
