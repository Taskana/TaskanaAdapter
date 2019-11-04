package pro.taskana.adapter.camunda.outbox.rest;

import pro.taskana.adapter.camunda.outbox.rest.dto.ReferencedTaskDTO;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/outbox")
public interface OutboxRestService {


    @Path("/getCreateEvents")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    List<ReferencedTaskDTO> getCreateEvents();


    @Path("/delete")
    @DELETE
    void deleteEvents(@QueryParam("ids") String ids);

}

