package pro.taskana.adapter.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.SqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pro.taskana.Task;
import pro.taskana.adapter.exceptions.ReferencedTaskDoesNotExistInExternalSystemException;
import pro.taskana.adapter.exceptions.TaskConversionFailedException;
import pro.taskana.adapter.exceptions.TaskCreationFailedException;
import pro.taskana.adapter.manager.AdapterConnection;
import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.adapter.util.Assert;
import pro.taskana.exceptions.TaskAlreadyExistException;

/**
 * Retrieves tasks in an external system and start corresponding tasks in taskana.
 *
 * @author bbr
 */
@Component
public class TaskanaTaskStarter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskStarter.class);

    @Value("${taskanaAdapter.total.transaction.lifetime.in.seconds:120}")
    private int maximumTotalTransactionLifetime;

    @Autowired
    private SqlSessionManager sqlSessionManager;

    @Autowired
    AdapterManager adapterManager;

    @Scheduled(fixedRateString = "${taskana.adapter.scheduler.run.interval.for.start.taskana.tasks.in.milliseconds}")
    public void retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks() {
        if (!adapterIsSPIInitialized()) {
            return;
        }

        synchronized (this.getClass()) {
            if (!adapterManager.isInitialized()) {
                return;
            }

            LOGGER.debug(
                "----------retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks started----------------------------");
            try (AdapterConnection connection = adapterManager.getAdapterConnection(sqlSessionManager)) {
                retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks();
            } catch (Exception ex) {
                LOGGER.error("Caught {} while trying to create Taskana tasks from referenced tasks", ex);
            }
        }
    }

    public void retrieveReferencedTasksAndCreateCorrespondingTaskanaTasks() {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());
        for (SystemConnector systemConnector : (adapterManager.getSystemConnectors().values())) {
            try {

                List<ReferencedTask> tasksToStart = systemConnector.retrieveNewStartedReferencedTasks();

                List<ReferencedTask> newCreatedTasksInTaskana = createAndStartTaskanaTasks(systemConnector,
                    tasksToStart);

                systemConnector.taskanaTasksHaveBeenCreatedForReferencedTasks(newCreatedTasksInTaskana);
            } finally {
                LOGGER.trace("{} {}",
                    "Leaving handling of new tasks for System Connector " + systemConnector.getSystemURL(),
                    getClass().getSimpleName(), Thread.currentThread().getStackTrace()[1].getMethodName());
            }
        }
    }

    private List<ReferencedTask> createAndStartTaskanaTasks(SystemConnector systemConnector,
        List<ReferencedTask> tasksToStart) {
        List<ReferencedTask> newCreatedTasksInTaskana = new ArrayList<>();
        for (ReferencedTask referencedTask : tasksToStart) {
            try {
                createTaskanaTask(referencedTask, adapterManager.getTaskanaConnectors(), systemConnector);
                newCreatedTasksInTaskana.add(referencedTask);
            } catch (Exception e) {
                if (e instanceof TaskCreationFailedException
                    && e.getCause() instanceof TaskAlreadyExistException) {
                    newCreatedTasksInTaskana.add(referencedTask);
                } else {
                    LOGGER.warn(
                        "caught Exception {} when attempting to start TaskanaTask for referencedTask {}", e,
                        referencedTask);
                }
            }
        }
        return newCreatedTasksInTaskana;
    }

    public void createTaskanaTask(ReferencedTask referencedTask, List<TaskanaConnector> taskanaConnectors,
        SystemConnector systemConnector) throws TaskConversionFailedException, TaskCreationFailedException {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());
        Assert.assertion(taskanaConnectors.size() == 1, "taskanaConnectors.size() == 1");
        TaskanaConnector connector = taskanaConnectors.get(0);
        referencedTask.setSystemURL(systemConnector.getSystemURL());
        try {
            addVariablesToReferencedTask(referencedTask, systemConnector);
            Task taskanaTask = connector.convertToTaskanaTask(referencedTask);
            connector.createTaskanaTask(taskanaTask);
        } catch (ReferencedTaskDoesNotExistInExternalSystemException e) {
            LOGGER.warn("While attempting to retrieve variables for task {} caught ", referencedTask.getId(), e);
        }

        LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());
    }

    private void addVariablesToReferencedTask(ReferencedTask referencedTask, SystemConnector connector) {
        if (referencedTask.getVariables() == null) {
            String variables = connector.retrieveVariables(referencedTask.getId());
            referencedTask.setVariables(variables);
        }
    }

    private boolean adapterIsSPIInitialized() {
        synchronized (AdapterManager.class) {
            if (!adapterManager.isSpiInitialized()) {
                adapterManager.initSPIs();
                return false;
            }
            return true;
        }
    }

}
