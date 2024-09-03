package io.kadai.adapter.exceptions;

import io.kadai.common.api.exceptions.ErrorCode;
import io.kadai.common.api.exceptions.KadaiRuntimeException;

/** This exception is thrown when an assertion is violated. */
public class AssertionViolationException extends KadaiRuntimeException {

  public static final String ERROR_KEY = "ASSERTION_FAILED";

  public AssertionViolationException(String message) {
    super(message, ErrorCode.of(ERROR_KEY));
  }
}
