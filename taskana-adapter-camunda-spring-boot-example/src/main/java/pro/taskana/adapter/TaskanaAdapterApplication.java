package pro.taskana.adapter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import pro.taskana.adapter.configuration.AdapterConfiguration;

/** Application that provides an adapter between TASKANA and one or more external systems. */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "pro.taskana.adapter")
@Import({AdapterConfiguration.class})
@EnableTransactionManagement
public class TaskanaAdapterApplication {

  public static void main(String[] args) {
    SpringApplication.run(TaskanaAdapterApplication.class, args);
  }

  // this method prevents checkstyle from thinking this class is a utility class
  public void dummy() {}
}
