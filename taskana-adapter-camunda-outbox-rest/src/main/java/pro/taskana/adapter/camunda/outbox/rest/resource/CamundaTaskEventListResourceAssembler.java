package pro.taskana.adapter.camunda.outbox.rest.resource;

import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEventList;

/**
 * Transforms {@link CamundaTaskEventList} to its resource counterpart {@link
 * CamundaTaskEventListResource} and vice versa.
 */
public class CamundaTaskEventListResourceAssembler {

  public CamundaTaskEventListResource toResource(CamundaTaskEventList camundaTaskEventList) {
    CamundaTaskEventListResource resource = new CamundaTaskEventListResource(camundaTaskEventList);
    return resource;
  }

  public CamundaTaskEventList toModel(CamundaTaskEventListResource camundaTaskEventListResource) {
    CamundaTaskEventList camundaTaskEventList = new CamundaTaskEventList();
    camundaTaskEventList.setCamundaTaskEvents(camundaTaskEventListResource.getCamundaTaskEvents());

    return camundaTaskEventList;
  }
}
