package pro.taskana.adapter.camunda.outbox.rest;

import java.io.Serializable;
import java.util.List;

/**
 * This class wraps a list of CamundaTaskEventResources in a seperate object. This is needed to make
 * the REST-API work with Jersey
 */
public class CamundaTaskEventListResource implements Serializable {

  static final long serialVersionUID = 1L;

  List<CamundaTaskEvent> camundaTaskEvents;

  public List<CamundaTaskEvent> getCamundaTaskEvents() {
    return camundaTaskEvents;
  }

  public void setCamundaTaskEvents(List<CamundaTaskEvent> theResources) {
    this.camundaTaskEvents = theResources;
  }
}
