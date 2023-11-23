package pro.taskana.adapter.systemconnector.camunda.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import pro.taskana.adapter.systemconnector.camunda.config.OkHttpPropertiesWithDefaultValuesIntegrationTest.OkHttpPropertiesWithDefaultValuesIntegrationTestConfiguration;

@SpringBootTest(classes = {OkHttpPropertiesWithDefaultValuesIntegrationTestConfiguration.class})
class OkHttpPropertiesWithDefaultValuesIntegrationTest {

  @Test
  void should_HaveDefaultConnectionTimeout2000ms_When_NoPropertyOkHttpConnectionTimeoutIsSet(
      @Autowired OkHttpProperties okHttpProperties
  ) {
    assertThat(okHttpProperties.getConnectionTimeout()).isEqualTo(2_000);
  }

  @Test
  void should_HaveDefaultReadTimeout5000ms_When_NoPropertyOkHttpReadTimeoutIsSet(
      @Autowired OkHttpProperties okHttpProperties
  ) {
    assertThat(okHttpProperties.getReadTimeout()).isEqualTo(5_000);
  }

  @EnableConfigurationProperties(OkHttpProperties.class)
  static class OkHttpPropertiesWithDefaultValuesIntegrationTestConfiguration {}
}
