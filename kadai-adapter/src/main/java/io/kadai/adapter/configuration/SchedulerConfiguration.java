package io.kadai.adapter.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * Configures the scheduler. Per default all @scheduled methods share a single thread. To provide a
 * unique thread per @scheduled method, we configure here 10 threads for the @scheduled methods.
 */
@Configuration
public class SchedulerConfiguration implements SchedulingConfigurer {

  @Override
  public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
    ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(10);
    taskScheduler.initialize();
    taskRegistrar.setTaskScheduler(taskScheduler);
  }
}
