package pro.taskana.camunda.taskanasystemconnector.api.impl;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pro.taskana.Classification;
import pro.taskana.ClassificationService;
import pro.taskana.ObjectReference;
import pro.taskana.Task;
import pro.taskana.TaskService;
import pro.taskana.Workbasket;
import pro.taskana.WorkbasketAccessItem;
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

    public Task convertToTaskanaTask(CamundaTask camundaTask) 
        throws DomainNotFoundException, InvalidWorkbasketException, NotAuthorizedException,
        WorkbasketAlreadyExistException, ClassificationAlreadyExistException, InvalidArgumentException, WorkbasketNotFoundException {
        
        
        Workbasket workbasket = findOrCreateWorkbasket(camundaTask.getAssignee());
        Classification classification = findOrCreateClassification();
        ObjectReference objectReference = createObjectReference();

        TaskImpl taskanaTask = (TaskImpl) taskService.newTask(workbasket.getId());
        HashMap<String, String> callbackInfo = new HashMap<>();
        callbackInfo.put(TaskanaSystemConnectorImpl.CAMUNDA_TASK_ID, camundaTask.getId());
        callbackInfo.put(TaskanaSystemConnectorImpl.CAMUNDA_SYSTEM_URL, camundaTask.getCamundaSystemURL());
        callbackInfo.put(TaskanaSystemConnectorImpl.CAMUNDA_TASK_INPUT_VARIABLES, camundaTask.getInputVariables());
        callbackInfo.put(TaskanaSystemConnectorImpl.CAMUNDA_TASK_OUTPUT_VARIABLES, camundaTask.getOutputVariables());
        taskanaTask.setCallbackInfo(callbackInfo);
        taskanaTask.setExternalId(camundaTask.getId());

        taskanaTask.setName(camundaTask.getName());
        taskanaTask.setDescription(camundaTask.getDescription());
        setTimestampsInTaskanaTask(taskanaTask, camundaTask);
        
        taskanaTask.setOwner(camundaTask.getAssignee());
        taskanaTask.setBusinessProcessId(camundaTask.getProcessInstanceId());
        taskanaTask.setClassificationKey(classification.getKey());
        taskanaTask.setPrimaryObjRef(objectReference);

        return taskanaTask;
    }

    private void setTimestampsInTaskanaTask(TaskImpl taskanaTask, CamundaTask camundaTask) {
    	Instant created = convertStringToInstant(camundaTask.getCreated(), Instant.now());
    	taskanaTask.setCreated(created);
    	Instant due = convertStringToInstant(camundaTask.getDue(),created.plus(Duration.ofDays(3)));
    	taskanaTask.setDue(due);    	
	}

	private Instant convertStringToInstant(String strTimestamp, Instant defaultTimestamp) {
		if (strTimestamp == null || strTimestamp.isEmpty()) {
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

	public CamundaTask convertToCamundaTask(Task taskanaTask) {
        CamundaTask camundaTask = new CamundaTask();
        camundaTask.setCamundaSystemURL(taskanaTask.getCallbackInfo().get(TaskanaSystemConnectorImpl.CAMUNDA_SYSTEM_URL));
        camundaTask.setId(taskanaTask.getCallbackInfo().get(TaskanaSystemConnectorImpl.CAMUNDA_TASK_ID));
        camundaTask.setInputVariables(taskanaTask.getCallbackInfo().get(TaskanaSystemConnectorImpl.CAMUNDA_TASK_INPUT_VARIABLES));
        camundaTask.setOutputVariables(taskanaTask.getCallbackInfo().get(TaskanaSystemConnectorImpl.CAMUNDA_TASK_OUTPUT_VARIABLES));
        camundaTask.setName(taskanaTask.getName());
        camundaTask.setDescription(taskanaTask.getDescription());
        camundaTask.setAssignee(taskanaTask.getOwner());
        camundaTask.setProcessInstanceId(taskanaTask.getBusinessProcessId());
        return camundaTask;
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
    InvalidWorkbasketException, NotAuthorizedException, WorkbasketAlreadyExistException, WorkbasketNotFoundException, InvalidArgumentException {
    if (key == null) {
        key = DEFAULT_WORKBASKET;
    }
    Workbasket wb;
    try {
        wb = workbasketService.getWorkbasket(key, DEFAULT_DOMAIN);
    } catch (WorkbasketNotFoundException e) {
        wb = workbasketService.newWorkbasket(key, DEFAULT_DOMAIN);
        wb.setName(key);
        wb.setOwner(key);
        wb.setType(WorkbasketType.PERSONAL);
        wb = workbasketService.createWorkbasket(wb);
        createWorkbasketAccessList(wb);
    }
    return wb;
}

    private void createWorkbasketAccessList(Workbasket wb) throws WorkbasketNotFoundException, InvalidArgumentException, NotAuthorizedException {
        WorkbasketAccessItem workbasketAccessItem = workbasketService.newWorkbasketAccessItem(wb.getId(), wb.getOwner());
        workbasketAccessItem.setAccessName(wb.getOwner());
        workbasketAccessItem.setPermAppend(true);
        workbasketAccessItem.setPermTransfer(true);
        workbasketAccessItem.setPermRead(true);
        workbasketAccessItem.setPermOpen(true);
        workbasketAccessItem.setPermDistribute(true);
        workbasketService.createWorkbasketAccessItem(workbasketAccessItem);
        
    }


    
}
