package pro.taskana.adapter.exceptions;

/** This exception is thrown when an assertion is violated. */
public class AssertionViolationException extends RuntimeException {

  public AssertionViolationException(String message) {
    super(message);
  }
}
