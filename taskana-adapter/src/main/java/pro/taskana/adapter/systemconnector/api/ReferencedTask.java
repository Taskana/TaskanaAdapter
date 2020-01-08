package pro.taskana.adapter.systemconnector.api;

/**
 * POJO that represents a task in the external system.
 *
 * @author kkl
 */
public class ReferencedTask {

  private String id;
  private String outboxEventId;
  private String outboxEventType;
  private String name;
  private String assignee;
  private String created;
  private String due;
  private String description;
  private String owner;
  private String priority;
  private String suspended;
  private String systemUrl;
  private String taskDefinitionKey;
  private String variables;
  // extension properties
  private String domain;
  private String classificationKey;
  private String workbasketKey;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getOutboxEventId() {
    return outboxEventId;
  }

  public void setOutboxEventId(String outboxEventId) {
    this.outboxEventId = outboxEventId;
  }

  public String getOutboxEventType() {
    return outboxEventType;
  }

  public void setOutboxEventType(String outboxEventType) {
    this.outboxEventType = outboxEventType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAssignee() {
    return assignee;
  }

  public void setAssignee(String assignee) {
    this.assignee = assignee;
  }

  public String getCreated() {
    return created;
  }

  public void setCreated(String created) {
    this.created = created;
  }

  public String getDue() {
    return due;
  }

  public void setDue(String due) {
    this.due = due;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getOwner() {
    return owner;
  }

  public void setOwner(String owner) {
    this.owner = owner;
  }

  public String getPriority() {
    return priority;
  }

  public void setPriority(String priority) {
    this.priority = priority;
  }

  public String getSuspended() {
    return suspended;
  }

  public void setSuspended(String suspended) {
    this.suspended = suspended;
  }

  public String getSystemUrl() {
    return systemUrl;
  }

  public void setSystemUrl(String systemUrl) {
    this.systemUrl = systemUrl;
  }

  public String getTaskDefinitionKey() {
    return taskDefinitionKey;
  }

  public void setTaskDefinitionKey(String taskDefinitionKey) {
    this.taskDefinitionKey = taskDefinitionKey;
  }

  public String getVariables() {
    return variables;
  }

  public void setVariables(String variables) {
    this.variables = variables;
  }

  public String getDomain() {
    return domain;
  }

  public void setDomain(String domain) {
    this.domain = domain;
  }

  public String getClassificationKey() {
    return classificationKey;
  }

  public void setClassificationKey(String classificationKey) {
    this.classificationKey = classificationKey;
  }

  public String getWorkbasketKey() {
    return workbasketKey;
  }

  public void setWorkbasketKey(String workbasketKey) {
    this.workbasketKey = workbasketKey;
  }

  @Override
  public String toString() {
    return "ReferencedTask [id="
        + id
        + ", outboxEventId="
        + outboxEventId
        + ", outboxEventType="
        + outboxEventType
        + ", name="
        + name
        + ", assignee="
        + assignee
        + ", created="
        + created
        + ", due="
        + due
        + ", description="
        + description
        + ", owner="
        + owner
        + ", priority="
        + priority
        + ", suspended="
        + suspended
        + ", systemURL="
        + systemUrl
        + ", taskDefinitionKey="
        + taskDefinitionKey
        + ", variables="
        + variables
        + ", domain="
        + domain
        + ", classificationKey="
        + classificationKey
        + ", workbasketKey="
        + workbasketKey
        + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((assignee == null) ? 0 : assignee.hashCode());
    result = prime * result + ((classificationKey == null) ? 0 : classificationKey.hashCode());
    result = prime * result + ((created == null) ? 0 : created.hashCode());
    result = prime * result + ((description == null) ? 0 : description.hashCode());
    result = prime * result + ((domain == null) ? 0 : domain.hashCode());
    result = prime * result + ((due == null) ? 0 : due.hashCode());
    result = prime * result + ((id == null) ? 0 : id.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((outboxEventId == null) ? 0 : outboxEventId.hashCode());
    result = prime * result + ((outboxEventType == null) ? 0 : outboxEventType.hashCode());
    result = prime * result + ((owner == null) ? 0 : owner.hashCode());
    result = prime * result + ((priority == null) ? 0 : priority.hashCode());
    result = prime * result + ((suspended == null) ? 0 : suspended.hashCode());
    result = prime * result + ((systemUrl == null) ? 0 : systemUrl.hashCode());
    result = prime * result + ((taskDefinitionKey == null) ? 0 : taskDefinitionKey.hashCode());
    result = prime * result + ((variables == null) ? 0 : variables.hashCode());
    result = prime * result + ((workbasketKey == null) ? 0 : workbasketKey.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof ReferencedTask)) {
      return false;
    }
    ReferencedTask other = (ReferencedTask) obj;
    if (assignee == null) {
      if (other.assignee != null) {
        return false;
      }
    } else if (!assignee.equals(other.assignee)) {
      return false;
    }
    if (classificationKey == null) {
      if (other.classificationKey != null) {
        return false;
      }
    } else if (!classificationKey.equals(other.classificationKey)) {
      return false;
    }
    if (created == null) {
      if (other.created != null) {
        return false;
      }
    } else if (!created.equals(other.created)) {
      return false;
    }
    if (description == null) {
      if (other.description != null) {
        return false;
      }
    } else if (!description.equals(other.description)) {
      return false;
    }
    if (domain == null) {
      if (other.domain != null) {
        return false;
      }
    } else if (!domain.equals(other.domain)) {
      return false;
    }
    if (due == null) {
      if (other.due != null) {
        return false;
      }
    } else if (!due.equals(other.due)) {
      return false;
    }
    if (id == null) {
      if (other.id != null) {
        return false;
      }
    } else if (!id.equals(other.id)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (outboxEventId == null) {
      if (other.outboxEventId != null) {
        return false;
      }
    } else if (!outboxEventId.equals(other.outboxEventId)) {
      return false;
    }
    if (outboxEventType == null) {
      if (other.outboxEventType != null) {
        return false;
      }
    } else if (!outboxEventType.equals(other.outboxEventType)) {
      return false;
    }
    if (owner == null) {
      if (other.owner != null) {
        return false;
      }
    } else if (!owner.equals(other.owner)) {
      return false;
    }
    if (priority == null) {
      if (other.priority != null) {
        return false;
      }
    } else if (!priority.equals(other.priority)) {
      return false;
    }
    if (suspended == null) {
      if (other.suspended != null) {
        return false;
      }
    } else if (!suspended.equals(other.suspended)) {
      return false;
    }
    if (systemUrl == null) {
      if (other.systemUrl != null) {
        return false;
      }
    } else if (!systemUrl.equals(other.systemUrl)) {
      return false;
    }
    if (taskDefinitionKey == null) {
      if (other.taskDefinitionKey != null) {
        return false;
      }
    } else if (!taskDefinitionKey.equals(other.taskDefinitionKey)) {
      return false;
    }
    if (variables == null) {
      if (other.variables != null) {
        return false;
      }
    } else if (!variables.equals(other.variables)) {
      return false;
    }
    if (workbasketKey == null) {
      if (other.workbasketKey != null) {
        return false;
      }
    } else if (!workbasketKey.equals(other.workbasketKey)) {
      return false;
    }
    return true;
  }
}
