package pro.taskana.adapter.camunda.tasklistener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Camunda task listner that records task events in the outbox database table.
 *
 * @author jhe
 */
public class TaskanaTaskListener implements TaskListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskListener.class);

    private static final String SQL_INSERT_EVENT = "INSERT INTO event_store (TYPE,CREATED,PAYLOAD) VALUES (?,?,?)";

    private static TaskanaTaskListener instance = null;

    public static TaskanaTaskListener getInstance() {
        if (instance == null) {
            instance = new TaskanaTaskListener();
        }
        return instance;
    }

    @Override
    public void notify(DelegateTask delegateTask) {

        try (Connection connection = Context.getProcessEngineConfiguration().getDataSource().getConnection()) {

            switch (delegateTask.getEventName()) {

                case "create":
                    insertCreateEventIntoOutbox(delegateTask, connection);
                    break;
                case "complete":
                    insertCompleteOrDeleteEventIntoOutbox(delegateTask, connection);
                    break;
                case "delete":
                    insertCompleteOrDeleteEventIntoOutbox(delegateTask, connection);
                    break;
                default:
                    break;
            }

        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to process a delegate task", e);
        }

    }

    private void insertCreateEventIntoOutbox(DelegateTask delegateTask, Connection connection) throws SQLException {

        String camundaSchema = null;

        try {

            camundaSchema = connection.getSchema();
            LOGGER.debug("camundaSchema in taskListener is {}", camundaSchema);

            String referencedTaskJson = getReferencedTaskJson(delegateTask);

            setOutboxSchema(connection);

            prepareAndExecuteStatement(connection, delegateTask, referencedTaskJson);

        } catch (SQLException e) {
            LOGGER.warn("Caught {} while trying to insert a \"create\" event into the outbox table", e);

        } finally {
            if (camundaSchema != null) {
                connection.setSchema(camundaSchema);
            }
        }
    }

    private void insertCompleteOrDeleteEventIntoOutbox(DelegateTask delegateTask, Connection connection)
        throws SQLException {

        if (delegateTask.getEventName().equals("complete") && taskWasCompletedByTaskanaAdapter(delegateTask)) {
            return;
        }

        String camundaSchema = null;

        try {

            String taskIdJson = "{\"id\":\"" + delegateTask.getId() + "\"}";

            camundaSchema = connection.getSchema();
            setOutboxSchema(connection);
            prepareAndExecuteStatement(connection, delegateTask, taskIdJson);
            connection.setSchema(camundaSchema);

        } catch (Exception e) {
            LOGGER.warn(
                "Caught {} while trying to insert a " + delegateTask.getEventName() + " event into the outbox table",
                e);
        } finally {
            if (camundaSchema != null) {
                connection.setSchema(camundaSchema);
            }
        }
    }

    private boolean taskWasCompletedByTaskanaAdapter(DelegateTask delegateTask) {

        if (delegateTask.getVariableNamesLocal().contains("completedByTaskanaAdapter")) {
            return true;
        }
        return false;
    }

    private void setOutboxSchema(Connection connection) throws SQLException {

        String dbProductName = connection.getMetaData().getDatabaseProductName();
        if ("PostgreSQL".equals(dbProductName)) {
            connection.setSchema("taskana_tables");
        } else {
            connection.setSchema("TASKANA_TABLES");
        }
    }

    private void prepareAndExecuteStatement(Connection connection, DelegateTask delegateTask, String payloadJson) {

        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL_INSERT_EVENT,
            Statement.RETURN_GENERATED_KEYS)) {

            Timestamp eventCreationTimestamp = Timestamp.from(Instant.now());

            preparedStatement.setString(1, delegateTask.getEventName());
            preparedStatement.setTimestamp(2, eventCreationTimestamp);
            preparedStatement.setString(3, payloadJson);

            preparedStatement.execute();

        } catch (Exception e) {

            LOGGER.warn("Caught {} while trying to prepare and execute statement", e);
        }
    }

    private String getReferencedTaskJson(DelegateTask delegateTask) {

        StringBuilder referencedTaskJsonBuilder = new StringBuilder();

        referencedTaskJsonBuilder.append("{\"id\":\"")
            .append(delegateTask.getId())
            .append("\",\"created\":\"")
            .append(formatDate(delegateTask.getCreateTime()))
            .append("\",\"priority\":\"")
            .append(delegateTask.getPriority());
        if (delegateTask.getName() != null) {
            referencedTaskJsonBuilder.append("\",\"name\":\"")
                .append(delegateTask.getName().replace("\n", "\\n"));
        }
        if (delegateTask.getAssignee() != null) {
            referencedTaskJsonBuilder.append("\",\"assignee\":\"")
                .append(delegateTask.getAssignee().replace("\n", "\\n"));
        }
        if (delegateTask.getDueDate() != null) {
            referencedTaskJsonBuilder.append("\",\"due\":\"")
                .append(formatDate(delegateTask.getDueDate()));
        }
        if (delegateTask.getDescription() != null) {
            referencedTaskJsonBuilder.append("\",\"description\":\"")
                .append(delegateTask.getDescription().replace("\n", "\\n"));
        }
        if (delegateTask.getOwner() != null) {
            referencedTaskJsonBuilder.append("\",\"owner\":\"")
                .append(delegateTask.getOwner().replace("\n", "\\n"));
        }
        if (delegateTask.getTaskDefinitionKey() != null) {
            referencedTaskJsonBuilder.append("\",\"taskDefinitionKey\":\"")
                .append(delegateTask.getTaskDefinitionKey());
        }
        String classificationKey = getUserTaskExtensionProperty(delegateTask, "classification-key");
        if (classificationKey != null) {
            referencedTaskJsonBuilder.append("\",\"classificationKey\":\"")
                .append(classificationKey);
        }
        String domain = getProcessModelExtensionProperty(delegateTask, "domain");
        if (domain != null) {
            referencedTaskJsonBuilder.append("\",\"domain\":\"")
                .append(domain);
        }

        referencedTaskJsonBuilder.append("\",\"variables\":\"{")
            .append(getProcessVariables(delegateTask));

        referencedTaskJsonBuilder.append("}");

        return referencedTaskJsonBuilder.toString();
    }

    private String getProcessVariables(DelegateTask delegateTask) {

        ObjectMapper objectMapper = new ObjectMapper();
        JacksonConfigurator.configureObjectMapper(objectMapper);
        StringBuilder processVariablesBuilder = new StringBuilder();

        String processVariablesConcatenated = getProcessModelExtensionProperty(delegateTask, "taskana-attributes");

        if (processVariablesConcatenated != null) {
            List<String> processVariables = splitProcessVariablesString(processVariablesConcatenated);

            processVariables.forEach(
                processVariable -> addToProcessVariablesBuilder(delegateTask, objectMapper, processVariablesBuilder,
                    processVariable));

            //check if someone sets the taskana-attributes extension property, but enters no values
            if (processVariablesBuilder.length() > 0) {
                processVariablesBuilder.deleteCharAt(processVariablesBuilder.length() - 1).append("}\"");
            } else {
                return "}\"";
            }

        } else {
            return "}\"";
        }

        return processVariablesBuilder.toString();
    }

    private void addToProcessVariablesBuilder(DelegateTask delegateTask, ObjectMapper objectMapper,
        StringBuilder processVariablesBuilder, String processVariable2) {

        Object processVariable = delegateTask.getVariable(processVariable2);

        if (processVariable != null) {

            try {

                Map<String, Object> valueInfo = new HashMap<>();
                valueInfo.put("objectTypeName", processVariable.getClass());
                VariableValueDto variableValueDto = new VariableValueDto(processVariable.getClass().getSimpleName(),
                    objectMapper.writeValueAsString(processVariable), valueInfo);

                String processVariableValueJson = objectMapper.writeValueAsString(variableValueDto)
                    .replace("\"", "\\\"");
                processVariablesBuilder.append("\\\"")
                    .append(processVariable2)
                    .append("\\\":")
                    .append(processVariableValueJson)
                    .append(",");

            } catch (Exception ex) {
                LOGGER.warn("Caught {} while trying to create JSON-String out of process variable object", ex);
            }
        }
    }

    private List<String> splitProcessVariablesString(String processVariablesConcatenated) {
        List<String> processVariables = Arrays.asList(processVariablesConcatenated.trim().split("\\s*,\\s*"));
        return processVariables;
    }

    private String getProcessModelExtensionProperty(DelegateTask delegateTask, String propertyKey) {

        String propertyValue = null;

        BpmnModelInstance model = delegateTask.getExecution().getBpmnModelInstance();

        try {
            List<CamundaProperty> processModelExtensionProperties = model.getModelElementsByType(CamundaProperty.class)
                .stream()
                .filter(camundaProperty -> camundaProperty.getCamundaName()
                    .equals(propertyKey))
                .collect(Collectors.toList());

            if (processModelExtensionProperties.isEmpty()) {
                return propertyValue;
            } else {
                propertyValue = processModelExtensionProperties.get(0).getCamundaValue();
            }

        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to retrieve the " + propertyKey + " property from a process model", e);

        }

        return propertyValue;

    }

    private String getUserTaskExtensionProperty(DelegateTask delegateTask, String propertyKey) {

        String propertyValue = null;

        try {

            ExtensionElements extensionElements = delegateTask.getExecution()
                .getBpmnModelElementInstance()
                .getExtensionElements();

            if (extensionElements == null) {
                return propertyValue;
            } else {
                CamundaProperties camundaProperties = extensionElements.getElementsQuery()
                    .filterByType(CamundaProperties.class)
                    .singleResult();

                List<CamundaProperty> userTaskExtensionProperties = camundaProperties.getCamundaProperties()
                    .stream()
                    .filter(camundaProperty -> camundaProperty.getCamundaName()
                        .equals(propertyKey))
                    .collect(Collectors.toList());

                if (userTaskExtensionProperties.isEmpty()) {
                    return propertyValue;
                } else {
                    propertyValue = userTaskExtensionProperties.get(0).getCamundaValue();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to retrieve the " + propertyKey + " property of a user task", e);
        }

        return propertyValue;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        } else {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .withZone(ZoneId.systemDefault())
                .format(date.toInstant());

        }
    }
}
