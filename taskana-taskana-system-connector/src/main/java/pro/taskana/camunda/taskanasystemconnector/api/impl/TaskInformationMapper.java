package pro.taskana.camunda.taskanasystemconnector.api.impl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pro.taskana.Classification;
import pro.taskana.ClassificationService;
import pro.taskana.ObjectReference;
import pro.taskana.Task;
import pro.taskana.TaskService;
import pro.taskana.TaskanaEngine;
import pro.taskana.Workbasket;
import pro.taskana.WorkbasketService;
import pro.taskana.WorkbasketType;
import pro.taskana.camunda.camundasystemconnector.api.CamundaTask;
import pro.taskana.exceptions.ClassificationAlreadyExistException;
import pro.taskana.exceptions.ClassificationNotFoundException;
import pro.taskana.exceptions.DomainNotFoundException;
import pro.taskana.exceptions.InvalidArgumentException;
import pro.taskana.exceptions.InvalidWorkbasketException;
import pro.taskana.exceptions.NotAuthorizedException;
import pro.taskana.exceptions.WorkbasketAlreadyExistException;
import pro.taskana.exceptions.WorkbasketNotFoundException;
import pro.taskana.impl.TaskImpl;

public class TaskInformationMapper {
    
    private TaskanaEngine taskanaEngine;
    private WorkbasketService workbasketService;
    
    private TaskService taskService;
    
    private ClassificationService classificationService;
    
    private static final String DEFAULT_WORKBASKET = "DEFAULT_WORKBASKET";
    private static final String DEFAULT_CLASSIFICATION = "DEFAULT_CLASSIFICATION";
    private static final String DEFAULT_DOMAIN = "DOMAIN_A";
    private static final String CLASSIFICATION_TYPE_TASK = "TASK";
    private static final String DEFAULT_COMPANY = "DEFAULT_COMPANY";
    private static final String DEFAULT_SYSTEM = "DEFAULT_SYSTEM";
    private static final String DEFAULT_SYSTEM_INSTANCE = "DEFAULT_SYSTEM_INSTANCE";
    private static final String DEFAULT_TYPE = "DEFAULT_TYPE";
    private static final String DEFAULT_VALUE = "DEFAULT_VALUE";
    private static final String CAMUNDA_TASK_ID = "camunda_task_id";
    private static final String CAMUNDA_SYSTEM_URL = "camunda_system_url";
    
    public TaskInformationMapper(TaskanaEngine taskanaEngine) {
        this.taskanaEngine = taskanaEngine;
        this.workbasketService = taskanaEngine.getWorkbasketService();
        this.taskService = taskanaEngine.getTaskService();
        this.classificationService = taskanaEngine.getClassificationService();
    }
    
    
    public Task convertToTaskanaTask(CamundaTask camundaTask) 
        throws DomainNotFoundException, InvalidWorkbasketException, NotAuthorizedException,
        WorkbasketAlreadyExistException, ClassificationAlreadyExistException, InvalidArgumentException {
        
        
        Workbasket workbasket = findOrCreateWorkbasket(camundaTask.getAssignee());
        Classification classification = findOrCreateClassification();
        ObjectReference objectReference = createObjectReference();

        TaskImpl taskanaTask = (TaskImpl) taskService.newTask(workbasket.getId());

        HashMap<String, String> callbackInfo = new HashMap<>();
        callbackInfo.put(CAMUNDA_TASK_ID, camundaTask.getId());
        callbackInfo.put(CAMUNDA_SYSTEM_URL, camundaTask.getCamundaSystemURL());
        taskanaTask.setCallbackInfo(callbackInfo);

        taskanaTask.setName(camundaTask.getName());
        taskanaTask.setDescription(camundaTask.getDescription());
        taskanaTask.setDue(parseDate(camundaTask.getDue()));
        taskanaTask.setOwner(camundaTask.getAssignee());
        taskanaTask.setBusinessProcessId(camundaTask.getProcessInstanceId());
        taskanaTask.setClassificationKey(classification.getKey());
        taskanaTask.setPrimaryObjRef(objectReference);

        return taskanaTask;
    }

    public CamundaTask convertToCamundaTask(Task taskanaTask) {
        CamundaTask camundaTask = new CamundaTask();
        camundaTask.setCamundaSystemURL(taskanaTask.getCallbackInfo().get(CAMUNDA_SYSTEM_URL));
        camundaTask.setId(taskanaTask.getCallbackInfo().get(CAMUNDA_TASK_ID));
        camundaTask.setName(taskanaTask.getName());
        camundaTask.setDescription(taskanaTask.getDescription());
        camundaTask.setAssignee(taskanaTask.getOwner());
        camundaTask.setProcessInstanceId(taskanaTask.getBusinessProcessId());
        return null;
    }

    private Instant parseDate(String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        TemporalAccessor temporalAccessor = formatter.parse(date);
        LocalDateTime localDateTime = LocalDateTime.from(temporalAccessor);
        ZonedDateTime zonedDateTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
        return Instant.from(zonedDateTime);
    }

    private Classification findOrCreateClassification()
        throws ClassificationAlreadyExistException, NotAuthorizedException,
        DomainNotFoundException, InvalidArgumentException {

        Classification classification;
        try {
            classification = classificationService.getClassification(DEFAULT_CLASSIFICATION, DEFAULT_DOMAIN);
        } catch (ClassificationNotFoundException e) {
            classification = classificationService.newClassification(DEFAULT_CLASSIFICATION, DEFAULT_DOMAIN,
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
    
    private Workbasket findOrCreateWorkbasket(String key) throws DomainNotFoundException,
    InvalidWorkbasketException, NotAuthorizedException, WorkbasketAlreadyExistException {
    if (key == null) {
        key = DEFAULT_WORKBASKET;
    }
    Workbasket wb;
    try {
        wb = workbasketService.getWorkbasket(key, DEFAULT_DOMAIN);
    } catch (WorkbasketNotFoundException e) {
        wb = workbasketService.newWorkbasket(key, DEFAULT_DOMAIN);
        wb.setName(key);
        wb.setType(WorkbasketType.PERSONAL);
        wb = workbasketService.createWorkbasket(wb);
    }
    return wb;
}


    
}
