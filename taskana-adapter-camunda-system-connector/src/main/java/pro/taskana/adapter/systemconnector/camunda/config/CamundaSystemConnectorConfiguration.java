package pro.taskana.adapter.systemconnector.camunda.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.client.RestTemplate;

import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskClaimCanceler;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskClaimer;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskCompleter;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskEventCleaner;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
import pro.taskana.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;

/** Configures the camunda system connector. */
@Configuration
@DependsOn(value = {"adapterSpringContextProvider"})
public class CamundaSystemConnectorConfiguration {
  @Bean
  HttpHeaderProvider httpHeaderProvider() {
    return new HttpHeaderProvider();
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }

  @Bean
  CamundaSystemUrls camundaSystemUrls(
      @Value("${taskana-system-connector-camundaSystemURLs}") final String strUrls) {
    return new CamundaSystemUrls(strUrls);
  }

  @Bean
  @DependsOn(value = {"httpHeaderProvider"})
  CamundaTaskRetriever camundaTaskRetriever() {
    return new CamundaTaskRetriever();
  }

  @Bean
  CamundaTaskCompleter camundaTaskCompleter() {
    return new CamundaTaskCompleter();
  }

  @Bean
  CamundaTaskClaimer camundaTaskClaimer() {
    return new CamundaTaskClaimer();
  }

  @Bean
  CamundaTaskClaimCanceler camundaTaskClaimCanceler() {
    return new CamundaTaskClaimCanceler();
  }

  @Bean
  CamundaTaskEventCleaner camundaTaskEventCleaner() {
    return new CamundaTaskEventCleaner();
  }
}
