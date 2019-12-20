package pro.taskana.adapter.camunda.outbox.rest.resource;

import java.io.Serializable;
import java.util.List;

import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEvent;

/**
 * This class wraps a list of CamundaTaskEventResources in a seperate object.
 * This is needed to make the REST-API work with Jersey
 *
 *
 */

public class CamundaTaskEventListResource implements Serializable {

    static final long serialVersionUID = 1L;

    List<CamundaTaskEvent> camundaTaskEvents;

    public CamundaTaskEventListResource() {

    }

    public CamundaTaskEventListResource(pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEventList camundaTaskEventList) {
        this.camundaTaskEvents = camundaTaskEventList.getCamundaTaskEvents();
    }

    public List<CamundaTaskEvent> getCamundaTaskEvents() {
        return camundaTaskEvents;
    }

    public void setCamundaTaskEvents(List<CamundaTaskEvent> theResources) {
        this.camundaTaskEvents = theResources;
    }

}
