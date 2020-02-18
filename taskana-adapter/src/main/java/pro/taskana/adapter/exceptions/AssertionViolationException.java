package pro.taskana.adapter.exceptions;

/** This exception is thrown when an assertion is violated. */
public class AssertionViolationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public AssertionViolationException(String message) {
    super(message);
  }
}
