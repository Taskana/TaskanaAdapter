package io.kadai.adapter.configuration;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** This class provides access to spring beans for classes not managed by Spring IOC container. */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class AdapterSpringContextProvider implements ApplicationContextAware {

  private static ApplicationContext context;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) {
    context = applicationContext;
  }

  /**
   * Get a Spring bean by type.
   *
   * @param <T> the type of the bean class
   * @param beanClass the class of the bean
   * @return the bean
   */
  public static <T> T getBean(Class<T> beanClass) {
    return context.getBean(beanClass);
  }

  /**
   * Get a Spring bean by name.
   *
   * @param beanName the name of the bean
   * @return the bean
   */
  public static Object getBean(String beanName) {
    return context.getBean(beanName);
  }
}
