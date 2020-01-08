package pro.taskana.adapter.systemconnector.api;

import org.springframework.http.HttpStatus;

/**
 * encapsulate a response from the external system.
 *
 * @author bbr
 */
public class SystemResponse {
  private HttpStatus statusCode;
  private Throwable throwable;

  public SystemResponse(HttpStatus statusCode, Throwable throwable) {
    this.statusCode = statusCode;
    this.throwable = throwable;
  }

  public HttpStatus getStatusCode() {
    return statusCode;
  }

  public Throwable getThrowable() {
    return throwable;
  }
}
