package io.kadai.adapter.configuration;

import io.kadai.adapter.impl.KadaiTaskStarter;
import io.kadai.adapter.impl.KadaiTaskTerminator;
import io.kadai.adapter.impl.ReferencedTaskClaimCanceler;
import io.kadai.adapter.impl.ReferencedTaskClaimer;
import io.kadai.adapter.impl.ReferencedTaskCompleter;
import io.kadai.adapter.manager.AdapterManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Configures the adapter. */
@EnableScheduling
@Import({SchedulerConfiguration.class})
@Configuration
@EnableTransactionManagement
public class AdapterConfiguration {

  @Value("${.jndi-name:no-jndi-configured}")
  private String jndiName;

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public AdapterManager manager() {
    return new AdapterManager();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public ReferencedTaskCompleter referencedTaskCompleter() {
    return new ReferencedTaskCompleter();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public ReferencedTaskClaimer referencedTaskClaimer() {
    return new ReferencedTaskClaimer();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public ReferencedTaskClaimCanceler referencedTaskClaimCanceler() {
    return new ReferencedTaskClaimCanceler();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public KadaiTaskStarter kadaiTaskStarter() {
    return new KadaiTaskStarter();
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  KadaiTaskTerminator kadaiTaskTerminator() {
    return new KadaiTaskTerminator();
  }
}
