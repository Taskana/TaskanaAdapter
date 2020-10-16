package pro.taskana.adapter.camunda.outbox.rest.exception;

import java.io.Serializable;

public class InvalidArgumentException extends Exception implements Serializable {

  private static final long serialVersionUID = 1L;

  public InvalidArgumentException(String msg) {
    super(msg);
  }

  public InvalidArgumentException(String msg, Throwable cause) {
    super(msg, cause);
  }
}
