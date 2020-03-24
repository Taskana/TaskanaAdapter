package pro.taskana.adapter.taskanaconnector.api.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
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
    taskanaTask.setPlanned(Instant.now());
    Map<String, String> callbackInfo = new HashMap<>();
    callbackInfo.put(Task.CALLBACK_STATE, CallbackState.CALLBACK_PROCESSING_REQUIRED.name());
    callbackInfo.put(TaskanaSystemConnectorImpl.REFERENCED_TASK_ID, referencedTask.getId());
    callbackInfo.put(TaskanaSystemConnectorImpl.SYSTEM_URL, referencedTask.getSystemUrl());
    taskanaTask.setCallbackInfo(callbackInfo);
    taskanaTask.setExternalId(referencedTask.getId());

    Map<String, String> customAttributes = new HashMap<>();
    customAttributes.put(
        TaskanaSystemConnectorImpl.REFERENCED_TASK_VARIABLES, referencedTask.getVariables());
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

    Map<String, String> customAttributes = taskanaTask.getCustomAttributes();
    if (customAttributes != null) {
      referencedTask.setVariables(
          customAttributes.get(TaskanaSystemConnectorImpl.REFERENCED_TASK_VARIABLES));
    }
    referencedTask.setName(taskanaTask.getName());
    referencedTask.setDescription(taskanaTask.getDescription());
    referencedTask.setAssignee(taskanaTask.getOwner());
    return referencedTask;
  }

  private void setTimestampsInTaskanaTask(TaskImpl taskanaTask, ReferencedTask camundaTask) {
    Instant created = convertStringToInstant(camundaTask.getCreated(), Instant.now());
    taskanaTask.setCreated(created);
    Instant due = convertStringToInstant(camundaTask.getDue(), Instant.now());
    taskanaTask.setDue(due);
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
    ObjectReference objRef = new ObjectReference();
    objRef.setCompany(defaultCompany);
    objRef.setSystem(defaultSystem);
    objRef.setSystemInstance(defaultSystemInstance);
    objRef.setType(defaultType);
    objRef.setValue(defaultValue);
    return objRef;
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
