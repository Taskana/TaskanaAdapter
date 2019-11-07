package pro.taskana.adapter.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.ibatis.session.SqlSessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.manager.AgentType;
import pro.taskana.adapter.mappings.AdapterMapper;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.adapter.util.Assert;
import pro.taskana.impl.util.IdGenerator;

/**
 * terminates taskana tasks if the associated task in the external system was finished.
 * 
 * @author bbr
 */
@Component
public class TaskanaTaskTerminator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskTerminator.class);

    private Map<String, Boolean> referencedTasksHaveAlreadyBeenProcessed = new HashMap<>();

    @Value("${taskanaAdapter.total.transaction.lifetime.in.seconds:120}")
    private int maximumTotalTransactionLifetime;

    @Autowired
    private SqlSessionManager sqlSessionManager;

    @Autowired
    AdapterManager adapterManager;

    private AdapterMapper adapterMapper;

    @PostConstruct
    public void init() {
        adapterMapper = sqlSessionManager.getMapper(AdapterMapper.class);
    }

    // @Scheduled(fixedRateString =
    // "${taskana.adapter.scheduler.run.interval.for.check.cancelled.referenced.tasks.in.milliseconds}")
    public void retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks() {

        synchronized (AdapterManager.class) {
            if (!adapterManager.isSpiInitialized()) {
                adapterManager.initSPIs();
                return;
            }
        }

        synchronized (this.getClass()) {

            if (!adapterManager.isInitialized()) {
                return;
            }

            LOGGER.debug(
                "----------retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks started----------------------------");

            adapterManager.openConnection(sqlSessionManager);
            try {
                for (SystemConnector systemConnector : (adapterManager.getSystemConnectors().values())) {
                    retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks(systemConnector);
                }
            } finally {
                LOGGER.debug(
                    "----------retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks finished ----------------------------");
                adapterManager.returnConnection(sqlSessionManager);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks(SystemConnector systemConnector) {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());

        try {
            Instant lowerThreshold;
            if (!referencedTasksHaveAlreadyBeenProcessed.containsKey(systemConnector.getSystemURL())) {
                Instant firstCreatedTask = adapterMapper.getOldestTaskCreationTimestamp(systemConnector.getSystemURL());
                if (firstCreatedTask != null) {
                    lowerThreshold = firstCreatedTask.minus(Duration.ofSeconds(maximumTotalTransactionLifetime));
                    referencedTasksHaveAlreadyBeenProcessed.put(systemConnector.getSystemURL(), true);
                } else {
                    LOGGER.debug(
                        "retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks didn't find running tasks -> returning");
                    return;
                }
            } else {
                Instant lastQuerytime = adapterMapper.getLatestQueryTimestamp(systemConnector.getSystemURL(),
                    AgentType.HANDLE_FINISHED_REFERENCED_TASKS);
                LOGGER.debug("lastQueryTime is {}", lastQuerytime);
                Assert.assertion(lastQuerytime != null, "lastQueryTime != null");
                lowerThreshold = lastQuerytime.minus(Duration.ofSeconds(maximumTotalTransactionLifetime));
            }

            List<ReferencedTask> tasksFinishedBySystem = systemConnector.retrieveFinishedTasks(lowerThreshold);
            List<ReferencedTask> taskanaTasksToTerminate = determineTaskanaTasksToTerminate(tasksFinishedBySystem,
                systemConnector.getSystemURL());
            for (ReferencedTask referencedTask : taskanaTasksToTerminate) {
                terminateTaskanaTask(referencedTask);
            }

            adapterMapper.rememberLastQueryTime(IdGenerator.generateWithPrefix("TCA"), Instant.now(),
                systemConnector.getSystemURL(), AgentType.HANDLE_FINISHED_REFERENCED_TASKS);
        } finally {
            LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(),
                Thread.currentThread().getStackTrace()[1].getMethodName());
        }
    }

    private List<ReferencedTask> determineTaskanaTasksToTerminate(List<ReferencedTask> tasksFinishedBySystem,
        String systemURL) {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());
        if (tasksFinishedBySystem == null || tasksFinishedBySystem.isEmpty()) {
            return new ArrayList<ReferencedTask>();
        }
        List<String> candidateTaskIds = tasksFinishedBySystem.stream()
            .map(ReferencedTask::getId)
            .collect(Collectors.toList());
        List<String> actualTaskIds = adapterMapper.findActiveTasks(systemURL, candidateTaskIds);
        List<ReferencedTask> result = tasksFinishedBySystem.stream()
            .filter(t -> actualTaskIds.contains(t.getId()))
            .collect(Collectors.toList());
        LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());
        return result;
    }

    private void terminateTaskanaTask(ReferencedTask referencedTask) {
        LOGGER.trace("{} {}", "ENTRY " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());
        List<TaskanaConnector> taskanaConnectors = adapterManager.getTaskanaConnectors();
        Assert.assertion(taskanaConnectors.size() == 1, "taskanaConnectors.size() == 1");
        TaskanaConnector taskanaConnector = taskanaConnectors.get(0);
        try {
            taskanaConnector.terminateTaskanaTask(referencedTask);
            adapterMapper.registerTaskCompleted(referencedTask.getId(), Instant.now());
        } catch (Exception ex) {

        }
        LOGGER.trace("{} {}", "EXIT " + getClass().getSimpleName(),
            Thread.currentThread().getStackTrace()[1].getMethodName());

    }

}
