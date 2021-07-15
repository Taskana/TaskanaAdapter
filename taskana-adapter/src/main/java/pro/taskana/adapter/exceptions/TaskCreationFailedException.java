package pro.taskana.adapter.exceptions;

import pro.taskana.common.api.exceptions.ErrorCode;
import pro.taskana.common.api.exceptions.TaskanaException;
import pro.taskana.common.internal.util.MapCreator;

/** This exception is thrown when the adapter failed to create a task in taskana. */
public class TaskCreationFailedException extends TaskanaException {

  public static final String ERROR_KEY = "TASK_CREATION_FAILED";
  private final String externalId;

  public TaskCreationFailedException(String externalId, Throwable cause) {
    super(
        String.format("Error when creating a TASKANA task with externalId '%s'", externalId),
        ErrorCode.of(ERROR_KEY, MapCreator.of("task", externalId)),
        cause);
    this.externalId = externalId;
  }

  public String getExternalId() {
    return externalId;
  }
}
