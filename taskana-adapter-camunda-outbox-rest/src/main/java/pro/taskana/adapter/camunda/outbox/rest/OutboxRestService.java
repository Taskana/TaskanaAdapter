package pro.taskana.adapter.camunda.outbox.rest;

import pro.taskana.adapter.camunda.outbox.rest.dto.ReferencedTaskDTO;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/outbox")
public interface OutboxRestService {


    @Path("/getCreateEvents")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ReferencedTaskDTO> getCreateEvents();


    //In Progress, will be changed to POST + ability to process list of ids
    @Path("/delete/{id}")
    @GET
    public void deleteEvents(@PathParam("id") int id);

}

