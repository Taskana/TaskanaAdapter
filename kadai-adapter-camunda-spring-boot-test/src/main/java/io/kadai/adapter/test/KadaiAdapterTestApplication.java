package io.kadai.adapter.test;

import io.kadai.adapter.configuration.AdapterConfiguration;
import io.kadai.adapter.test.configuration.CamundaConfiguration;
import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Application to test the integration of Camunda BPM with REST API with the KADAI Adapter. */
@EnableScheduling
@ComponentScan("io.kadai.adapter")
@Import({AdapterConfiguration.class, CamundaConfiguration.class})
@SpringBootApplication
@EnableProcessApplication
@EnableTransactionManagement
public class KadaiAdapterTestApplication {

  public static void main(String... args) {
    SpringApplication.run(KadaiAdapterTestApplication.class, args);
  }

  // this method prevents checkstyle from thinking this class is a utility class
  public void dummy() {}
}
