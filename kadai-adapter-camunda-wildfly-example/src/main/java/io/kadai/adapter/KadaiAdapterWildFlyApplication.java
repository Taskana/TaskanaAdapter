package io.kadai.adapter;

import io.kadai.adapter.configuration.AdapterConfiguration;
import io.kadai.adapter.kadaiconnector.config.KadaiSystemConnectorConfiguration;
import io.kadai.adapter.systemconnector.camunda.config.CamundaSystemConnectorConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Example Application showing the implementation of kadai-adapter for jboss application server.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(
    basePackages = {
      "io.kadai.adapter",
      "io.kadai.adapter.configuration",
      "io.kadai",
      "io.kadai.adapter.systemconnector.camunda.config",
      "io.kadai.adapter.kadaiconnector.config"
    })
@SuppressWarnings("checkstyle:Indentation")
@Import({
  AdapterConfiguration.class,
  CamundaSystemConnectorConfiguration.class,
  KadaiSystemConnectorConfiguration.class
})
public class KadaiAdapterWildFlyApplication extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(KadaiAdapterWildFlyApplication.class, args);
  }
}
