package io.kadai.adapter.camunda.outbox.rest.resource;

import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEvent;

/** EntityRessource class for {@link CamundaTaskEvent}. */
public class CamundaTaskEventResource {

  private int id;
  private String type;
  private String created;
  private String payload;
  private int remainingRetries;
  private String blockedUntil;
  private String error;
  private String camundaTaskId;
  private String lockExpiresAt;

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

  public String getLockExpiresAt() {
    return lockExpiresAt;
  }

  public void setLockExpiresAt(String lockExpiresAt) {
    this.lockExpiresAt = lockExpiresAt;
  }
}
