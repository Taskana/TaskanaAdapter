package pro.taskana.camunda.spring.boot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pro.taskana.adapter.camunda.outbox.rest.config.OutboxRestServiceConfig;
import pro.taskana.adapter.camunda.outbox.rest.controller.CamundaTaskEventsController;
import pro.taskana.adapter.camunda.parselistener.TaskanaParseListenerProcessEnginePlugin;

@SpringBootTest(classes = CamundaSpringBootExample.class, webEnvironment = RANDOM_PORT)
class CamundaSpringBootExampleIntegrationTest {

  private final OutboxRestServiceConfig outboxRestServiceConfig;

  private final CamundaTaskEventsController camundaTaskEventsController;

  private final TaskanaParseListenerProcessEnginePlugin taskanaParseListenerProcessEnginePlugin;

  CamundaSpringBootExampleIntegrationTest(
      @Autowired(required = false) OutboxRestServiceConfig outboxRestServiceConfig,
      @Autowired(required = false) CamundaTaskEventsController camundaTaskEventsController,
      @Autowired(required = false)
      TaskanaParseListenerProcessEnginePlugin taskanaParseListenerProcessEnginePlugin) {
    this.outboxRestServiceConfig = outboxRestServiceConfig;
    this.camundaTaskEventsController = camundaTaskEventsController;
    this.taskanaParseListenerProcessEnginePlugin = taskanaParseListenerProcessEnginePlugin;
  }

  @Test
  void should_AutowireOutboxRestServiceConfig_When_ApplicationIsStarting() {
    assertThat(outboxRestServiceConfig).isNotNull();
  }

  @Test
  void should_AutowireCamundaTaskEventsController_When_ApplicationIsStarting() {
    assertThat(camundaTaskEventsController).isNotNull();
  }

  @Test
  void should_AutowireTaskanaParseListenerProcessEnginePlugin_When_ApplicationIsStarting() {
    assertThat(taskanaParseListenerProcessEnginePlugin).isNotNull();
  }

}
