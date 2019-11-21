package pro.taskana.adapter.camunda.tasklistener;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Date;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            }

        } catch (SQLException e) {
            LOGGER.warn("Caught {} while trying to process a delegate task", e);
        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to process a delegate task", e);
        }

    }

    private void insertCreateEventIntoOutbox(DelegateTask delegateTask, Connection connection) throws SQLException {

        String camundaSchema = null;

        try {

            camundaSchema = connection.getSchema();
            LOGGER.debug("camundaSchema in taskListener is {}", camundaSchema);

            setOutboxSchema (connection);

            String referencedTaskJson = getReferencedTaskJson(delegateTask);

            prepareAndExecuteStatement(connection, delegateTask, referencedTaskJson);
            
        } catch (SQLException e) {
            LOGGER.warn("Caught {} while trying to insert a \"create\" event into the outbox table", e);

        } finally {
            if (camundaSchema!=null){
                connection.setSchema(camundaSchema);
            }
        }
    }

    private void insertCompleteOrDeleteEventIntoOutbox(DelegateTask delegateTask, Connection connection) throws SQLException {

        if (delegateTask.getEventName().equals("complete") && taskWasCompletedByAdapter(delegateTask)) {
            return;
        } else {

            String camundaSchema = null;

            try {

                camundaSchema = connection.getSchema();
                setOutboxSchema(connection);

                String taskIdJson = "{\"id\":\"" + delegateTask.getId() + "\"}";
                prepareAndExecuteStatement(connection, delegateTask, taskIdJson);

                connection.setSchema(camundaSchema);

            } catch (SQLException e) {
                LOGGER.warn("Caught {} while trying to insert a " + delegateTask.getEventName() + " event into the outbox table", e);
            } catch (Exception e) {
                LOGGER.warn("Caught {} while trying to insert a " + delegateTask.getEventName() + " event into the outbox table", e);
            }
            finally {
                if (camundaSchema != null) {
                    connection.setSchema(camundaSchema);
                }
            }
        }

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

        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL_INSERT_EVENT, Statement.RETURN_GENERATED_KEYS)) {

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

        referencedTaskJsonBuilder.append("{\"id\":\"").append(delegateTask.getId())
          .append("\",\"name\":\"").append(delegateTask.getName())
          .append("\",\"assignee\":\"").append(delegateTask.getAssignee())
          .append("\",\"created\":\"").append(formatDate(delegateTask.getCreateTime()))
          .append("\",\"due\":\"").append(formatDate(delegateTask.getDueDate()))
          .append("\",\"description\":\"").append(delegateTask.getDescription())
          .append("\",\"owner\":\"").append(delegateTask.getOwner())
          .append("\",\"priority\":\"").append(delegateTask.getPriority())
          .append("\",\"taskDefinitionKey\":\"").append(delegateTask.getTaskDefinitionKey())
          .append("\",\"classification\":\"").append(getUserTaskExtensionProperty(delegateTask, "classification"))
          .append("\",\"domain\":\"").append(getProcessModelExtensionProperty(delegateTask, "domain"))
          .append("\"}");

        return referencedTaskJsonBuilder.toString();
    }


    private String getProcessModelExtensionProperty(DelegateTask delegateTask, String propertyKey) {

        String propertyValue = null;

        BpmnModelInstance model = delegateTask.getExecution().getBpmnModelInstance();

        try {
            List<CamundaProperty> processModelExtensionProperties =
                    model.getModelElementsByType(CamundaProperty.class)
                            .stream()
                            .filter(camundaProperty ->
                                    camundaProperty.getCamundaName()
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

            if (extensionElements ==  null){
                return propertyValue;
            } else {
                CamundaProperties camundaProperties = extensionElements.getElementsQuery()
                        .filterByType(CamundaProperties.class).singleResult();

                List<CamundaProperty> userTaskExtensionProperties = camundaProperties.getCamundaProperties()
                        .stream()
                        .filter(camundaProperty ->
                                camundaProperty.getCamundaName()
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

    private boolean taskWasCompletedByAdapter(DelegateTask delegateTask) {

        String taskWasCompletedByAdapterValue = getUserTaskExtensionProperty(delegateTask, "taskWasCompletedByAdapter");

        if (taskWasCompletedByAdapterValue != null && taskWasCompletedByAdapterValue.equals("true")) {
            return true;
        }

        return false;
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