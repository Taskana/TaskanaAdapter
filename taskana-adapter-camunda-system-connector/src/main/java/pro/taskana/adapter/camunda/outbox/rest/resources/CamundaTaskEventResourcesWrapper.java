package pro.taskana.adapter.camunda.outbox.rest.resources;

import java.io.Serializable;
import java.util.List;

/**
 * This class wraps a list of CamundaTaskEventResources in a seperate object.
 * This is needed to make the REST-API work with Jersey
 *
 *
 */

public class CamundaTaskEventResourcesWrapper implements Serializable {

    static final long serialVersionUID = 1L;

    List<CamundaTaskEventResource> camundaTaskEventResources;

    public List<CamundaTaskEventResource> getCamundaTaskEventResources() {
        return camundaTaskEventResources;
    }

    public void setCamundaTaskEventResources(List<CamundaTaskEventResource> theResources) {
        this.camundaTaskEventResources = theResources;
    }

}
