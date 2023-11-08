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
  void default_Value_for_connection_Timeout_is_2000_milliseconds(
      @Autowired OkHttpProperties okHttpProperties
  ) {
    assertThat(okHttpProperties.getConnectionTimeout()).isEqualTo(2_000);
  }

  @Test
  void default_Value_for_read_Timeout_is_5000_milliseconds(
      @Autowired OkHttpProperties okHttpProperties
  ) {
    assertThat(okHttpProperties.getReadTimeout()).isEqualTo(5_000);
  }

  @EnableConfigurationProperties(OkHttpProperties.class)
  static class OkHttpPropertiesWithDefaultValuesIntegrationTestConfiguration {}
}
