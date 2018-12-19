package pro.taskana.camunda;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import pro.taskana.camunda.configuration.RestClientConfiguration;

/**
 * Application that centralizes the tasks of several Camunda task lists in Taskana.
 *
 * @author kkl
 */

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan(basePackages = "pro.taskana.camunda")
@Import({RestClientConfiguration.class})

public class TaskanaCamundaAdapterApplication {


    public static void main(String[] args) {
        SpringApplication.run(TaskanaCamundaAdapterApplication.class, args);
    }

    public String dummyMethod() {
        return "Hello";
    }

}
