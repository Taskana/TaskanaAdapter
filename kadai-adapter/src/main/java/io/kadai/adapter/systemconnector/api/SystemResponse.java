package io.kadai.adapter.systemconnector.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/** encapsulate a response from the external system. */
public class SystemResponse {
  private final HttpStatus statusCode;
  private final Throwable throwable;

  public SystemResponse(int statusCode, Throwable throwable) {
    this(HttpStatus.resolve(statusCode), throwable);
  }

  public SystemResponse(HttpStatusCode statusCode, Throwable throwable) {
    this(HttpStatus.resolve(statusCode.value()), throwable);
  }

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
