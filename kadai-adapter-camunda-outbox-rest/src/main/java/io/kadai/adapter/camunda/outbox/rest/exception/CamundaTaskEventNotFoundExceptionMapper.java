package io.kadai.adapter.camunda.outbox.rest.exception;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;

public class CamundaTaskEventNotFoundExceptionMapper
    implements ExceptionMapper<CamundaTaskEventNotFoundException> {

  @Override
  public Response toResponse(CamundaTaskEventNotFoundException exception) {
    return Response.status(Status.NOT_FOUND).entity(exception.toString()).build();
  }
}
