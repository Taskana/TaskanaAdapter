package pro.taskana.adapter.camunda.outbox.rest.jboss;

import pro.taskana.adapter.camunda.outbox.rest.core.dto.ReferencedTaskDTO;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/outbox")
public interface OutboxRestServiceJboss {


    @Path("/getCreateEvents")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ReferencedTaskDTO> getCreateEvents();


    @Path("/delete")
    @DELETE
    void deleteEvents(@QueryParam("ids") String ids);

}