package io.kadai.adapter.camunda.outbox.rest.resource;

import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEventList;
import java.io.Serializable;
import java.util.List;

/**
 * This class wraps a list of CamundaTaskEventResources in a seperate object. This is needed to make
 * the REST-API work with Jersey
 */
public class CamundaTaskEventListResource implements Serializable {

  private List<CamundaTaskEvent> camundaTaskEvents;

  public CamundaTaskEventListResource() {}

  public CamundaTaskEventListResource(
      CamundaTaskEventList camundaTaskEventList) {
    this.camundaTaskEvents = camundaTaskEventList.getCamundaTaskEvents();
  }

  public List<CamundaTaskEvent> getCamundaTaskEvents() {
    return camundaTaskEvents;
  }

  public void setCamundaTaskEvents(List<CamundaTaskEvent> theResources) {
    this.camundaTaskEvents = theResources;
  }
}
