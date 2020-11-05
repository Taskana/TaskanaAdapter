package pro.taskana.adapter.camunda.outbox.rest.controller;

import java.util.List;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import spinjar.com.fasterxml.jackson.core.JsonProcessingException;
import spinjar.com.fasterxml.jackson.core.type.TypeReference;
import spinjar.com.fasterxml.jackson.databind.ObjectMapper;

import pro.taskana.adapter.camunda.outbox.rest.exception.CamundaTaskEventNotFoundException;
import pro.taskana.adapter.camunda.outbox.rest.exception.InvalidArgumentException;
import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEventList;
import pro.taskana.adapter.camunda.outbox.rest.resource.CamundaTaskEventListResource;
import pro.taskana.adapter.camunda.outbox.rest.resource.CamundaTaskEventListResourceAssembler;
import pro.taskana.adapter.camunda.outbox.rest.resource.CamundaTaskEventResource;
import pro.taskana.adapter.camunda.outbox.rest.resource.CamundaTaskEventResourceAssembler;
import pro.taskana.adapter.camunda.outbox.rest.service.CamundaTaskEventsService;

/** Controller for the Outbox REST service. */
@Path(Mapping.URL_EVENTS)
public class CamundaTaskEventsController {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  CamundaTaskEventsService camundaTaskEventService = new CamundaTaskEventsService();
  CamundaTaskEventResourceAssembler camundaTaskEventResourceAssembler =
      new CamundaTaskEventResourceAssembler();
  CamundaTaskEventListResourceAssembler camundaTaskEventListResourceAssembler =
      new CamundaTaskEventListResourceAssembler();

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEvents(@Context UriInfo uriInfo) throws InvalidArgumentException {

    CamundaTaskEventList camundaTaskEventList = new CamundaTaskEventList();

    MultivaluedMap<String, String> filterParams = uriInfo.getQueryParameters();

    List<CamundaTaskEvent> camundaTaskEvents = camundaTaskEventService.getEvents(filterParams);

    camundaTaskEventList.setCamundaTaskEvents(camundaTaskEvents);

    CamundaTaskEventListResource camundaTaskEventListResource =
        camundaTaskEventListResourceAssembler.toResource(camundaTaskEventList);

    return Response.status(200).entity(camundaTaskEventListResource).build();
  }

  @Path(Mapping.URL_EVENT)
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEvent(@PathParam("eventId") final int eventId)
      throws CamundaTaskEventNotFoundException {

    CamundaTaskEvent camundaTaskEvent = camundaTaskEventService.getEvent(eventId);

    CamundaTaskEventResource camundaTaskEventResource =
        camundaTaskEventResourceAssembler.toResource(camundaTaskEvent);

    return Response.status(200).entity(camundaTaskEventResource).build();
  }

  @Path(Mapping.URL_DELETE_EVENTS)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteEvents(String idsAsString) {

    camundaTaskEventService.deleteEvents(idsAsString);

    return Response.status(204).build();
  }

  @Path(Mapping.URL_DECREASE_REMAINING_RETRIES)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response decreaseRemainingRetriesAndLogError(
      String eventIdOfTaskFailedToStartAndErrorLog) {

    camundaTaskEventService.decreaseRemainingRetriesAndLogError(
        eventIdOfTaskFailedToStartAndErrorLog);

    return Response.status(204).build();
  }

  @Path(Mapping.URL_EVENT)
  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response setRemainingRetries(@PathParam("eventId") int eventId, String body)
      throws InvalidArgumentException, CamundaTaskEventNotFoundException, JsonProcessingException {

    Map<String, Integer> patchMap =
        OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, Integer>>() {});

    if (patchMap == null || !patchMap.containsKey("remainingRetries")) {
      throw new InvalidArgumentException(
          "Please provide a valid json body in the format {\"remainingRetries\":3}");
    }

    int retriesToSet = patchMap.get("remainingRetries");

    CamundaTaskEvent camundaTaskEvent =
        camundaTaskEventService.setRemainingRetries(eventId, retriesToSet);

    CamundaTaskEventResource camundaTaskEventResource =
        camundaTaskEventResourceAssembler.toResource(camundaTaskEvent);

    return Response.status(200).entity(camundaTaskEventResource).build();
  }

  @PATCH
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response setRemainingRetriesForMultipleEvents(
      @QueryParam("retries") final String retries, String body)
      throws InvalidArgumentException, JsonProcessingException {

    if (retries == null || retries.isEmpty()) {
      throw new InvalidArgumentException("Please provide a valid \"retries\" query parameter");
    }

    Map<String, Integer> patchMap =
        OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, Integer>>() {});

    if (patchMap == null || !patchMap.containsKey("remainingRetries")) {
      throw new InvalidArgumentException(
          "Please provide a valid json body in the format {\"remainingRetries\":3}");
    }

    int retriesToSet = patchMap.get("remainingRetries");

    int remainingRetries = Integer.parseInt(retries);

    List<CamundaTaskEvent> camundaTaskEvents =
        camundaTaskEventService.setRemainingRetriesForMultipleEvents(
            remainingRetries, retriesToSet);

    CamundaTaskEventList camundaTaskEventList = new CamundaTaskEventList();
    camundaTaskEventList.setCamundaTaskEvents(camundaTaskEvents);

    CamundaTaskEventListResource camundaTaskEventListResource =
        camundaTaskEventListResourceAssembler.toResource(camundaTaskEventList);

    return Response.status(200).entity(camundaTaskEventListResource).build();
  }

  @Path(Mapping.URL_EVENT)
  @DELETE
  public Response deleteFailedEvent(@PathParam("eventId") int eventId) {

    camundaTaskEventService.deleteFailedEvent(eventId);

    return Response.status(204).build();
  }

  @Path(Mapping.DELETE_FAILED_EVENTS)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response deleteAllFailedEvents() {

    camundaTaskEventService.deleteAllFailedEvents();

    return Response.status(204).build();
  }

  @GET
  @Path(Mapping.URL_COUNT_FAILED_EVENTS)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEventsCount(@QueryParam("retries") int remainingRetries) {

    String failedEventsCount = camundaTaskEventService.getEventsCount(remainingRetries);

    return Response.status(200).entity(failedEventsCount).build();
  }
}
