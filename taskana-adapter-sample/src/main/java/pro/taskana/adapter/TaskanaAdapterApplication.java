package pro.taskana.adapter;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import pro.taskana.adapter.configuration.AdapterConfiguration;

/**
 * Application that provides an adapter between taskana and one or more external systems.
 *
 * @author kkl
 */

@SpringBootConfiguration
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan(basePackages = "pro.taskana.adapter")
@Import({AdapterConfiguration.class})

public class TaskanaAdapterApplication {


    public static void main(String[] args) {
        SpringApplication.run(TaskanaAdapterApplication.class, args);
    }

    public String dummyMethod() {
        return "Hello";
    }

}
