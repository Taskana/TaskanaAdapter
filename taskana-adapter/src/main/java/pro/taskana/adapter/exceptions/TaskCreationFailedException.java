package pro.taskana.adapter.exceptions;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import pro.taskana.common.api.exceptions.ErrorCode;
import pro.taskana.common.api.exceptions.TaskanaException;

/** This exception is thrown when the adapter failed to create a task in taskana. */
public class TaskCreationFailedException extends TaskanaException {

  public static final String ERROR_KEY = "TASK_CREATION_FAILED";
  private final String externalId;

  public TaskCreationFailedException(String externalId, Throwable cause) {
    super(
        String.format("Error when creating a TASKANA task with externalId '%s'", externalId),
        ErrorCode.of(ERROR_KEY, createUnmodifiableMap("task", externalId)),
        cause);
    this.externalId = externalId;
  }

  public String getExternalId() {
    return externalId;
  }

  private static Map<String, Serializable> createUnmodifiableMap(String key, Serializable value) {
    HashMap<String, Serializable> map = new HashMap<>();
    map.put(key, ensureNullIsHandled(value));
    return Collections.unmodifiableMap(map);
  }
}
