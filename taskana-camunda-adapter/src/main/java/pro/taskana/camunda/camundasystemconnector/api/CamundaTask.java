package pro.taskana.camunda.camundasystemconnector.api;

/**
 * POJO that represents a Camunda task.
 *
 * @author kkl
 */
public class CamundaTask {

    private String id;
    private String name;
    private String assignee;
    private String created;
    private String due;
    private String followUp;
    private String delegationState;
    private String description;
    private String executionId;
    private String owner;
    private String parentTaskId;
    private String priority;
    private String processDefinitionId;
    private String processInstanceId;
    private String taskDefinitionKey;
    private String caseExecutionId;
    private String caseInstanceId;
    private String caseDefinitionId;
    private String suspended;
    private String formKey;
    private String tenantId;
    private String camundaSystemURL;
    private String inputVariables;
    private String outputVariables;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getFollowUp() {
        return followUp;
    }

    public void setFollowUp(String followUp) {
        this.followUp = followUp;
    }

    public String getDelegationState() {
        return delegationState;
    }

    public void setDelegationState(String delegationState) {
        this.delegationState = delegationState;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(String parentTaskId) {
        this.parentTaskId = parentTaskId;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public String getCaseExecutionId() {
        return caseExecutionId;
    }

    public void setCaseExecutionId(String caseExecutionId) {
        this.caseExecutionId = caseExecutionId;
    }

    public String getCaseInstanceId() {
        return caseInstanceId;
    }

    public void setCaseInstanceId(String caseInstanceId) {
        this.caseInstanceId = caseInstanceId;
    }

    public String getCaseDefinitionId() {
        return caseDefinitionId;
    }

    public void setCaseDefinitionId(String caseDefinitionId) {
        this.caseDefinitionId = caseDefinitionId;
    }

    public String getSuspended() {
        return suspended;
    }

    public void setSuspended(String suspended) {
        this.suspended = suspended;
    }

    public String getFormKey() {
        return formKey;
    }

    public void setFormKey(String formKey) {
        this.formKey = formKey;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCamundaSystemURL() {
        return camundaSystemURL;
    }

    public void setCamundaSystemURL(String camundaSystemName) {
        this.camundaSystemURL = camundaSystemName;
    }

    public String getInputVariables() {
        return inputVariables;
    }

    public void setInputVariables(String inputVariables) {
        this.inputVariables = inputVariables;
    }

    public String getOutputVariables() {
        return outputVariables;
    }

    public void setOutputVariables(String outputVariables) {
        this.outputVariables = outputVariables;
    }

    @Override
    public String toString() {
        return "CamundaTask [id=" + id + ", name=" + name + ", assignee=" + assignee + ", created=" + created + ", due="
            + due + ", followUp=" + followUp + ", delegationState=" + delegationState + ", description=" + description
            + ", executionId=" + executionId + ", owner=" + owner + ", parentTaskId=" + parentTaskId + ", priority="
            + priority + ", processDefinitionId=" + processDefinitionId + ", processInstanceId=" + processInstanceId
            + ", taskDefinitionKey=" + taskDefinitionKey + ", caseExecutionId=" + caseExecutionId + ", caseInstanceId="
            + caseInstanceId + ", caseDefinitionId=" + caseDefinitionId + ", suspended=" + suspended + ", formKey="
            + formKey + ", tenantId=" + tenantId + ", camundaSystemURL=" + camundaSystemURL + ", inputVariables="
            + inputVariables + ", outputVariables=" + outputVariables + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((assignee == null) ? 0 : assignee.hashCode());
        result = prime * result + ((camundaSystemURL == null) ? 0 : camundaSystemURL.hashCode());
        result = prime * result + ((caseDefinitionId == null) ? 0 : caseDefinitionId.hashCode());
        result = prime * result + ((caseExecutionId == null) ? 0 : caseExecutionId.hashCode());
        result = prime * result + ((caseInstanceId == null) ? 0 : caseInstanceId.hashCode());
        result = prime * result + ((created == null) ? 0 : created.hashCode());
        result = prime * result + ((delegationState == null) ? 0 : delegationState.hashCode());
        result = prime * result + ((description == null) ? 0 : description.hashCode());
        result = prime * result + ((due == null) ? 0 : due.hashCode());
        result = prime * result + ((executionId == null) ? 0 : executionId.hashCode());
        result = prime * result + ((followUp == null) ? 0 : followUp.hashCode());
        result = prime * result + ((formKey == null) ? 0 : formKey.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((inputVariables == null) ? 0 : inputVariables.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((outputVariables == null) ? 0 : outputVariables.hashCode());
        result = prime * result + ((owner == null) ? 0 : owner.hashCode());
        result = prime * result + ((parentTaskId == null) ? 0 : parentTaskId.hashCode());
        result = prime * result + ((priority == null) ? 0 : priority.hashCode());
        result = prime * result + ((processDefinitionId == null) ? 0 : processDefinitionId.hashCode());
        result = prime * result + ((processInstanceId == null) ? 0 : processInstanceId.hashCode());
        result = prime * result + ((suspended == null) ? 0 : suspended.hashCode());
        result = prime * result + ((taskDefinitionKey == null) ? 0 : taskDefinitionKey.hashCode());
        result = prime * result + ((tenantId == null) ? 0 : tenantId.hashCode());
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
        if (getClass() != obj.getClass()) {
            return false;
        }
        CamundaTask other = (CamundaTask) obj;
        if (assignee == null) {
            if (other.assignee != null) {
                return false;
            }
        } else if (!assignee.equals(other.assignee)) {
            return false;
        }
        if (camundaSystemURL == null) {
            if (other.camundaSystemURL != null) {
                return false;
            }
        } else if (!camundaSystemURL.equals(other.camundaSystemURL)) {
            return false;
        }
        if (caseDefinitionId == null) {
            if (other.caseDefinitionId != null) {
                return false;
            }
        } else if (!caseDefinitionId.equals(other.caseDefinitionId)) {
            return false;
        }
        if (caseExecutionId == null) {
            if (other.caseExecutionId != null) {
                return false;
            }
        } else if (!caseExecutionId.equals(other.caseExecutionId)) {
            return false;
        }
        if (caseInstanceId == null) {
            if (other.caseInstanceId != null) {
                return false;
            }
        } else if (!caseInstanceId.equals(other.caseInstanceId)) {
            return false;
        }
        if (created == null) {
            if (other.created != null) {
                return false;
            }
        } else if (!created.equals(other.created)) {
            return false;
        }
        if (delegationState == null) {
            if (other.delegationState != null) {
                return false;
            }
        } else if (!delegationState.equals(other.delegationState)) {
            return false;
        }
        if (description == null) {
            if (other.description != null) {
                return false;
            }
        } else if (!description.equals(other.description)) {
            return false;
        }
        if (due == null) {
            if (other.due != null) {
                return false;
            }
        } else if (!due.equals(other.due)) {
            return false;
        }
        if (executionId == null) {
            if (other.executionId != null) {
                return false;
            }
        } else if (!executionId.equals(other.executionId)) {
            return false;
        }
        if (followUp == null) {
            if (other.followUp != null) {
                return false;
            }
        } else if (!followUp.equals(other.followUp)) {
            return false;
        }
        if (formKey == null) {
            if (other.formKey != null) {
                return false;
            }
        } else if (!formKey.equals(other.formKey)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (inputVariables == null) {
            if (other.inputVariables != null) {
                return false;
            }
        } else if (!inputVariables.equals(other.inputVariables)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (outputVariables == null) {
            if (other.outputVariables != null) {
                return false;
            }
        } else if (!outputVariables.equals(other.outputVariables)) {
            return false;
        }
        if (owner == null) {
            if (other.owner != null) {
                return false;
            }
        } else if (!owner.equals(other.owner)) {
            return false;
        }
        if (parentTaskId == null) {
            if (other.parentTaskId != null) {
                return false;
            }
        } else if (!parentTaskId.equals(other.parentTaskId)) {
            return false;
        }
        if (priority == null) {
            if (other.priority != null) {
                return false;
            }
        } else if (!priority.equals(other.priority)) {
            return false;
        }
        if (processDefinitionId == null) {
            if (other.processDefinitionId != null) {
                return false;
            }
        } else if (!processDefinitionId.equals(other.processDefinitionId)) {
            return false;
        }
        if (processInstanceId == null) {
            if (other.processInstanceId != null) {
                return false;
            }
        } else if (!processInstanceId.equals(other.processInstanceId)) {
            return false;
        }
        if (suspended == null) {
            if (other.suspended != null) {
                return false;
            }
        } else if (!suspended.equals(other.suspended)) {
            return false;
        }
        if (taskDefinitionKey == null) {
            if (other.taskDefinitionKey != null) {
                return false;
            }
        } else if (!taskDefinitionKey.equals(other.taskDefinitionKey)) {
            return false;
        }
        if (tenantId == null) {
            if (other.tenantId != null) {
                return false;
            }
        } else if (!tenantId.equals(other.tenantId)) {
            return false;
        }
        return true;
    }


}
