package pro.taskana.adapter.test.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Taskana Adapter.
 * 
 * @author Ben Fuernrohr
 */
@Configuration
public class TaskanaAdapterConfiguration {

    @Value("${taskana.adapter.schemaName}")
    private String adapterSchemaName;

    @Bean(name = "adapterSchemaName")
    public String adapterSchemaName() {
        return adapterSchemaName;
    }
}
