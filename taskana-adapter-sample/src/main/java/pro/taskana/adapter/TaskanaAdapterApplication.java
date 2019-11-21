package pro.taskana.adapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import pro.taskana.adapter.configuration.AdapterConfiguration;

/**
 * Application that provides an adapter between taskana and one or more external systems.
 *
 * @author kkl
 */

@SpringBootApplication
@EnableAutoConfiguration
@EnableScheduling
@ComponentScan(basePackages = "pro.taskana.adapter")
@Import({AdapterConfiguration.class})

public class TaskanaAdapterApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaAdapterApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(TaskanaAdapterApplication.class, args);
    }

}
