package pro.taskana.adapter.camunda.outbox.rest.exception;

import java.io.Serializable;

public class CamundaTaskEventNotFoundException extends Exception implements Serializable {

  private static final long serialVersionUID = 1L;

  public CamundaTaskEventNotFoundException(String msg) {
    super(msg);
  }

  public CamundaTaskEventNotFoundException(String msg, Throwable cause) {
    super(msg, cause);
  }

}
