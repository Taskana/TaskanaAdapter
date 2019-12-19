package pro.taskana.adapter.camunda.outbox.rest.resources;

/**
 * POJO that represents an event in the camunda outbox table.
 *
 * @author jhe.
 */
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
        StringBuilder builder = new StringBuilder();
        builder.append("CamundaTaskEventResource [id=")
            .append(id)
            .append(", type=")
            .append(type)
            .append(", created=")
            .append(created)
            .append(", payload=")
            .append(payload)
            .append("]");
        return builder.toString();
    }

}
