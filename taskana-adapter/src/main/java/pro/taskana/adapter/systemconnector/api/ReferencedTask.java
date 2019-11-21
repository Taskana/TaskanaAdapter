package pro.taskana.adapter.systemconnector.api;

/**
 * POJO that represents a task in the external system.
 *
 * @author kkl
 */

public class ReferencedTask {

    private String id;
    private String creationEventId;
    private String name;
    private String assignee;
    private String created;
    private String due;
    private String description;
    private String owner;
    private String priority;
    private String suspended;
    private String systemURL;
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

    public String getCreationEventId() {
        return creationEventId;
    }

    public void setCreationEventId(String creationEventId) {
        this.creationEventId = creationEventId;
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

    public String getSystemURL() {
        return systemURL;
    }

    public void setSystemURL(String systemURL) {
        this.systemURL = systemURL;
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
        StringBuilder builder = new StringBuilder();
        builder.append("ReferencedTask [id=");
        builder.append(id);
        builder.append(", creationEventId=");
        builder.append(creationEventId);
        builder.append(", name=");
        builder.append(name);
        builder.append(", assignee=");
        builder.append(assignee);
        builder.append(", created=");
        builder.append(created);
        builder.append(", due=");
        builder.append(due);
        builder.append(", description=");
        builder.append(description);
        builder.append(", owner=");
        builder.append(owner);
        builder.append(", priority=");
        builder.append(priority);
        builder.append(", suspended=");
        builder.append(suspended);
        builder.append(", systemURL=");
        builder.append(systemURL);
        builder.append(", taskDefinitionKey=");
        builder.append(taskDefinitionKey);
        builder.append(", variables=");
        builder.append(variables);
        builder.append(", domain=");
        builder.append(domain);
        builder.append(", classificationKey=");
        builder.append(classificationKey);
        builder.append(", workbasketKey=");
        builder.append(workbasketKey);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((assignee == null) ? 0 : assignee.hashCode());
        result = prime * result + ((classificationKey == null) ? 0 : classificationKey.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((creationEventId == null) ? 0 : creationEventId.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((domain == null) ? 0 : domain.hashCode());
        result = prime * result + ((due == null) ? 0 : due.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((priority == null) ? 0 : priority.hashCode());
        result = prime * result + ((suspended == null) ? 0 : suspended.hashCode());
        result = prime * result + ((systemURL == null) ? 0 : systemURL.hashCode());
        result = prime * result + ((taskDefinitionKey == null) ? 0 : taskDefinitionKey.hashCode());
        result = prime * result + ((variables == null) ? 0 : variables.hashCode());
        result = prime * result + ((workbasketKey == null) ? 0 : workbasketKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ReferencedTask other = (ReferencedTask) obj;
        if (assignee == null) {
            if (other.assignee != null)
                return false;
        } else if (!assignee.equals(other.assignee))
            return false;
        if (classificationKey == null) {
            if (other.classificationKey != null)
                return false;
        } else if (!classificationKey.equals(other.classificationKey))
            return false;
        if (created == null) {
            if (other.created != null)
                return false;
        } else if (!created.equals(other.created))
            return false;
        if (creationEventId == null) {
            if (other.creationEventId != null)
                return false;
        } else if (!creationEventId.equals(other.creationEventId))
            return false;
        if (description == null) {
            if (other.description != null)
                return false;
        } else if (!description.equals(other.description))
            return false;
        if (domain == null) {
            if (other.domain != null)
                return false;
        } else if (!domain.equals(other.domain))
            return false;
        if (due == null) {
            if (other.due != null)
                return false;
        } else if (!due.equals(other.due))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (owner == null) {
            if (other.owner != null)
                return false;
        } else if (!owner.equals(other.owner))
            return false;
        if (priority == null) {
            if (other.priority != null)
                return false;
        } else if (!priority.equals(other.priority))
            return false;
        if (suspended == null) {
            if (other.suspended != null)
                return false;
        } else if (!suspended.equals(other.suspended))
            return false;
        if (systemURL == null) {
            if (other.systemURL != null)
                return false;
        } else if (!systemURL.equals(other.systemURL))
            return false;
        if (taskDefinitionKey == null) {
            if (other.taskDefinitionKey != null)
                return false;
        } else if (!taskDefinitionKey.equals(other.taskDefinitionKey))
            return false;
        if (variables == null) {
            if (other.variables != null)
                return false;
        } else if (!variables.equals(other.variables))
            return false;
        if (workbasketKey == null) {
            if (other.workbasketKey != null)
                return false;
        } else if (!workbasketKey.equals(other.workbasketKey))
            return false;
        return true;
    }

}
