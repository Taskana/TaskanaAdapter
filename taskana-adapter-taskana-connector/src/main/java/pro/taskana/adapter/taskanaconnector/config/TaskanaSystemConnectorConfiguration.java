package pro.taskana.adapter.taskanaconnector.config;

import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import pro.taskana.ClassificationService;
import pro.taskana.TaskService;
import pro.taskana.TaskanaEngine;
import pro.taskana.WorkbasketService;
import pro.taskana.configuration.SpringTaskanaEngineConfiguration;
import pro.taskana.configuration.TaskanaEngineConfiguration;

@Configuration
@DependsOn(value= {"adapterSpringContextProvider"})
public class TaskanaSystemConnectorConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaSystemConnectorConfiguration.class);


    @Value("${taskana.schemaName:TASKANA}")
    public String taskanaSchemaName;

    @Value("${taskana.datasource.jndi-name:no-jndi-configured}")
    private String jndiName;

    @Bean(name = "taskanaDataSource")
    @ConfigurationProperties(prefix = "taskana.datasource")
    public DataSource adapterDataSource() throws NamingException {
        if ("no-jndi-configured".equals(jndiName)) {
            return  DataSourceBuilder.create().build();
        } else {
            Context ctx = new InitialContext();
            return (DataSource) ctx.lookup(jndiName);
        }
    }

    @Bean
    public TaskService getTaskService(TaskanaEngine taskanaEngine) {
        return taskanaEngine.getTaskService();
    }

    @Bean
    public WorkbasketService getWorkbasketService(TaskanaEngine taskanaEngine) {
        return taskanaEngine.getWorkbasketService();
    }

    @Bean
    public ClassificationService getClassificationService(TaskanaEngine taskanaEngine) {
        return taskanaEngine.getClassificationService();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public TaskanaEngine taskanaEngine(TaskanaEngineConfiguration taskanaEngineConfiguration) {
        return taskanaEngineConfiguration.buildTaskanaEngine();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public SpringTaskanaEngineConfiguration taskanaEngineConfiguration(@Qualifier("taskanaDataSource") DataSource dataSource) throws SQLException {
        return new SpringTaskanaEngineConfiguration(dataSource, true, false, taskanaSchemaName);
    }

    @Bean(name = "taskanaTransactionManager")
    public PlatformTransactionManager txManager(@Qualifier("taskanaDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

}
