package pro.taskana.adapter.camunda.tasklistener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.util.ClassUtils;

import pro.taskana.adapter.camunda.dto.ReferencedTask;
import pro.taskana.adapter.camunda.dto.VariableValueDto;
import pro.taskana.adapter.camunda.mapper.JacksonConfigurator;

/**
 * This class is responsible for dealing with events within the lifecycle of a camunda user task.
 */
public class TaskanaTaskListener implements TaskListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskListener.class);

  private static final String SQL_INSERT_EVENT =
      "INSERT INTO event_store (TYPE,CREATED,PAYLOAD) VALUES (?,?,?)";

  private static TaskanaTaskListener instance = null;

  private boolean gotActivated = false;

  private ObjectMapper objectMapper = JacksonConfigurator.createAndConfigureObjectMapper();

  public static TaskanaTaskListener getInstance() {
    if (instance == null) {
      instance = new TaskanaTaskListener();
    }
    return instance;
  }

  @Override
  public void notify(DelegateTask delegateTask) {

    try (Connection connection =
        Context.getProcessEngineConfiguration().getDataSource().getConnection()) {

      if (!gotActivated) {
        gotActivated = true;
        LOGGER.info(
            "TaskanaTaskListener activated successfully, connected to "
                + connection.getMetaData().getURL());
      }

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

  private void insertCreateEventIntoOutbox(DelegateTask delegateTask, Connection connection)
      throws SQLException {

    String camundaSchema = null;

    try {

      camundaSchema = connection.getSchema();
      LOGGER.debug("camundaSchema in taskListener is {}", camundaSchema);

      String referencedTaskJson = getReferencedTaskJson(delegateTask);

      setOutboxSchema(connection);

      prepareAndExecuteStatement(connection, delegateTask, referencedTaskJson);

    } catch (JsonProcessingException e) {

      LOGGER.warn("Caught {} while trying to convert ReferencedTask to JSON-String");
    } catch (Exception e) {
      LOGGER.warn("Caught {} while trying to insert a \"create\" event into the outbox table", e);

    } finally {
      if (camundaSchema != null) {
        connection.setSchema(camundaSchema);
      }
    }
  }

  private void insertCompleteOrDeleteEventIntoOutbox(
      DelegateTask delegateTask, Connection connection) throws SQLException {

    if (delegateTask.getEventName().equals("complete")
        && taskWasCompletedByTaskanaAdapter(delegateTask)) {
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
          "Caught {} while trying to insert a "
              + delegateTask.getEventName()
              + " event into the outbox table",
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

  private void prepareAndExecuteStatement(
      Connection connection, DelegateTask delegateTask, String payloadJson) {

    try (PreparedStatement preparedStatement =
        connection.prepareStatement(SQL_INSERT_EVENT, Statement.RETURN_GENERATED_KEYS)) {

      Timestamp eventCreationTimestamp = Timestamp.from(Instant.now());

      preparedStatement.setString(1, delegateTask.getEventName());
      preparedStatement.setTimestamp(2, eventCreationTimestamp);
      preparedStatement.setString(3, payloadJson);

      preparedStatement.execute();

    } catch (Exception e) {

      LOGGER.warn("Caught {} while trying to prepare and execute statement", e);
    }
  }

  private String getReferencedTaskJson(DelegateTask delegateTask) throws JsonProcessingException {

    ReferencedTask referencedTask = new ReferencedTask();

    referencedTask.setId(delegateTask.getId());
    referencedTask.setCreated(formatDate(delegateTask.getCreateTime()));
    referencedTask.setPriority(String.valueOf(delegateTask.getPriority()));
    referencedTask.setName(delegateTask.getName());
    referencedTask.setAssignee(delegateTask.getAssignee());
    referencedTask.setDue(formatDate(delegateTask.getDueDate()));
    referencedTask.setDescription(delegateTask.getDescription());
    referencedTask.setOwner(delegateTask.getOwner());
    referencedTask.setTaskDefinitionKey(delegateTask.getTaskDefinitionKey());
    referencedTask.setClassificationKey(
        getUserTaskExtensionProperty(delegateTask, "taskana.classification-key"));
    referencedTask.setDomain(getProcessModelExtensionProperty(delegateTask, "taskana.domain"));
    referencedTask.setWorkbasketKey(getWorkbasketKey(delegateTask));
    referencedTask.setVariables(getProcessVariables(delegateTask));
    String referencedTaskJson = objectMapper.writeValueAsString(referencedTask);
    LOGGER.debug("Exit from getReferencedTaskJson. Returning {}.", referencedTaskJson);
    return referencedTaskJson;
  }

  private String getProcessVariables(DelegateTask delegateTask) {

    StringBuilder processVariablesBuilder = new StringBuilder();

    String processVariablesConcatenated =
        getProcessModelExtensionProperty(delegateTask, "taskana-attributes");

    if (processVariablesConcatenated != null) {
      List<String> processVariablenames =
          splitProcessVariableNamesString(processVariablesConcatenated);

      processVariablenames.forEach(
          nameOfProcessVariableToAdd ->
              addToProcessVariablesBuilder(
                  delegateTask, objectMapper, processVariablesBuilder, nameOfProcessVariableToAdd));

      // check if someone sets the taskana-attributes extension property, but enters no values
      if (processVariablesBuilder.length() > 0) {
        processVariablesBuilder.deleteCharAt(processVariablesBuilder.length() - 1).append("}");
        processVariablesBuilder.insert(0, "{");
      } else {
        return "{}";
      }

    } else {
      return "{}";
    }

    return processVariablesBuilder.toString();
  }

  private void addToProcessVariablesBuilder(
      DelegateTask delegateTask,
      ObjectMapper objectMapper,
      StringBuilder processVariablesBuilder,
      String nameOfprocessVariableToAdd) {

    Object processVariable = delegateTask.getVariable(nameOfprocessVariableToAdd);

    if (processVariable != null) {

      try {

        VariableValueDto variableValueDto =
            determineProcessVariableTypeAndCreateVariableValueDto(processVariable, objectMapper);

        String variableValueDtoJson = objectMapper.writeValueAsString(variableValueDto);

        processVariablesBuilder
            .append("\"")
            .append(nameOfprocessVariableToAdd)
            .append("\":")
            .append(variableValueDtoJson)
            .append(",");

      } catch (Exception ex) {
        LOGGER.warn(
            "Caught {} while trying to create JSON-String out of process variable object", ex);
      }
    }
  }

  private VariableValueDto determineProcessVariableTypeAndCreateVariableValueDto(
      Object processVariable, ObjectMapper objectMapper) throws JsonProcessingException {

    VariableValueDto variableValueDto = new VariableValueDto();

    String type = "Object";

    Map<String, Object> valueInfo = new HashMap<>();
    valueInfo.put("objectTypeName", processVariable.getClass());

    if (ClassUtils.isPrimitiveOrWrapper(processVariable.getClass())
        && !processVariable.getClass().getTypeName().equals("String")) {

      type = processVariable.getClass().getSimpleName();

      variableValueDto.setType(type);
      variableValueDto.setValue(processVariable);
      variableValueDto.setValueInfo(valueInfo);

    } else {

      String processVariableJsonString = objectMapper.writeValueAsString(processVariable);
      valueInfo.put("serializationDataFormat", "application/json");
      variableValueDto.setType(type);
      variableValueDto.setValue(processVariableJsonString);
      variableValueDto.setValueInfo(valueInfo);
    }

    return variableValueDto;
  }

  private List<String> splitProcessVariableNamesString(String processVariableNamesConcatenated) {
    List<String> processVariableNames =
        Arrays.asList(processVariableNamesConcatenated.trim().split("\\s*,\\s*"));
    return processVariableNames;
  }

  private String getWorkbasketKey(DelegateTask delegateTask) {
    String workbasketKey = null;
    try {
      Object workbasketKeyObj = delegateTask.getVariable("taskana.workbasket-key");
      if (workbasketKeyObj != null && workbasketKeyObj instanceof String) {
        workbasketKey = (String) workbasketKeyObj;
      }
    } catch (Exception e) {
      LOGGER.warn("Caught {} while trying to retrieve taskana.workbasket-key", e);
    }
    return workbasketKey;
  }

  private String getProcessModelExtensionProperty(DelegateTask delegateTask, String propertyKey) {

    String propertyValue = null;

    BpmnModelInstance model = delegateTask.getExecution().getBpmnModelInstance();

    try {
      List<CamundaProperty> processModelExtensionProperties =
          model.getModelElementsByType(CamundaProperty.class).stream()
              .filter(camundaProperty -> camundaProperty.getCamundaName().equals(propertyKey))
              .collect(Collectors.toList());

      if (processModelExtensionProperties.isEmpty()) {
        return propertyValue;
      } else {
        propertyValue = processModelExtensionProperties.get(0).getCamundaValue();
      }

    } catch (Exception e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve the "
              + propertyKey
              + " property from a process model",
          e);
    }

    return propertyValue;
  }

  private String getUserTaskExtensionProperty(DelegateTask delegateTask, String propertyKey) {

    String propertyValue = null;

    try {

      ExtensionElements extensionElements =
          delegateTask.getExecution().getBpmnModelElementInstance().getExtensionElements();

      if (extensionElements == null) {
        return propertyValue;
      } else {
        CamundaProperties camundaProperties =
            extensionElements
                .getElementsQuery()
                .filterByType(CamundaProperties.class)
                .singleResult();

        List<CamundaProperty> userTaskExtensionProperties =
            camundaProperties.getCamundaProperties().stream()
                .filter(camundaProperty -> camundaProperty.getCamundaName().equals(propertyKey))
                .collect(Collectors.toList());

        if (userTaskExtensionProperties.isEmpty()) {
          return propertyValue;
        } else {
          propertyValue = userTaskExtensionProperties.get(0).getCamundaValue();
        }
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Caught {} while trying to retrieve the " + propertyKey + " property of a user task", e);
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
