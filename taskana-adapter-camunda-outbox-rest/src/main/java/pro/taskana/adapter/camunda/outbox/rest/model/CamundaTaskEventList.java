package pro.taskana.adapter.camunda.outbox.rest.model;

import java.io.Serializable;
import java.util.List;

/** POJO that represents a list of events in the camunda outbox table. */
public class CamundaTaskEventList implements Serializable {

  static final long serialVersionUID = 1L;

  List<CamundaTaskEvent> camundaTaskEvents;

  public List<CamundaTaskEvent> getCamundaTaskEvents() {
    return camundaTaskEvents;
  }

  public void setCamundaTaskEvents(List<CamundaTaskEvent> theResources) {
    this.camundaTaskEvents = theResources;
  }
}
