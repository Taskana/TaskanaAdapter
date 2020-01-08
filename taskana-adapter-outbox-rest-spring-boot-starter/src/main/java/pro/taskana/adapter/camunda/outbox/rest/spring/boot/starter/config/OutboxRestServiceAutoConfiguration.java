package pro.taskana.adapter.camunda.outbox.rest.spring.boot.starter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pro.taskana.adapter.camunda.outbox.rest.config.OutboxRestServiceConfig;
import pro.taskana.adapter.camunda.outbox.rest.controller.CamundaTaskEventsController;
import pro.taskana.adapter.camunda.parselistener.TaskanaParseListenerProcessEnginePlugin;

/**
 * Configuration for the outbox REST service.
 *
 * @author jhe
 */
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
  public TaskanaParseListenerProcessEnginePlugin taskanaParseListenerProcessEnginePlugin() {
    return new TaskanaParseListenerProcessEnginePlugin();
  }
}
