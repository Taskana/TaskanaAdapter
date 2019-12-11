package pro.taskana.adapter.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;

import pro.taskana.adapter.impl.ReferencedTaskCompleter;
import pro.taskana.adapter.impl.TaskanaTaskStarter;
import pro.taskana.adapter.impl.TaskanaTaskTerminator;
import pro.taskana.adapter.manager.AdapterManager;

/**
 * Configures the adapter.
 */
@EnableScheduling
@Import({SchedulerConfiguration.class})
@Configuration
public class AdapterConfiguration {

    @Value("${.jndi-name:no-jndi-configured}")
    private String jndiName;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public AdapterManager manager() {
        return new AdapterManager();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public ReferencedTaskCompleter referencedTaskCompleter() {
        return new ReferencedTaskCompleter();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public TaskanaTaskStarter taskanaTaskStarter() {
        return new TaskanaTaskStarter();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    TaskanaTaskTerminator taskanaTaskTerminator() {
        return new TaskanaTaskTerminator();
    }

}
