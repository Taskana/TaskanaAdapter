package pro.taskana.adapter.taskanaconnector.api.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.task.api.CallbackState;
import pro.taskana.task.api.TaskService;
import pro.taskana.task.api.models.ObjectReference;
import pro.taskana.task.api.models.Task;
import pro.taskana.task.internal.models.TaskImpl;

/**
 * Maps properties between ReferencedTasks from external systems and corresponding TASKANA tasks.
 */
@Component
public class TaskInformationMapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskInformationMapper.class);
  private static final String CAMUNDA_PROCESS_VARIABLE_PREFIX = "camunda:";

  @Value("${taskana.adapter.mapping.default.objectreference.company:DEFAULT_COMPANY}")
  private String defaultCompany;

  @Value("${taskana.adapter.mapping.default.objectreference.system:DEFAULT_SYSTEM}")
  private String defaultSystem;

  @Value(
      "${taskana.adapter.mapping.default.objectreference.system.instance:DEFAULT_SYSTEM_INSTANCE}")
  private String defaultSystemInstance;

  @Value("${taskana.adapter.mapping.default.objectreference.type:DEFAULT_TYPE}")
  private String defaultType;

  @Value("${taskana.adapter.mapping.default.objectreference.value:DEFAULT_VALUE}")
  private String defaultValue;

  @Autowired private TaskService taskService;

  public Task convertToTaskanaTask(ReferencedTask referencedTask) {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("entry to TaskInformationMapper.convertToTaskanaTask {}", this);
    }

    String domain = referencedTask.getDomain();
    String classificationKey = referencedTask.getClassificationKey();

    String workbasketKey = referencedTask.getWorkbasketKey();
    TaskImpl taskanaTask = (TaskImpl) taskService.newTask(workbasketKey, domain);
    taskanaTask.setClassificationKey(classificationKey);
    taskanaTask.setBusinessProcessId(referencedTask.getBusinessProcessId());
    Map<String, String> callbackInfo = new HashMap<>();
    callbackInfo.put(Task.CALLBACK_STATE, CallbackState.CALLBACK_PROCESSING_REQUIRED.name());
    callbackInfo.put(TaskanaSystemConnectorImpl.REFERENCED_TASK_ID, referencedTask.getId());
    callbackInfo.put(TaskanaSystemConnectorImpl.SYSTEM_URL, referencedTask.getSystemUrl());
    taskanaTask.setCallbackInfo(callbackInfo);
    taskanaTask.setExternalId(referencedTask.getId());

    Map<String, String> customAttributes =
        retrieveCustomAttributesFromProcessVariables(referencedTask.getVariables());
    taskanaTask.setCustomAttributes(customAttributes);

    if (referencedTask.getName() != null && !referencedTask.getName().isEmpty()) {
      taskanaTask.setName(referencedTask.getName());
    } else {
      taskanaTask.setName(referencedTask.getTaskDefinitionKey());
    }
    taskanaTask.setDescription(referencedTask.getDescription());
    setTimestampsInTaskanaTask(taskanaTask, referencedTask);

    taskanaTask.setOwner(referencedTask.getAssignee());

    taskanaTask.setPrimaryObjRef(createObjectReference());

    return taskanaTask;
  }

  public ReferencedTask convertToReferencedTask(Task taskanaTask) {
    ReferencedTask referencedTask = new ReferencedTask();
    Map<String, String> callbackInfo = taskanaTask.getCallbackInfo();
    if (callbackInfo != null) {
      referencedTask.setSystemUrl(callbackInfo.get(TaskanaSystemConnectorImpl.SYSTEM_URL));
      referencedTask.setId(taskanaTask.getExternalId());
    }

    Map<String, String> customAttributes = taskanaTask.getCustomAttributeMap();
    if (customAttributes != null && !customAttributes.isEmpty()) {

      String processVariables = getProcessVariablesFromCustomAttributes(customAttributes);
      referencedTask.setVariables(processVariables);
    }
    referencedTask.setName(taskanaTask.getName());
    referencedTask.setDescription(taskanaTask.getDescription());
    referencedTask.setAssignee(taskanaTask.getOwner());
    return referencedTask;
  }

  private String getProcessVariablesFromCustomAttributes(Map<String, String> customAttributes) {

    StringBuilder builder = new StringBuilder();

    customAttributes.entrySet().stream()
        .filter(entry -> entry.getKey().startsWith(CAMUNDA_PROCESS_VARIABLE_PREFIX))
        .forEach(
            entry ->
                builder
                    .append("\"")
                    .append(entry.getKey().replace(CAMUNDA_PROCESS_VARIABLE_PREFIX, ""))
                    .append("\":")
                    .append(entry.getValue())
                    .append(","));

    if (builder.length() > 0) {
      return builder.deleteCharAt(builder.length() - 1).toString();
    }

    return "";
  }

  private Map<String, String> retrieveCustomAttributesFromProcessVariables(
      String processVariables) {

    Map<String, String> customAttributes = new HashMap<>();

    JSONObject jsonObject = new JSONObject(processVariables);

    jsonObject
        .toMap()
        .entrySet()
        .forEach(
            entry ->
                customAttributes.put(
                    CAMUNDA_PROCESS_VARIABLE_PREFIX + entry.getKey(),
                    String.valueOf(jsonObject.get(entry.getKey()))));

    return customAttributes;
  }

  private void setTimestampsInTaskanaTask(TaskImpl taskanaTask, ReferencedTask camundaTask) {
    Instant now = Instant.now();
    Instant created = convertStringToInstant(camundaTask.getCreated(), now);
    taskanaTask.setCreated(created);

    String due = camundaTask.getDue();
    if (due == null || due.isEmpty() || "null".equals(due)) {
      taskanaTask.setPlanned(now);
    } else {
      taskanaTask.setDue(convertStringToInstant(camundaTask.getDue(), now));
    }
    Instant planned = convertStringToInstant(camundaTask.getPlanned(), now);
    taskanaTask.setPlanned(planned);
  }

  private Instant convertStringToInstant(String strTimestamp, Instant defaultTimestamp) {
    if (strTimestamp == null || strTimestamp.isEmpty() || "null".equals(strTimestamp)) {
      return defaultTimestamp;
    } else {
      try {
        return parseDate(strTimestamp);
      } catch (RuntimeException e) {
        LOGGER.error("Caught {} when attemptin to parse date {} ", e, strTimestamp);
        return defaultTimestamp;
      }
    }
  }

  private Instant parseDate(String date) {
    if (date == null || date.isEmpty()) {
      return null;
    }
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    TemporalAccessor temporalAccessor = formatter.parse(date);
    LocalDateTime localDateTime = LocalDateTime.from(temporalAccessor);
    ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
    return Instant.from(zonedDateTime);
  }

  private ObjectReference createObjectReference() {

    return taskService.newObjectReference(
        defaultCompany, defaultSystem, defaultSystemInstance, defaultType, defaultValue);
  }

  @Override
  public String toString() {
    return "TaskInformationMapper [defaultCompany="
        + defaultCompany
        + ", defaultSystem="
        + defaultSystem
        + ", defaultSystemInstance="
        + defaultSystemInstance
        + ", defaultType="
        + defaultType
        + ", defaultValue="
        + defaultValue
        + ", taskService="
        + taskService
        + "]";
  }
}
