package io.kadai.adapter.kadaiconnector.config;

import io.kadai.KadaiConfiguration;
import io.kadai.classification.api.ClassificationService;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.internal.SpringKadaiEngine;
import io.kadai.task.api.TaskService;
import io.kadai.workbasket.api.WorkbasketService;
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
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Configuration for KADAI task system connector.
 */
@Configuration
@DependsOn(value = {"adapterSpringContextProvider"})
@EnableTransactionManagement
public class KadaiSystemConnectorConfiguration {

  @Value("${kadai.schemaName:KADAI}")
  public String kadaiSchemaName;

  @Value("${kadai.datasource.jndi-name:no-jndi-configured}")
  private String jndiName;

  @Bean(name = "kadaiDataSource")
  @ConfigurationProperties(prefix = "kadai.datasource")
  public DataSource kadaiDataSource() throws NamingException {
    if ("no-jndi-configured".equals(jndiName)) {
      return DataSourceBuilder.create().build();
    } else {
      Context ctx = new InitialContext();
      return (DataSource) ctx.lookup(jndiName);
    }
  }

  @Bean
  public TaskService getTaskService(KadaiEngine kadaiEngine) {
    return kadaiEngine.getTaskService();
  }

  @Bean
  public WorkbasketService getWorkbasketService(KadaiEngine kadaiEngine) {
    return kadaiEngine.getWorkbasketService();
  }

  @Bean
  public ClassificationService getClassificationService(KadaiEngine kadaiEngine) {
    return kadaiEngine.getClassificationService();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public SpringKadaiEngine kadaiEngine(KadaiConfiguration kadaiConfiguration)
      throws SQLException {
    return SpringKadaiEngine.buildKadaiEngine(kadaiConfiguration);
  }

  @Bean
  @ConditionalOnMissingBean(KadaiConfiguration.class)
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public KadaiConfiguration kadaiConfiguration(
      @Qualifier("kadaiDataSource") DataSource kadaiDataSource,
      @Qualifier("kadaiPropertiesFileName") String propertiesFileName,
      @Qualifier("kadaiPropertiesDelimiter") String delimiter) {
    return new KadaiConfiguration.Builder(kadaiDataSource, true, kadaiSchemaName, true)
        .initKadaiProperties(propertiesFileName, delimiter)
        .build();
  }

  @Bean
  public String kadaiPropertiesFileName() {
    return "/kadai.properties";
  }

  @Bean
  public String kadaiPropertiesDelimiter() {
    return "|";
  }
}
