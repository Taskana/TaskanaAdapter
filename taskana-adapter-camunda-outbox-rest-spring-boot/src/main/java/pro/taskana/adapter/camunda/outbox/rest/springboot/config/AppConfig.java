package pro.taskana.adapter.camunda.outbox.rest.springboot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.taskana.adapter.camunda.outbox.rest.core.OutboxRestServiceCoreImpl;

@Configuration
public class AppConfig {

    @Bean
    public OutboxRestServiceCoreImpl outboxRestServiceCore() {
        return new OutboxRestServiceCoreImpl();
    }

}