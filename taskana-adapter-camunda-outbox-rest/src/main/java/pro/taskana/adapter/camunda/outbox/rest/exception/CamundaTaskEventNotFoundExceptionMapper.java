package pro.taskana.adapter.camunda.outbox.rest.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

public class CamundaTaskEventNotFoundExceptionMapper
    implements ExceptionMapper<CamundaTaskEventNotFoundException> {

  @Override
  public Response toResponse(CamundaTaskEventNotFoundException exception) {
    return Response.status(Status.NOT_FOUND).entity(exception.toString()).build();
  }
}
