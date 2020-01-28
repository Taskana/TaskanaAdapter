package pro.taskana.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import pro.taskana.adapter.configuration.AdapterConfiguration;
import pro.taskana.adapter.systemconnector.camunda.config.CamundaSystemConnectorConfiguration;
import pro.taskana.adapter.taskanaconnector.config.TaskanaSystemConnectorConfiguration;

/**
 * Example Application showing the implementation of taskana-adapter for jboss application server.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(
    basePackages = {
      "pro.taskana.adapter",
      "pro.taskana.adapter.configuration",
      "pro.taskana",
      "pro.taskana.adapter.systemconnector.camunda.config",
      "pro.taskana.adapter.taskanaconnector.config"
    })
@SuppressWarnings("checkstyle:Indentation")
@Import({
  AdapterConfiguration.class,
  CamundaSystemConnectorConfiguration.class,
  TaskanaSystemConnectorConfiguration.class
})
public class TaskanaAdapterWildFlyApplication extends SpringBootServletInitializer {

  public static void main(String[] args) {
    SpringApplication.run(TaskanaAdapterWildFlyApplication.class, args);
  }
}
