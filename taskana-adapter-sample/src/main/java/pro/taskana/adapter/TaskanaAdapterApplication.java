package pro.taskana.adapter;


import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

import pro.taskana.adapter.configuration.AdapterConfiguration;

/**
 * Application that provides an adapter between taskana and one or more external systems.
 *
 * @author kkl
 */

@SpringBootApplication 
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan(basePackages = "pro.taskana.adapter")
@Import({AdapterConfiguration.class})

public class TaskanaAdapterApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaAdapterApplication.class);

    @Value("${taskana.adapter.schemaName}")
    private String adapterSchemaName;

    @Bean(name = "adapterSchemaName")
    public String adapterSchemaName() {
        return adapterSchemaName;
    }

    public static void main(String[] args) {
        SpringApplication.run(TaskanaAdapterApplication.class, args);
    }

}
