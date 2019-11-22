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
import org.springframework.stereotype.Component;

import pro.taskana.CallbackState;
import pro.taskana.Classification;
import pro.taskana.ClassificationService;
import pro.taskana.ObjectReference;
import pro.taskana.Task;
import pro.taskana.TaskService;
import pro.taskana.Workbasket;
import pro.taskana.WorkbasketAccessItem;
import pro.taskana.WorkbasketService;
import pro.taskana.WorkbasketType;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.exceptions.ClassificationAlreadyExistException;
import pro.taskana.exceptions.ClassificationNotFoundException;
import pro.taskana.exceptions.DomainNotFoundException;
import pro.taskana.exceptions.InvalidArgumentException;
import pro.taskana.exceptions.InvalidWorkbasketException;
import pro.taskana.exceptions.NotAuthorizedException;
import pro.taskana.exceptions.WorkbasketAlreadyExistException;
import pro.taskana.exceptions.WorkbasketNotFoundException;
import pro.taskana.impl.TaskImpl;

@Component
public class TaskInformationMapper {

    @Autowired
    private WorkbasketService workbasketService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ClassificationService classificationService;

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskInformationMapper.class);
    private static final String DEFAULT_WORKBASKET = "DEFAULT_WORKBASKET";
    private static final String DEFAULT_CLASSIFICATION = "DEFAULT_CLASSIFICATION";
    private static final String DEFAULT_DOMAIN = "DOMAIN_A";
    private static final String CLASSIFICATION_TYPE_TASK = "TASK";
    private static final String DEFAULT_COMPANY = "DEFAULT_COMPANY";
    private static final String DEFAULT_SYSTEM = "DEFAULT_SYSTEM";
    private static final String DEFAULT_SYSTEM_INSTANCE = "DEFAULT_SYSTEM_INSTANCE";
    private static final String DEFAULT_TYPE = "DEFAULT_TYPE";
    private static final String DEFAULT_VALUE = "DEFAULT_VALUE";

    public Task convertToTaskanaTask(ReferencedTask referencedTask)
        throws DomainNotFoundException, InvalidWorkbasketException, NotAuthorizedException,
        WorkbasketAlreadyExistException, ClassificationAlreadyExistException, InvalidArgumentException,
        WorkbasketNotFoundException {

        String domain = referencedTask.getDomain();
        if (!isValidString(domain)) {
            domain = DEFAULT_DOMAIN;
        }

        Workbasket workbasket = findOrCreateWorkbasket(referencedTask.getWorkbasketKey(), domain,
            referencedTask.getAssignee());
        Classification classification = findOrCreateClassification(referencedTask.getClassificationKey(), domain);
        ObjectReference objectReference = createObjectReference();

        TaskImpl taskanaTask = (TaskImpl) taskService.newTask(workbasket.getId());
        Map<String, String> callbackInfo = new HashMap<>();
        callbackInfo.put(Task.CALLBACK_STATE, CallbackState.CALLBACK_PROCESSING_REQUIRED.name());
        callbackInfo.put(TaskanaSystemConnectorImpl.REFERENCED_TASK_ID, referencedTask.getId());
        callbackInfo.put(TaskanaSystemConnectorImpl.SYSTEM_URL, referencedTask.getSystemURL());
        taskanaTask.setCallbackInfo(callbackInfo);
        taskanaTask.setExternalId(referencedTask.getId());

        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put(TaskanaSystemConnectorImpl.REFERENCED_TASK_VARIABLES, referencedTask.getVariables());
        taskanaTask.setCustomAttributes(customAttributes);

        if (referencedTask.getName() != null && !referencedTask.getName().isEmpty()) {
            taskanaTask.setName(referencedTask.getName());
        } else {
            taskanaTask.setName(referencedTask.getTaskDefinitionKey());
        }
        taskanaTask.setDescription(referencedTask.getDescription());
        setTimestampsInTaskanaTask(taskanaTask, referencedTask);

        taskanaTask.setOwner(referencedTask.getAssignee());
        taskanaTask.setClassificationKey(classification.getKey());
        taskanaTask.setPrimaryObjRef(objectReference);

        return taskanaTask;
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

    public ReferencedTask convertToReferencedTask(Task taskanaTask) {
        ReferencedTask referencedTask = new ReferencedTask();
        Map<String, String> callbackInfo = taskanaTask.getCallbackInfo();
        if (callbackInfo != null) {
            referencedTask.setSystemURL(callbackInfo.get(TaskanaSystemConnectorImpl.SYSTEM_URL));
            referencedTask.setId(taskanaTask.getExternalId());
        }

        Map<String, String> customAttributes = taskanaTask.getCustomAttributes();
        if (customAttributes != null) {
            referencedTask.setVariables(customAttributes.get(TaskanaSystemConnectorImpl.REFERENCED_TASK_VARIABLES));
        }
        referencedTask.setName(taskanaTask.getName());
        referencedTask.setDescription(taskanaTask.getDescription());
        referencedTask.setAssignee(taskanaTask.getOwner());
        return referencedTask;
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

    private Classification findOrCreateClassification(String classificationKey, String domain)
        throws ClassificationAlreadyExistException, NotAuthorizedException,
        DomainNotFoundException, InvalidArgumentException {

        if (!isValidString(classificationKey)) {
            classificationKey = DEFAULT_CLASSIFICATION;
        }
        if (!isValidString(domain)) {
            domain = DEFAULT_DOMAIN;
        }
        Classification classification;
        try {
            classification = classificationService.getClassification(classificationKey, domain);
        } catch (ClassificationNotFoundException e) {
            classification = classificationService.newClassification(classificationKey, domain,
                CLASSIFICATION_TYPE_TASK);
            classification = classificationService.createClassification(classification);
        }
        return classification;
    }

    private ObjectReference createObjectReference() {
        ObjectReference objRef = new ObjectReference();
        objRef.setCompany(DEFAULT_COMPANY);
        objRef.setSystem(DEFAULT_SYSTEM);
        objRef.setSystemInstance(DEFAULT_SYSTEM_INSTANCE);
        objRef.setType(DEFAULT_TYPE);
        objRef.setValue(DEFAULT_VALUE);
        return objRef;
    }

    private Workbasket findOrCreateWorkbasket(String workbasketKey, String domain, String assignee)
        throws DomainNotFoundException,
        InvalidWorkbasketException, NotAuthorizedException, WorkbasketAlreadyExistException,
        WorkbasketNotFoundException, InvalidArgumentException {
        if (!isValidString(workbasketKey)) {
            workbasketKey = DEFAULT_WORKBASKET;
        }
        if (!isValidString(domain)) {
            domain = DEFAULT_DOMAIN;
        }
        Workbasket wb;

        try {
            wb = workbasketService.getWorkbasket(workbasketKey, domain);
        } catch (WorkbasketNotFoundException e) {
            wb = workbasketService.newWorkbasket(workbasketKey, domain);
            wb.setName(workbasketKey);
            wb.setOwner(assignee);
            wb.setType(WorkbasketType.PERSONAL);
            wb = workbasketService.createWorkbasket(wb);
            createWorkbasketAccessList(wb);
        }
        return wb;
    }

    private void createWorkbasketAccessList(Workbasket wb)
        throws WorkbasketNotFoundException, InvalidArgumentException, NotAuthorizedException {
        WorkbasketAccessItem workbasketAccessItem = workbasketService.newWorkbasketAccessItem(wb.getId(),
            wb.getOwner());
        workbasketAccessItem.setAccessName(wb.getOwner());
        workbasketAccessItem.setPermAppend(true);
        workbasketAccessItem.setPermTransfer(true);
        workbasketAccessItem.setPermRead(true);
        workbasketAccessItem.setPermOpen(true);
        workbasketAccessItem.setPermDistribute(true);
        workbasketService.createWorkbasketAccessItem(workbasketAccessItem);

    }

    boolean isValidString(String string) {
        return !(string == null || string.isEmpty() || "null".equals(string));
    }

}
