package io.kadai.adapter.systemconnector.camunda.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "httpcomponentsclient")
public class HttpComponentsClientProperties {

  private long connectionTimeout = 2_000;

  private long readTimeout = 5_000;

  public long getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(long connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public long getReadTimeout() {
    return readTimeout;
  }

  public void setReadTimeout(long readTimeout) {
    this.readTimeout = readTimeout;
  }
}
