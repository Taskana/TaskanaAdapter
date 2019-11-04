package pro.taskana.adapter.camunda.outbox.rest.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.taskana.adapter.camunda.outbox.rest.core.OutboxRestServiceCoreImpl;

import javax.sql.DataSource;

@Configuration
public class OutboxRestServiceSpringBootConfig {

    @Bean
    public OutboxRestServiceCoreImpl outboxRestServiceCore() {
        return new OutboxRestServiceCoreImpl();
    }


    @Bean(name = "outboxRestServiceDataSource")
    @ConfigurationProperties(prefix = "taskana.adapter.camunda.outbox.rest.datasource")
    public DataSource outboxRestServiceDataSource() {
        return DataSourceBuilder.create().build();
    }

}