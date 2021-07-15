package pro.taskana.adapter.exceptions;

import pro.taskana.common.api.exceptions.ErrorCode;
import pro.taskana.common.api.exceptions.TaskanaRuntimeException;

/** This exception is thrown when an assertion is violated. */
public class AssertionViolationException extends TaskanaRuntimeException {

  public static final String ERROR_KEY = "ASSERTION_FAILED";

  public AssertionViolationException(String message) {
    super(message, ErrorCode.of(ERROR_KEY));
  }
}
