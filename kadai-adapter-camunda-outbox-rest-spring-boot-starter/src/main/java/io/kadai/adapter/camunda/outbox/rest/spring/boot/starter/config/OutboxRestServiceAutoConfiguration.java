package io.kadai.adapter.camunda.outbox.rest.spring.boot.starter.config;

import io.kadai.adapter.camunda.outbox.rest.config.OutboxRestServiceConfig;
import io.kadai.adapter.camunda.outbox.rest.controller.CamundaTaskEventsController;
import io.kadai.adapter.camunda.parselistener.KadaiParseListenerProcessEnginePlugin;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuration for the outbox REST service. */
@Configuration
@ConditionalOnClass(CamundaTaskEventsController.class)
public class OutboxRestServiceAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public OutboxRestServiceConfig outboxRestServiceConfig() {
    return new OutboxRestServiceConfig();
  }

  @Bean
  @ConditionalOnMissingBean
  public CamundaTaskEventsController camundaTaskEventsController() {
    return new CamundaTaskEventsController();
  }

  @Bean
  @ConditionalOnMissingBean
  public KadaiParseListenerProcessEnginePlugin kadaiParseListenerProcessEnginePlugin() {
    return new KadaiParseListenerProcessEnginePlugin();
  }
}
