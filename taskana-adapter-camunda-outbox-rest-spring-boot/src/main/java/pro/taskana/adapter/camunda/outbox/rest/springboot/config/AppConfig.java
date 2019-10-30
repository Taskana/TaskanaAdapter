package pro.taskana.adapter.camunda.outbox.rest.springboot.config;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.taskana.adapter.camunda.outbox.rest.core.OutboxRestServiceCoreImpl;

import javax.sql.DataSource;

@Configuration
public class AppConfig {

    @Bean
    public OutboxRestServiceCoreImpl outboxRestServiceCore() {
        return new OutboxRestServiceCoreImpl();
    }


    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.url("jdbc:postgresql://localhost:5432/postgres");
        dataSourceBuilder.username("postgres");
        dataSourceBuilder.password("postgres");
        return dataSourceBuilder.build();    }

}