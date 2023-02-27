package pro.taskana.adapter.exceptions;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import pro.taskana.common.api.exceptions.ErrorCode;
import pro.taskana.common.api.exceptions.TaskanaException;

/** This exception is thrown when the adapter failed to terminate a task in taskana. */
public class TaskTerminationFailedException extends TaskanaException {

  public static final String ERROR_KEY = "TASK_TERMINATION_FAILED";
  private final String taskId;

  public TaskTerminationFailedException(String taskId, Throwable cause) {
    super(
        String.format("Task termination failed for task with id '%s'", taskId),
        ErrorCode.of(ERROR_KEY, createUnmodifiableMap("taskId", taskId)),
        cause);
    this.taskId = taskId;
  }

  public String getTaskId() {
    return taskId;
  }

  private static Map<String, Serializable> createUnmodifiableMap(String key, Serializable value) {
    HashMap<String, Serializable> map = new HashMap<>();
    map.put(key, ensureNullIsHandled(value));
    return Collections.unmodifiableMap(map);
  }
}
