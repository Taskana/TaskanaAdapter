package io.kadai.adapter.camunda.outbox.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;

public class InvalidArgumentExceptionMapper implements ExceptionMapper<InvalidArgumentException> {

  @Override
  public Response toResponse(InvalidArgumentException exception) {
    return Response.status(Status.BAD_REQUEST).entity(exception.toString()).build();
  }
}
