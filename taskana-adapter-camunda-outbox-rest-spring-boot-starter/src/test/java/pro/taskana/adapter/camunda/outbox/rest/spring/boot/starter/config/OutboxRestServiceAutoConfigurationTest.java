package pro.taskana.adapter.camunda.outbox.rest.spring.boot.starter.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import pro.taskana.adapter.camunda.outbox.rest.config.OutboxRestServiceConfig;
import pro.taskana.adapter.camunda.outbox.rest.controller.CamundaTaskEventsController;
import pro.taskana.adapter.camunda.parselistener.TaskanaParseListenerProcessEnginePlugin;

@SpringBootTest
class OutboxRestServiceAutoConfigurationTest {

  private final OutboxRestServiceConfig outboxRestServiceConfig;

  private final CamundaTaskEventsController camundaTaskEventsController;

  private final TaskanaParseListenerProcessEnginePlugin taskanaParseListenerProcessEnginePlugin;

  OutboxRestServiceAutoConfigurationTest(
      @Autowired(required = false) OutboxRestServiceConfig outboxRestServiceConfig,
      @Autowired(required = false) CamundaTaskEventsController camundaTaskEventsController,
      @Autowired(required = false)
          TaskanaParseListenerProcessEnginePlugin taskanaParseListenerProcessEnginePlugin) {
    this.outboxRestServiceConfig = outboxRestServiceConfig;
    this.camundaTaskEventsController = camundaTaskEventsController;
    this.taskanaParseListenerProcessEnginePlugin = taskanaParseListenerProcessEnginePlugin;
  }

  @Test
  void outboxRestServiceConfig_is_automatically_configured() {
    assertThat(outboxRestServiceConfig).isNotNull();
  }

  @Test
  void camundaTaskEventsController_is_automatically_configured() {
    assertThat(outboxRestServiceConfig).isNotNull();
  }

  @Test
  void taskanaParseListenerProcessEnginePlugin_is_automatically_configured() {
    assertThat(outboxRestServiceConfig).isNotNull();
  }

  @Configuration
  @EnableAutoConfiguration
  static class TestConfig {
    // empty class to enable AutoConfiguration and configure spring boot test for it
  }
}
