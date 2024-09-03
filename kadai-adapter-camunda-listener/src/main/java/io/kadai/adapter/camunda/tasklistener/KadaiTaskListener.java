package io.kadai.adapter.camunda.tasklistener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.kadai.adapter.camunda.CamundaListenerConfiguration;
import io.kadai.adapter.camunda.dto.ReferencedTask;
import io.kadai.adapter.camunda.dto.VariableValueDto;
import io.kadai.adapter.camunda.exceptions.SystemException;
import io.kadai.adapter.camunda.mapper.JacksonConfigurator;
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
import org.camunda.bpm.engine.variable.value.TypedValue;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for dealing with events within the lifecycle of a camunda user task.
 */
public class KadaiTaskListener implements TaskListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(KadaiTaskListener.class);
  private static final String TASK_STATE_COMPLETED = "COMPLETED";
  private static final String TASK_STATE_CANCELLED = "CANCELLED";
  private static final String TASK_STATE_TERMINATED = "TERMINATED";
  private static final String DEFAULT_SCHEMA = "kadai_tables";
  private static final String SQL_INSERT_EVENT =
      "INSERT INTO event_store (TYPE,CREATED,PAYLOAD,REMAINING_RETRIES,"
          + "BLOCKED_UNTIL,CAMUNDA_TASK_ID, SYSTEM_ENGINE_IDENTIFIER) VALUES (?,?,?,?,?,?,?)";
  private static KadaiTaskListener instance = null;

  private final ObjectMapper objectMapper = JacksonConfigurator.createAndConfigureObjectMapper();
  private boolean gotActivated = false;
  private String outboxSchemaName = null;

  public static KadaiTaskListener getInstance() {
    if (instance == null) {
      instance = new KadaiTaskListener();
    }
    return instance;
  }

  @Override
  public void notify(DelegateTask delegateTask) {

    try (Connection connection =
        Context.getProcessEngineConfiguration().getDataSource().getConnection()) {

      if (!gotActivated) {
        gotActivated = true;
        if (LOGGER.isInfoEnabled()) {
          LOGGER.info(
              String.format(
                  "KadaiTaskListener activated successfully, connected to %s",
                  connection.getMetaData().getURL()));
        }
      }

      String engineName = delegateTask.getProcessEngine().getName();
      if (engineName.length() > 128) {
        throw new SystemException(
            "The configured process engine name "
                + engineName
                + " is too long. "
                + "Length must not exceed 128 characters.");
      }

      switch (delegateTask.getEventName()) {
        case "create":
          insertCreateEventIntoOutbox(delegateTask, connection);
          break;
        case "complete":
        case "delete":
          insertCompleteOrDeleteEventIntoOutbox(delegateTask, connection);
          break;
        default:
          break;
      }

    } catch (Exception e) {
      LOGGER.error("Unexpected Exception while trying to process a delegate task", e);
      throw new SystemException("Unexpected Exception while trying to process a delegate task", e);
    }
  }

  private void insertCreateEventIntoOutbox(DelegateTask delegateTask, Connection connection)
      throws Exception {

    String camundaSchema = null;

    try {

      camundaSchema = connection.getSchema();
      LOGGER.debug("camundaSchema in taskListener is {}", camundaSchema);

      String referencedTaskJson = getReferencedTaskJson(delegateTask);

      setOutboxSchema(connection);

      prepareAndExecuteStatement(connection, delegateTask, referencedTaskJson);

    } finally {
      if (camundaSchema != null) {
        connection.setSchema(camundaSchema);
      }
    }
  }

  private void insertCompleteOrDeleteEventIntoOutbox(
      DelegateTask delegateTask, Connection connection) throws Exception {

    if (delegateTask.getEventName().equals("complete")
        && taskWasCompletedByKadaiAdapter(delegateTask)) {
      return;
    }

    String camundaSchema = null;

    try {
      String taskState = TASK_STATE_COMPLETED;
      if (delegateTask.getEventName().equals("delete")) {
        if (delegateTask.getExecution().isCanceled()) {
          taskState = TASK_STATE_CANCELLED;
        } else {
          taskState = TASK_STATE_TERMINATED;
        }
      }

      String payload =
          String.format("{\"id\":\"%s\",\"taskState\":\"%s\"}", delegateTask.getId(), taskState);

      camundaSchema = connection.getSchema();
      setOutboxSchema(connection);
      prepareAndExecuteStatement(connection, delegateTask, payload);
      connection.setSchema(camundaSchema);
    } finally {
      if (camundaSchema != null) {
        connection.setSchema(camundaSchema);
      }
    }
  }

  private boolean taskWasCompletedByKadaiAdapter(DelegateTask delegateTask) {

    return delegateTask.getVariableNamesLocal().contains("completedByKadaiAdapter");
  }

  private void setOutboxSchema(Connection connection) throws SQLException {

    if (outboxSchemaName == null) {
      outboxSchemaName = CamundaListenerConfiguration.getOutboxSchema();
    }

    outboxSchemaName =
        (outboxSchemaName == null || outboxSchemaName.isEmpty())
            ? DEFAULT_SCHEMA
            : outboxSchemaName;

    String dbProductName = connection.getMetaData().getDatabaseProductName();
    if ("PostgreSQL".equals(dbProductName)) {
      connection.setSchema(outboxSchemaName.toLowerCase());
    } else {
      connection.setSchema(outboxSchemaName.toUpperCase());
    }
  }

  private void prepareAndExecuteStatement(
      Connection connection, DelegateTask delegateTask, String payloadJson) throws Exception {

    try (PreparedStatement preparedStatement =
        connection.prepareStatement(SQL_INSERT_EVENT, Statement.RETURN_GENERATED_KEYS)) {

      Timestamp eventCreationTimestamp = Timestamp.from(Instant.now());

      int initialRetries = CamundaListenerConfiguration.getInitialNumberOfTaskCreationRetries();

      preparedStatement.setString(1, delegateTask.getEventName());
      preparedStatement.setTimestamp(2, eventCreationTimestamp);
      preparedStatement.setString(3, payloadJson);
      preparedStatement.setInt(4, initialRetries);
      preparedStatement.setTimestamp(5, eventCreationTimestamp);
      preparedStatement.setString(6, delegateTask.getId());
      preparedStatement.setString(7, delegateTask.getProcessEngine().getName());

      preparedStatement.execute();
    }
  }

  private String getReferencedTaskJson(DelegateTask delegateTask) throws JsonProcessingException {

    ReferencedTask referencedTask = new ReferencedTask();

    referencedTask.setId(delegateTask.getId());
    referencedTask.setCreated(formatDate(delegateTask.getCreateTime()));
    referencedTask.setPriority(String.valueOf(delegateTask.getPriority()));
    referencedTask.setName(delegateTask.getName());
    referencedTask.setAssignee(delegateTask.getAssignee());
    referencedTask.setPlanned(formatDate(delegateTask.getFollowUpDate()));
    referencedTask.setDue(formatDate(delegateTask.getDueDate()));
    referencedTask.setDescription(delegateTask.getDescription());
    referencedTask.setOwner(delegateTask.getOwner());
    referencedTask.setTaskDefinitionKey(delegateTask.getTaskDefinitionKey());
    referencedTask.setBusinessProcessId(delegateTask.getProcessInstanceId());
    referencedTask.setClassificationKey(
        getUserTaskExtensionProperty(delegateTask, "kadai.classification-key"));
    referencedTask.setDomain(getDomainVariable(delegateTask));
    referencedTask.setWorkbasketKey(getVariable(delegateTask, "kadai.workbasket-key", null));
    referencedTask.setManualPriority(getVariable(delegateTask, "kadai.manual-priority", "-1"));
    referencedTask.setCustomInt1(getVariable(delegateTask, "kadai.custom-int-1", null));
    referencedTask.setCustomInt2(getVariable(delegateTask, "kadai.custom-int-2", null));
    referencedTask.setCustomInt3(getVariable(delegateTask, "kadai.custom-int-3", null));
    referencedTask.setCustomInt4(getVariable(delegateTask, "kadai.custom-int-4", null));
    referencedTask.setCustomInt5(getVariable(delegateTask, "kadai.custom-int-5", null));
    referencedTask.setCustomInt6(getVariable(delegateTask, "kadai.custom-int-6", null));
    referencedTask.setCustomInt7(getVariable(delegateTask, "kadai.custom-int-7", null));
    referencedTask.setCustomInt8(getVariable(delegateTask, "kadai.custom-int-8", null));
    referencedTask.setVariables(getProcessVariables(delegateTask));
    String referencedTaskJson = objectMapper.writeValueAsString(referencedTask);
    LOGGER.debug("Exit from getReferencedTaskJson. Returning {}.", referencedTaskJson);
    return referencedTaskJson;
  }

  private String getDomainVariable(DelegateTask delegateTask) {
    String taskDomain = getVariable(delegateTask, "kadai.domain", null);
    if (taskDomain != null) {
      return taskDomain;
    }
    taskDomain = getUserTaskExtensionProperty(delegateTask, "kadai.domain");
    if (taskDomain != null) {
      return taskDomain;
    }
    return getProcessModelExtensionProperty(delegateTask, "kadai.domain");
  }

  private String getVariable(
      DelegateTask delegateTask, String variableReference, String defaultValue) {
    String variable = defaultValue;
    try {
      Object variableObj = delegateTask.getVariable(variableReference);
      if (variableObj instanceof String) {
        variable = (String) variableObj;
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Caught exception while trying to retrieve {} " + "for task {} in ProcessDefinition {}",
          variableReference,
          delegateTask.getName(),
          delegateTask.getProcessDefinitionId(),
          e);
    }
    return variable;
  }

  private String getProcessVariables(DelegateTask delegateTask) {

    StringBuilder variablesBuilder = new StringBuilder();
    List<String> variableNames;

    // Get Task Variables
    String taskVariablesConcatenated =
        getUserTaskExtensionProperty(delegateTask, "kadai-attributes");

    if (taskVariablesConcatenated != null) {
      variableNames = splitVariableNamesString(taskVariablesConcatenated);

    } else {
      String processVariablesConcatenated =
          getProcessModelExtensionProperty(delegateTask, "kadai-attributes");
      if (processVariablesConcatenated != null) {
        variableNames = splitVariableNamesString(processVariablesConcatenated);
      } else {
        return "{}";
      }
    }

    variableNames.forEach(
        nameOfVariableToAdd ->
            addToVariablesBuilder(
                delegateTask, objectMapper, variablesBuilder, nameOfVariableToAdd));

    if (variablesBuilder.length() > 0) {
      variablesBuilder.deleteCharAt(variablesBuilder.length() - 1).append("}");
      variablesBuilder.insert(0, "{");
    } else {
      return "{}";
    }
    return variablesBuilder.toString();
  }

  private void addToVariablesBuilder(
      DelegateTask delegateTask,
      ObjectMapper objectMapper,
      StringBuilder processVariablesBuilder,
      String nameOfprocessVariableToAdd) {

    TypedValue processVariable = delegateTask.getVariableTyped(nameOfprocessVariableToAdd);

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

      } catch (JsonProcessingException ex) {

        if (CamundaListenerConfiguration.shouldCatchAndLogExceptionForFaultyProcessVariables()) {

          LOGGER.error("Caught exception while trying to serialize process variables to JSON", ex);

        } else {
          throw new SystemException(
              "Exception while trying to serialize process variables to JSON", ex);
        }
      }
    }
  }

  private VariableValueDto determineProcessVariableTypeAndCreateVariableValueDto(
      TypedValue processVariable, ObjectMapper objectMapper) throws JsonProcessingException {

    VariableValueDto variableValueDto = new VariableValueDto();

    if (processVariable.getType().isPrimitiveValueType()) {

      variableValueDto.setType(processVariable.getType().getName());
      variableValueDto.setValue(processVariable.getValue());

    } else {

      variableValueDto.setType(processVariable.getType().getName());

      String processVariableJsonString =
          objectMapper.writeValueAsString(processVariable.getValue());
      variableValueDto.setValue(processVariableJsonString);

      Map<String, Object> valueInfo = new HashMap<>();
      valueInfo.put("serializationDataFormat", "application/json");
      valueInfo.put("objectTypeName", processVariable.getValue().getClass());
      variableValueDto.setValueInfo(valueInfo);
    }

    return variableValueDto;
  }

  private List<String> splitVariableNamesString(String variableNamesConcatenated) {
    return Arrays.asList(variableNamesConcatenated.trim().split("\\s*,\\s*"));
  }

  private String getProcessModelExtensionProperty(DelegateTask delegateTask, String propertyKey) {

    String propertyValue = null;

    BpmnModelInstance model = delegateTask.getExecution().getBpmnModelInstance();

    List<CamundaProperty> processModelExtensionProperties =
        model.getModelElementsByType(CamundaProperty.class).stream()
            .filter(camundaProperty -> camundaProperty.getCamundaName() != null)
            .filter(camundaProperty -> camundaProperty.getCamundaName().equals(propertyKey))
            .collect(Collectors.toList());

    if (processModelExtensionProperties.isEmpty()) {
      return propertyValue;
    } else {
      propertyValue = processModelExtensionProperties.get(0).getCamundaValue();
    }

    return propertyValue;
  }

  private String getUserTaskExtensionProperty(DelegateTask delegateTask, String propertyKey) {

    String propertyValue = null;

    ExtensionElements extensionElements =
        delegateTask.getExecution().getBpmnModelElementInstance().getExtensionElements();

    if (extensionElements == null) {
      return propertyValue;
    } else {
      CamundaProperties camundaProperties =
          extensionElements.getElementsQuery().filterByType(CamundaProperties.class).singleResult();

      List<CamundaProperty> userTaskExtensionProperties =
          camundaProperties.getCamundaProperties().stream()
              .filter(camundaProperty -> camundaProperty.getCamundaName() != null)
              .filter(camundaProperty -> camundaProperty.getCamundaName().equals(propertyKey))
              .collect(Collectors.toList());

      if (userTaskExtensionProperties.isEmpty()) {
        return propertyValue;
      } else {
        propertyValue = userTaskExtensionProperties.get(0).getCamundaValue();
      }
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
