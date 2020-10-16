package pro.taskana.adapter.camunda.outbox.rest.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

public class InvalidArgumentExceptionMapper implements ExceptionMapper<InvalidArgumentException> {

  @Override
  public Response toResponse(InvalidArgumentException exception) {
    return Response.status(Status.BAD_REQUEST).entity(exception.toString()).build();
  }
}
