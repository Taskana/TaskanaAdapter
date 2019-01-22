package pro.taskana.adapter.taskanaconnector.api.impl;

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
import pro.taskana.adapter.exceptions.TaskConversionFailedException;
import pro.taskana.adapter.exceptions.TaskCreationFailedException;
import pro.taskana.adapter.scheduler.Scheduler;
import pro.taskana.adapter.systemconnector.api.GeneralTask;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
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
public class TaskanaSystemConnectorImpl implements TaskanaConnector {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);
    static String  GENERAL_TASK_ID = "general_task_id";
    static String  GENERAL_TASK_VARIABLES = "general_task_variables";

    static String  SYSTEM_URL = "system_url";

    @Autowired    
    private TaskService taskService;
    
    @Autowired
    private TaskInformationMapper taskInformationMapper;

    @Override
    public List<GeneralTask> retrieveCompletedTaskanaTasks(Instant completedAfter) {
        Instant now = Instant.now();
        TimeInterval completedIn = new TimeInterval(completedAfter, now);

        List<TaskSummary> completedTasks = taskService.createTaskQuery()
                                           .stateIn(TaskState.COMPLETED)
                                           .completedWithin(completedIn)
                                           .list();
        List<GeneralTask> result = new ArrayList<>();
        for (TaskSummary taskSummary : completedTasks) {
            try {
                Task taskanaTask = taskService.getTask(taskSummary.getTaskId());
                Map<String,String> callbackInfo = taskanaTask.getCallbackInfo();
                if ( callbackInfo != null && callbackInfo.get(GENERAL_TASK_ID) != null 
                                          && callbackInfo.get(SYSTEM_URL) != null) {
                    result.add(taskInformationMapper.convertToGeneralTask(taskanaTask));
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
    public Task convertToTaskanaTask(GeneralTask camundaTask) throws TaskConversionFailedException {
        try {
            return taskInformationMapper.convertToTaskanaTask(camundaTask);
        } catch (DomainNotFoundException | InvalidWorkbasketException | NotAuthorizedException | WorkbasketNotFoundException
            | WorkbasketAlreadyExistException | ClassificationAlreadyExistException | InvalidArgumentException e) {
            throw new TaskConversionFailedException("Error when converting camunda task " + camundaTask + " to taskana task.", e);
        }
    }

    @Override
    public GeneralTask convertToGeneralTask(Task task) {
        return taskInformationMapper.convertToGeneralTask(task);
    }

}
