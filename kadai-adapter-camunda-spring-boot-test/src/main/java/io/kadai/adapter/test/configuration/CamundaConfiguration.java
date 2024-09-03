package io.kadai.adapter.test.configuration;

import javax.sql.DataSource;
import org.camunda.bpm.engine.impl.jobexecutor.DefaultJobExecutor;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Configuration for Camunda BPM. */
@Configuration
public class CamundaConfiguration {

  @Bean
  public JobExecutor customJobExecutor() {
    DefaultJobExecutor jobExecutor = new DefaultJobExecutor();
    jobExecutor.setWaitTimeInMillis(1500);
    jobExecutor.setMaxWait(2000);
    return jobExecutor;
  }

  @Bean(name = "camundaBpmDataSource")
  @Primary
  @ConfigurationProperties(prefix = "camunda.datasource")
  public DataSource secondaryDataSource() {
    return DataSourceBuilder.create().build();
  }
}
