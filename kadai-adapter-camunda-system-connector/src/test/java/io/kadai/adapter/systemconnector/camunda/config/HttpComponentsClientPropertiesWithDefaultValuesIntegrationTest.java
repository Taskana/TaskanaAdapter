package io.kadai.adapter.systemconnector.camunda.config;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.systemconnector.camunda.config.HttpComponentsClientPropertiesWithDefaultValuesIntegrationTest.OkHttpPropertiesWithDefaultValuesIntegrationTestConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {OkHttpPropertiesWithDefaultValuesIntegrationTestConfiguration.class})
class HttpComponentsClientPropertiesWithDefaultValuesIntegrationTest {

  @Test
  void should_HaveDefaultConnectionTimeout2000ms_When_NoPropertyOkHttpConnectionTimeoutIsSet(
      @Autowired HttpComponentsClientProperties httpComponentsClientProperties) {
    assertThat(httpComponentsClientProperties.getConnectionTimeout()).isEqualTo(2_000);
  }

  @Test
  void should_HaveDefaultReadTimeout5000ms_When_NoPropertyOkHttpReadTimeoutIsSet(
      @Autowired HttpComponentsClientProperties httpComponentsClientProperties) {
    assertThat(httpComponentsClientProperties.getReadTimeout()).isEqualTo(5_000);
  }

  @EnableConfigurationProperties(HttpComponentsClientProperties.class)
  static class OkHttpPropertiesWithDefaultValuesIntegrationTestConfiguration {}
}
