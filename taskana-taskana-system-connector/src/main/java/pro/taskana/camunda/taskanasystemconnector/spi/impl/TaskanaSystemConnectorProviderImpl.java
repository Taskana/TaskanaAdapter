package pro.taskana.camunda.taskanasystemconnector.spi.impl;

import java.util.ArrayList;
import java.util.List;

import pro.taskana.camunda.configuration.SpringContextProvider;
import pro.taskana.camunda.taskanasystemconnector.api.TaskanaSystemConnector;
import pro.taskana.camunda.taskanasystemconnector.spi.TaskanaSystemConnectorProvider;

public class TaskanaSystemConnectorProviderImpl implements TaskanaSystemConnectorProvider {


    @Override
    public List<TaskanaSystemConnector> create() {
        // note: this class is created by ServiceLoader, not by Spring. Therefore it is no bean and we must
        // retrieve the Spring-generated Bean for taskanaSystemConnector programmatically.
        // Only this bean has the correct injected properties.
        // In order for this bean to be retrievable, the SpringContextProvider must already be initialized.
        // This is assured via the
        // @DependsOn(value= {"springContextProvider"}) annotation of TaskanaSystemConnectorConfiguration

        List<TaskanaSystemConnector> result = new ArrayList<>();
        TaskanaSystemConnector taskanaSystemConnector = SpringContextProvider.getBean(TaskanaSystemConnector.class);

        result.add(taskanaSystemConnector);
        return result;
    }

}
