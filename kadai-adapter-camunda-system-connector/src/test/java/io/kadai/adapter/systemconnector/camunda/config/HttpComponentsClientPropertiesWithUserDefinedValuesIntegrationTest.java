package io.kadai.adapter.systemconnector.camunda.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.systemconnector.camunda.config.HttpComponentsClientPropertiesWithUserDefinedValuesIntegrationTest.OkHttpPropertiesWithUserDefinedValuesIntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
    classes = {OkHttpPropertiesWithUserDefinedValuesIntegrationTestConfiguration.class},
    properties = {
      "httpcomponentsclient.connection-timeout=1000",
      "httpcomponentsclient.read-timeout=10000"
    })
class HttpComponentsClientPropertiesWithUserDefinedValuesIntegrationTest {

  @Test
  void should_HaveConnectionTimeout1000ms_When_PropertyOkHttpConnectionTimeoutIsSet(
      @Autowired HttpComponentsClientProperties httpComponentsClientProperties) {
    assertThat(httpComponentsClientProperties.getConnectionTimeout()).isEqualTo(1_000);
  }

  @Test
  void should_HaveReadTimeout10000ms_When_PropertyOkHttpReadTimeoutIsSet(
      @Autowired HttpComponentsClientProperties httpComponentsClientProperties) {
    assertThat(httpComponentsClientProperties.getReadTimeout()).isEqualTo(10_000);
  }

  @EnableConfigurationProperties(HttpComponentsClientProperties.class)
  static class OkHttpPropertiesWithUserDefinedValuesIntegrationTestConfiguration {}
}
