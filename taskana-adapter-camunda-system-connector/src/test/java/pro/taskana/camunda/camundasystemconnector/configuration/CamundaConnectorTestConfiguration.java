package pro.taskana.camunda.camundasystemconnector.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskCompleter;
import pro.taskana.adapter.systemconnector.camunda.api.impl.CamundaTaskRetriever;
import pro.taskana.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import pro.taskana.adapter.systemconnector.camunda.config.HttpComponentsClientProperties;

/**
 * Configuration for test of Camunda System Connector.
 *
 * @author bbr
 */
@Configuration
@EnableConfigurationProperties(HttpComponentsClientProperties.class)
public class CamundaConnectorTestConfiguration {

  @Bean
  RestTemplate restTemplate(
      RestTemplateBuilder builder, HttpComponentsClientProperties httpComponentsClientProperties) {
    return builder
        .setConnectTimeout(Duration.ofMillis(httpComponentsClientProperties.getConnectionTimeout()))
        .setReadTimeout(Duration.ofMillis(httpComponentsClientProperties.getReadTimeout()))
        .requestFactory(HttpComponentsClientHttpRequestFactory.class)
        .build();
  }

  @Bean
  RestTemplateBuilder restTemplateBuilder() {
    return new RestTemplateBuilder(new MockServerRestTemplateCustomizer());
  }

  @Bean
  ObjectMapper objectMapper() {
    return new ObjectMapper();
  }

  @Bean
  HttpHeaderProvider httpHeaderProvider() {
    return new HttpHeaderProvider();
  }

  @Bean
  CamundaTaskRetriever camundaTaskRetriever() {
    return new CamundaTaskRetriever();
  }

  @Bean
  CamundaTaskCompleter camundaTaskCompleter() {
    return new CamundaTaskCompleter();
  }
}
