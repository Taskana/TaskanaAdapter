package pro.taskana.adapter.camunda.outbox.rest.resource;

import java.time.Instant;

public class CamundaTaskEventResource {

    int id;
    String type;
    String created;
    String payload;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "CamundaTaskEventResource{" +
                "id=" + id +
                ", type='" + type + '\'' +
                ", payload='" + payload + '\'' +
                '}';
    }
}
