package pro.taskana.camunda.taskanasystemconnector.spi.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import pro.taskana.camunda.taskanasystemconnector.api.TaskanaSystemConnector;
import pro.taskana.camunda.taskanasystemconnector.api.impl.TaskanaSystemConnectorImpl;
import pro.taskana.camunda.taskanasystemconnector.spi.TaskanaSystemConnectorProvider;

public class TaskanaSystemConnectorProviderImpl implements TaskanaSystemConnectorProvider {

    TaskanaSystemConnector taskanaSystemConnector;

    public TaskanaSystemConnectorProviderImpl() throws SQLException {
        taskanaSystemConnector = new TaskanaSystemConnectorImpl();
    }
    
    @Override
    public List<TaskanaSystemConnector> create() {
        List<TaskanaSystemConnector> result = new ArrayList<>();
        result.add(taskanaSystemConnector);
        return result;
    }

}
