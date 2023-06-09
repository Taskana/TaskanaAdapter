package pro.taskana.adapter.taskanaconnector.config;

import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;
import pro.taskana.TaskanaConfiguration;
import pro.taskana.classification.api.ClassificationService;
import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.task.api.TaskService;
import pro.taskana.workbasket.api.WorkbasketService;

/**
 * Configuration for TASKANA task system connector.
 */
@Configuration
@DependsOn(value = {"adapterSpringContextProvider"})
public class TaskanaSystemConnectorConfiguration {

  @Value("${taskana.schemaName:TASKANA}")
  public String taskanaSchemaName;

  @Value("${taskana.datasource.jndi-name:no-jndi-configured}")
  private String jndiName;

  @Bean(name = "taskanaDataSource")
  @ConfigurationProperties(prefix = "taskana.datasource")
  public DataSource taskanaDataSource() throws NamingException {
    if ("no-jndi-configured".equals(jndiName)) {
      return DataSourceBuilder.create().build();
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
  public TaskanaEngine taskanaEngine(TaskanaConfiguration taskanaConfiguration)
      throws SQLException {
    return TaskanaEngine.buildTaskanaEngine(taskanaConfiguration);
  }

  @Bean
  @ConditionalOnMissingBean(TaskanaConfiguration.class)
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public TaskanaConfiguration taskanaConfiguration(
      @Qualifier("taskanaDataSource") DataSource taskanaDataSource,
      @Qualifier("taskanaPropertiesFileName") String propertiesFileName,
      @Qualifier("taskanaPropertiesDelimiter") String delimiter) {
    return new TaskanaConfiguration.Builder(taskanaDataSource, true, taskanaSchemaName, true)
        .initTaskanaProperties(propertiesFileName, delimiter)
        .build();
  }

  @Bean
  public String taskanaPropertiesFileName() {
    return "/taskana.properties";
  }

  @Bean
  public String taskanaPropertiesDelimiter() {
    return "|";
  }
}
