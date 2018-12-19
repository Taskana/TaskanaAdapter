package pro.taskana.camunda.taskanasystemconnector.api.impl;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import pro.taskana.Task;
import pro.taskana.TaskService;
import pro.taskana.TaskState;
import pro.taskana.TaskSummary;
import pro.taskana.TaskanaEngine;
import pro.taskana.TimeInterval;
import pro.taskana.camunda.camundasystemconnector.api.CamundaTask;
import pro.taskana.camunda.exceptions.TaskConversionFailedException;
import pro.taskana.camunda.exceptions.TaskCreationFailedException;
import pro.taskana.camunda.scheduler.Scheduler;
import pro.taskana.camunda.taskanasystemconnector.api.TaskanaSystemConnector;
import pro.taskana.camunda.taskanasystemconnector.config.TaskanaSystemConnectorConfiguration;
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


public class TaskanaSystemConnectorImpl implements TaskanaSystemConnector {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(Scheduler.class);

    private TaskanaEngine taskanaEngine;
    private TaskService taskService;
    private TaskInformationMapper taskInformationMapper;

    public TaskanaSystemConnectorImpl() throws SQLException {
        TaskanaSystemConnectorConfiguration configuration = new TaskanaSystemConnectorConfiguration();
        taskanaEngine = configuration.getTaskanaEngine();
        taskService = taskanaEngine.getTaskService();
        taskInformationMapper = new TaskInformationMapper(taskanaEngine);
    }    

    @Override
    public List<CamundaTask> retrieveCompletedTaskanaTasks(Instant completedAfter) {
        Instant now = Instant.now();
        TimeInterval completedIn = new TimeInterval(completedAfter, now);

        List<TaskSummary> completedTasks = taskanaEngine.getTaskService().createTaskQuery()
                                           .stateIn(TaskState.COMPLETED)
                                           .completedWithin(completedIn)
                                           .list();
        List<CamundaTask> result = new ArrayList<>();
        for (TaskSummary taskSummary : completedTasks) {
            try {
                Task taskanaTask = taskService.getTask(taskSummary.getTaskId());
                result.add(taskInformationMapper.convertToCamundaTask(taskanaTask));
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
        } catch (DomainNotFoundException | InvalidWorkbasketException | NotAuthorizedException
            | WorkbasketAlreadyExistException | ClassificationAlreadyExistException | InvalidArgumentException e) {
            throw new TaskConversionFailedException("Error when converting camunda task " + camundaTask + " to taskana task.", e);
        }
    }

    @Override
    public CamundaTask convertToCamundaTask(Task task) {
        return taskInformationMapper.convertToCamundaTask(task);
    }

}
