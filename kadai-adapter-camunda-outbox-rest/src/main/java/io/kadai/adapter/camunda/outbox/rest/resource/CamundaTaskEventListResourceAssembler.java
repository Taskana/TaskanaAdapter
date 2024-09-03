package io.kadai.adapter.camunda.outbox.rest.resource;

import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEventList;

/**
 * Transforms {@link CamundaTaskEventList} to its resource counterpart {@link
 * CamundaTaskEventListResource} and vice versa.
 */
public class CamundaTaskEventListResourceAssembler {

  public CamundaTaskEventListResource toResource(CamundaTaskEventList camundaTaskEventList) {
    return new CamundaTaskEventListResource(camundaTaskEventList);
  }

  public CamundaTaskEventList toModel(CamundaTaskEventListResource camundaTaskEventListResource) {
    CamundaTaskEventList camundaTaskEventList = new CamundaTaskEventList();
    camundaTaskEventList.setCamundaTaskEvents(camundaTaskEventListResource.getCamundaTaskEvents());

    return camundaTaskEventList;
  }
}
