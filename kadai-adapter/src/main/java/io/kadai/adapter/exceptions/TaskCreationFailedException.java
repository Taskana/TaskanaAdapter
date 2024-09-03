package io.kadai.adapter.exceptions;

import io.kadai.common.api.exceptions.ErrorCode;
import io.kadai.common.api.exceptions.KadaiException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/** This exception is thrown when the adapter failed to create a task in kadai. */
public class TaskCreationFailedException extends KadaiException {

  public static final String ERROR_KEY = "TASK_CREATION_FAILED";
  private final String externalId;

  public TaskCreationFailedException(String externalId, Throwable cause) {
    super(
        String.format("Error when creating a KADAI task with externalId '%s'", externalId),
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
