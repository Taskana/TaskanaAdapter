package pro.taskana.adapter.taskanaconnector.spi.impl;

import java.util.ArrayList;
import java.util.List;

import pro.taskana.adapter.configuration.SpringContextProvider;
import pro.taskana.adapter.taskanaconnector.api.TaskanaConnector;
import pro.taskana.adapter.taskanaconnector.spi.TaskanaConnectorProvider;

public class TaskanaConnectorProviderImpl implements TaskanaConnectorProvider {


    @Override
    public List<TaskanaConnector> create() {
        // note: this class is created by ServiceLoader, not by Spring. Therefore it is no bean and we must
        // retrieve the Spring-generated Bean for taskanaSystemConnector programmatically.
        // Only this bean has the correct injected properties.
        // In order for this bean to be retrievable, the SpringContextProvider must already be initialized.
        // This is assured via the
        // @DependsOn(value= {"springContextProvider"}) annotation of TaskanaSystemConnectorConfiguration

        List<TaskanaConnector> result = new ArrayList<>();
        TaskanaConnector taskanaSystemConnector = SpringContextProvider.getBean(TaskanaConnector.class);

        result.add(taskanaSystemConnector);
        return result;
    }

}
