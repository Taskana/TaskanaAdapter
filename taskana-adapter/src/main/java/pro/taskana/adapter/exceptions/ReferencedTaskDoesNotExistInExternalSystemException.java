package pro.taskana.adapter.exceptions;

import pro.taskana.exceptions.TaskanaRuntimeException;

/**
 * This exception is thrown when an attempt is made to create a Taskana task for a referenced task
 * that does not exist.
 *
 * @author bbr
 */
public class ReferencedTaskDoesNotExistInExternalSystemException extends TaskanaRuntimeException {

  private static final long serialVersionUID = 1L;

  public ReferencedTaskDoesNotExistInExternalSystemException(String msg) {
    super(msg);
  }

  public ReferencedTaskDoesNotExistInExternalSystemException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
