package io.kadai.adapter.camunda.outbox.rest.controller;

import io.kadai.adapter.camunda.outbox.rest.exception.CamundaTaskEventNotFoundException;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentException;
import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEventList;
import io.kadai.adapter.camunda.outbox.rest.resource.CamundaTaskEventListResource;
import io.kadai.adapter.camunda.outbox.rest.resource.CamundaTaskEventListResourceAssembler;
import io.kadai.adapter.camunda.outbox.rest.resource.CamundaTaskEventResource;
import io.kadai.adapter.camunda.outbox.rest.resource.CamundaTaskEventResourceAssembler;
import io.kadai.adapter.camunda.outbox.rest.service.CamundaTaskEventsService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import spinjar.com.fasterxml.jackson.core.JsonProcessingException;
import spinjar.com.fasterxml.jackson.core.type.TypeReference;
import spinjar.com.fasterxml.jackson.databind.ObjectMapper;

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

  @Path(Mapping.URL_UNLOCK_EVENT)
  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  public Response unlockEvent(
      @PathParam("eventId") final int eventId) {

    camundaTaskEventService.unlockEventForId(
        eventId);

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
