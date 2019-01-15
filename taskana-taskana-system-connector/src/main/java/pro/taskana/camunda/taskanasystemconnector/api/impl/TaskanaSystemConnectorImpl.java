package pro.taskana.camunda.taskanasystemconnector.api.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pro.taskana.Task;
import pro.taskana.TaskService;
import pro.taskana.TaskState;
import pro.taskana.TaskSummary;
import pro.taskana.TimeInterval;
import pro.taskana.camunda.camundasystemconnector.api.CamundaTask;
import pro.taskana.camunda.exceptions.TaskConversionFailedException;
import pro.taskana.camunda.exceptions.TaskCreationFailedException;
import pro.taskana.camunda.scheduler.Scheduler;
import pro.taskana.camunda.taskanasystemconnector.api.TaskanaSystemConnector;
import pro.taskana.exceptions.ClassificationAlreadyExistException;
import pro.taskana.exceptions.ClassificationNotFoundException;
import pro.taskana.exceptions.DomainNotFoundException;
import pro.taskana.exceptions.InvalidArgumentException;
import pro.taskana.exceptions.InvalidWorkbasketException;
import pro.taskana.exceptions.NotAuthorizedException;
import pro.taskana.exceptions.TaskAlreadyExistException;
import pro.taskana.exceptions.TaskNotFoundException;
import pro.taskana.exceptions.WorkbasketAlreadyExistException;
import pro.taskana.exceptions.WorkbasketNotFoundException;

@Component
public class TaskanaSystemConnectorImpl implements TaskanaSystemConnector {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    static String  CAMUNDA_TASK_ID = "camunda_task_id";
    static String  CAMUNDA_TASK_INPUT_VARIABLES = "camunda_task_input_variables";
    static String  CAMUNDA_TASK_OUTPUT_VARIABLES = "camunda_task_output_variables";

    static String  CAMUNDA_SYSTEM_URL = "camunda_system_url";

    @Autowired    
    private TaskService taskService;
    
    @Autowired
    private TaskInformationMapper taskInformationMapper;

    @Override
    public List<CamundaTask> retrieveCompletedTaskanaTasks(Instant completedAfter) {
        Instant now = Instant.now();
        TimeInterval completedIn = new TimeInterval(completedAfter, now);

        List<TaskSummary> completedTasks = taskService.createTaskQuery()
                                           .stateIn(TaskState.COMPLETED)
                                           .completedWithin(completedIn)
                                           .list();
        List<CamundaTask> result = new ArrayList<>();
        for (TaskSummary taskSummary : completedTasks) {
            try {
                Task taskanaTask = taskService.getTask(taskSummary.getTaskId());
                Map<String,String> callbackInfo = taskanaTask.getCallbackInfo();
                if ( callbackInfo != null && callbackInfo.get(CAMUNDA_TASK_ID) != null 
                                          && callbackInfo.get(CAMUNDA_SYSTEM_URL) != null) {
                    result.add(taskInformationMapper.convertToCamundaTask(taskanaTask));
                }
            } catch (TaskNotFoundException | NotAuthorizedException e) {
                LOGGER.error("Caught {} when trying to retrieve completed taskana tasks." , e);
            }
        }
        
        return result;
    }

    @Override
    public void createTaskanaTask(Task taskanaTask) throws TaskCreationFailedException {
        try {
            taskService.createTask(taskanaTask);
       } catch ( NotAuthorizedException | InvalidArgumentException | ClassificationNotFoundException | WorkbasketNotFoundException | TaskAlreadyExistException e) {
            LOGGER.error("Caught Exception {} when creating taskana task {} ", e, taskanaTask); 
            throw new TaskCreationFailedException("Error when creationg a taskana task " + taskanaTask , e);
        } 
    }

    @Override
    public Task convertToTaskanaTask(CamundaTask camundaTask) throws TaskConversionFailedException {
        try {
            return taskInformationMapper.convertToTaskanaTask(camundaTask);
        } catch (DomainNotFoundException | InvalidWorkbasketException | NotAuthorizedException | WorkbasketNotFoundException
            | WorkbasketAlreadyExistException | ClassificationAlreadyExistException | InvalidArgumentException e) {
            throw new TaskConversionFailedException("Error when converting camunda task " + camundaTask + " to taskana task.", e);
        }
    }

    @Override
    public CamundaTask convertToCamundaTask(Task task) {
        return taskInformationMapper.convertToCamundaTask(task);
    }

}
