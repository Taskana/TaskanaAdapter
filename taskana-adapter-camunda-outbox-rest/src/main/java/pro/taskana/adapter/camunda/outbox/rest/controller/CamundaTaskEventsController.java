package pro.taskana.adapter.camunda.outbox.rest.controller;

import pro.taskana.adapter.camunda.outbox.rest.service.CamundaTaskEventsService;
import pro.taskana.adapter.camunda.outbox.rest.resource.CamundaTaskEventResource;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/taskana-outbox/rest/events")
public class CamundaTaskEventsController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEvents(@QueryParam("type") final List<String> requestedEventTypes ){

        CamundaTaskEventsService camundaTaskEventService = new CamundaTaskEventsService();

        List<CamundaTaskEventResource> camundaTaskEventResources = camundaTaskEventService.getEvents(requestedEventTypes);

        return Response.status(200).entity(camundaTaskEventResources).build();
    }

    @Path("/delete")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteEvents(String idsAsString){

        CamundaTaskEventsService camundaTaskEventService = new CamundaTaskEventsService();

        camundaTaskEventService.deleteEvents(idsAsString);

        return Response.status(200).build();
    }

}
