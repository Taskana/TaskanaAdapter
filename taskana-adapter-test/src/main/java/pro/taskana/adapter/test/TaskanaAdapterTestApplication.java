package pro.taskana.adapter.test;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import pro.taskana.adapter.configuration.AdapterConfiguration;

/**
 * Application to test the integration of Camunda BPM with REST API with the Taskana Adapter.
 * 
 * @author Ben Fuernrohr
 */
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan(basePackages = {"pro.taskana.adapter", "pro.taskana.taskana_adapter_test"})
@Import({AdapterConfiguration.class})
@SpringBootApplication
@EnableProcessApplication
public class TaskanaAdapterTestApplication {

    public static void main(String... args) {
        SpringApplication.run(TaskanaAdapterTestApplication.class, args);
    }
}