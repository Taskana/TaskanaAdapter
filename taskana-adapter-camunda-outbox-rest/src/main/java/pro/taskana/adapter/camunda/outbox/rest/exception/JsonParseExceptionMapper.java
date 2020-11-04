package pro.taskana.adapter.camunda.outbox.rest.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import spinjar.com.fasterxml.jackson.core.JsonParseException;

public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

  @Override
  public Response toResponse(JsonParseException exception) {
    return Response.status(Status.BAD_REQUEST).entity(exception.toString()).build();
  }
}
