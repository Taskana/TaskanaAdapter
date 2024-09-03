package io.kadai.adapter.kadaiconnector.api.impl;

import io.kadai.adapter.systemconnector.api.ReferencedTask;
import io.kadai.task.api.CallbackState;
import io.kadai.task.api.TaskService;
import io.kadai.task.api.models.ObjectReference;
import io.kadai.task.api.models.Task;
import io.kadai.task.internal.models.TaskImpl;
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

/**
 * Maps properties between ReferencedTasks from external systems and corresponding KADAI tasks.
 */
@Component
public class TaskInformationMapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(TaskInformationMapper.class);
  private static final String CAMUNDA_PROCESS_VARIABLE_PREFIX = "camunda:";

  @Value("${kadai.adapter.mapping.default.objectreference.company:DEFAULT_COMPANY}")
  private String defaultCompany;

  @Value("${kadai.adapter.mapping.default.objectreference.system:DEFAULT_SYSTEM}")
  private String defaultSystem;

  @Value(
      "${kadai.adapter.mapping.default.objectreference.system.instance:DEFAULT_SYSTEM_INSTANCE}")
  private String defaultSystemInstance;

  @Value("${kadai.adapter.mapping.default.objectreference.type:DEFAULT_TYPE}")
  private String defaultType;

  @Value("${kadai.adapter.mapping.default.objectreference.value:DEFAULT_VALUE}")
  private String defaultValue;

  @Autowired private TaskService taskService;

  public Task convertToKadaiTask(ReferencedTask referencedTask) {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("entry to TaskInformationMapper.convertToKadaiTask {}", this);
    }

    String domain = referencedTask.getDomain();
    String classificationKey = referencedTask.getClassificationKey();

    String workbasketKey = referencedTask.getWorkbasketKey();
    TaskImpl kadaiTask = (TaskImpl) taskService.newTask(workbasketKey, domain);
    kadaiTask.setClassificationKey(classificationKey);
    kadaiTask.setBusinessProcessId(referencedTask.getBusinessProcessId());
    Map<String, String> callbackInfo = new HashMap<>();
    callbackInfo.put(Task.CALLBACK_STATE, CallbackState.CALLBACK_PROCESSING_REQUIRED.name());
    callbackInfo.put(KadaiSystemConnectorImpl.REFERENCED_TASK_ID, referencedTask.getId());
    callbackInfo.put(KadaiSystemConnectorImpl.SYSTEM_URL, referencedTask.getSystemUrl());
    kadaiTask.setCallbackInfo(callbackInfo);
    kadaiTask.setExternalId(referencedTask.getId());

    Map<String, String> customAttributes =
        retrieveCustomAttributesFromProcessVariables(referencedTask.getVariables());
    kadaiTask.setCustomAttributes(customAttributes);

    if (referencedTask.getName() != null && !referencedTask.getName().isEmpty()) {
      kadaiTask.setName(referencedTask.getName());
    } else {
      kadaiTask.setName(referencedTask.getTaskDefinitionKey());
    }
    kadaiTask.setDescription(referencedTask.getDescription());
    setTimestampsInKadaiTask(kadaiTask, referencedTask);

    kadaiTask.setOwner(referencedTask.getAssignee());

    kadaiTask.setPrimaryObjRef(createObjectReference());

    if (referencedTask.getManualPriority() == null
        || referencedTask.getManualPriority().isEmpty()
        || "null".equals(referencedTask.getManualPriority())) {
      kadaiTask.setManualPriority(-1);
    } else {
      kadaiTask.setManualPriority(Integer.parseInt(referencedTask.getManualPriority()));
    }

    setCustomIntegers(referencedTask, kadaiTask);

    return kadaiTask;
  }

  public ReferencedTask convertToReferencedTask(Task kadaiTask) {
    ReferencedTask referencedTask = new ReferencedTask();
    Map<String, String> callbackInfo = kadaiTask.getCallbackInfo();
    if (callbackInfo != null) {
      referencedTask.setSystemUrl(callbackInfo.get(KadaiSystemConnectorImpl.SYSTEM_URL));
      referencedTask.setId(kadaiTask.getExternalId());
    }

    Map<String, String> customAttributes = kadaiTask.getCustomAttributeMap();
    if (customAttributes != null && !customAttributes.isEmpty()) {

      String processVariables = getProcessVariablesFromCustomAttributes(customAttributes);
      referencedTask.setVariables(processVariables);
    }
    referencedTask.setName(kadaiTask.getName());
    referencedTask.setDescription(kadaiTask.getDescription());
    referencedTask.setAssignee(kadaiTask.getOwner());
    return referencedTask;
  }

  private void setCustomIntegers(ReferencedTask referencedTask, TaskImpl kadaiTask) {
    if (referencedTask.getCustomInt1() != null && !referencedTask.getCustomInt1().isEmpty()) {
      kadaiTask.setCustomInt1(Integer.parseInt(referencedTask.getCustomInt1()));
    }
    if (referencedTask.getCustomInt2() != null && !referencedTask.getCustomInt2().isEmpty()) {
      kadaiTask.setCustomInt2(Integer.parseInt(referencedTask.getCustomInt2()));
    }
    if (referencedTask.getCustomInt3() != null && !referencedTask.getCustomInt3().isEmpty()) {
      kadaiTask.setCustomInt3(Integer.parseInt(referencedTask.getCustomInt3()));
    }
    if (referencedTask.getCustomInt4() != null && !referencedTask.getCustomInt4().isEmpty()) {
      kadaiTask.setCustomInt4(Integer.parseInt(referencedTask.getCustomInt4()));
    }
    if (referencedTask.getCustomInt5() != null && !referencedTask.getCustomInt5().isEmpty()) {
      kadaiTask.setCustomInt5(Integer.parseInt(referencedTask.getCustomInt5()));
    }
    if (referencedTask.getCustomInt6() != null && !referencedTask.getCustomInt6().isEmpty()) {
      kadaiTask.setCustomInt6(Integer.parseInt(referencedTask.getCustomInt6()));
    }
    if (referencedTask.getCustomInt7() != null && !referencedTask.getCustomInt7().isEmpty()) {
      kadaiTask.setCustomInt7(Integer.parseInt(referencedTask.getCustomInt7()));
    }
    if (referencedTask.getCustomInt8() != null && !referencedTask.getCustomInt8().isEmpty()) {
      kadaiTask.setCustomInt8(Integer.parseInt(referencedTask.getCustomInt8()));
    }
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

  private void setTimestampsInKadaiTask(TaskImpl kadaiTask, ReferencedTask camundaTask) {
    Instant now = Instant.now();
    Instant created = convertStringToInstant(camundaTask.getCreated(), now);
    kadaiTask.setCreated(created);

    String due = camundaTask.getDue();
    if (due == null || due.isEmpty() || "null".equals(due)) {
      kadaiTask.setPlanned(now);
    } else {
      kadaiTask.setDue(convertStringToInstant(camundaTask.getDue(), now));
    }
    Instant planned = convertStringToInstant(camundaTask.getPlanned(), now);
    kadaiTask.setPlanned(planned);
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
