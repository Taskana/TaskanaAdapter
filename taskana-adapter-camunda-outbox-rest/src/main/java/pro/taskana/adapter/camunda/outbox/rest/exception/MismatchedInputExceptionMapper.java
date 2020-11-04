package pro.taskana.adapter.camunda.outbox.rest.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import spinjar.com.fasterxml.jackson.databind.exc.MismatchedInputException;

public class MismatchedInputExceptionMapper implements ExceptionMapper<MismatchedInputException> {

  @Override
  public Response toResponse(MismatchedInputException exception) {
    return Response.status(Status.BAD_REQUEST).entity(exception.toString()).build();
  }
}
