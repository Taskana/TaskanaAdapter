package pro.taskana.adapter.camunda.outbox.rest.exception;

/** This exception is thrown when a generic problem is encountered. */
public class SystemException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SystemException(String msg) {
    super(msg);
  }

  public SystemException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
