package pro.taskana.adapter.camunda.outbox.rest.resource;

import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEvent;

/**
 * Transforms {@link CamundaTaskEvent} to its resource counterpart {@link CamundaTaskEventResource}
 * and vice versa.
 */
public class CamundaTaskEventResourceAssembler {

  public CamundaTaskEventResource toResource(CamundaTaskEvent camundaTaskEvent) {

    CamundaTaskEventResource camundaTaskEventResource = new CamundaTaskEventResource();

    camundaTaskEventResource.setId(camundaTaskEvent.getId());
    camundaTaskEventResource.setCreated(camundaTaskEvent.getCreated());
    camundaTaskEventResource.setType(camundaTaskEvent.getType());
    camundaTaskEventResource.setPayload(camundaTaskEvent.getPayload());
    camundaTaskEventResource.setRemainingRetries(camundaTaskEvent.getRemainingRetries());
    camundaTaskEventResource.setBlockedUntil(camundaTaskEvent.getBlockedUntil());
    camundaTaskEventResource.setError(camundaTaskEvent.getError());

    return camundaTaskEventResource;
  }

  public CamundaTaskEvent toModel(CamundaTaskEventResource camundaTaskEventResource) {

    CamundaTaskEvent camundaTaskEvent = new CamundaTaskEvent();

    camundaTaskEvent.setId(camundaTaskEventResource.getId());
    camundaTaskEvent.setCreated(camundaTaskEventResource.getCreated());
    camundaTaskEvent.setType(camundaTaskEventResource.getType());
    camundaTaskEvent.setPayload(camundaTaskEventResource.getPayload());
    camundaTaskEvent.setRemainingRetries(camundaTaskEventResource.getRemainingRetries());
    camundaTaskEvent.setBlockedUntil(camundaTaskEventResource.getBlockedUntil());
    camundaTaskEvent.setError(camundaTaskEventResource.getError());

    return camundaTaskEvent;
  }
}
