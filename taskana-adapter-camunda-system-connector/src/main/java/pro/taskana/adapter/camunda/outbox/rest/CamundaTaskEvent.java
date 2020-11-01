package pro.taskana.adapter.camunda.outbox.rest;

import java.io.Serializable;

/** POJO that represents an event in the camunda outbox table. */
public class CamundaTaskEvent implements Serializable {

  private int id;
  private String type;
  private String created;
  private String payload;
  private int remainingRetries;
  private String blockedUntil;
  private String error;
  private String camundaTaskId;

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

  public int getRemainingRetries() {
    return remainingRetries;
  }

  public void setRemainingRetries(int remainingRetries) {
    this.remainingRetries = remainingRetries;
  }

  public String getBlockedUntil() {
    return blockedUntil;
  }

  public void setBlockedUntil(String blockedUntil) {
    this.blockedUntil = blockedUntil;
  }

  public String getError() {
    return error;
  }

  public void setError(String error) {
    this.error = error;
  }

  public String getCamundaTaskId() {
    return camundaTaskId;
  }

  public void setCamundaTaskId(String camundaTaskId) {
    this.camundaTaskId = camundaTaskId;
  }

  @Override
  public String toString() {
    return "CamundaTaskEvent [id="
        + id
        + ", type="
        + type
        + ", created="
        + created
        + ", payload="
        + payload
        + ", remainingRetries="
        + remainingRetries
        + ", blockedUntil="
        + blockedUntil
        + ", error="
        + error
        + ", camundaTaskId="
        + camundaTaskId
        + "]";
  }
}
