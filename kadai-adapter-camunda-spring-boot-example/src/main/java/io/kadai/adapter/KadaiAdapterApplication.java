package io.kadai.adapter;

import io.kadai.adapter.configuration.AdapterConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/** Application that provides an adapter between KADAI and one or more external systems. */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "io.kadai.adapter")
@Import({AdapterConfiguration.class})
@EnableTransactionManagement
public class KadaiAdapterApplication {

  public static void main(String[] args) {
    SpringApplication.run(KadaiAdapterApplication.class, args);
  }

  // this method prevents checkstyle from thinking this class is a utility class
  public void dummy() {}
}
