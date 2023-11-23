package pro.taskana.adapter.systemconnector.camunda.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import pro.taskana.adapter.systemconnector.camunda.config.OkHttpPropertiesWithUserDefinedValuesIntegrationTest.OkHttpPropertiesWithUserDefinedValuesIntegrationTestConfiguration;

@SpringBootTest(
    classes = {OkHttpPropertiesWithUserDefinedValuesIntegrationTestConfiguration.class},
    properties = {"okhttp.connection-timeout=1000", "okhttp.read-timeout=10000"})
class OkHttpPropertiesWithUserDefinedValuesIntegrationTest {

  @Test
  void should_HaveConnectionTimeout1000ms_When_PropertyOkHttpConnectionTimeoutIsSet(
      @Autowired OkHttpProperties okHttpProperties) {
    assertThat(okHttpProperties.getConnectionTimeout()).isEqualTo(1_000);
  }

  @Test
  void should_HaveReadTimeout10000ms_When_PropertyOkHttpReadTimeoutIsSet(
      @Autowired OkHttpProperties okHttpProperties) {
    assertThat(okHttpProperties.getReadTimeout()).isEqualTo(10_000);
  }

  @EnableConfigurationProperties(OkHttpProperties.class)
  static class OkHttpPropertiesWithUserDefinedValuesIntegrationTestConfiguration {}
}
