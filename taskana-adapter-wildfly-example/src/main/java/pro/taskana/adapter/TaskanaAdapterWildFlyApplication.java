package pro.taskana.adapter;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

import pro.taskana.adapter.configuration.AdapterConfiguration;
import pro.taskana.adapter.systemconnector.camunda.config.CamundaSystemConnectorConfiguration;
import pro.taskana.adapter.taskanaconnector.config.TaskanaSystemConnectorConfiguration;


/**
 * Example Application showing the implementation of taskana-adapter  for jboss application server.
 */
@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = {"pro.taskana.adapter","pro.taskana.adapter.configuration","pro.taskana.adapter.systemconnector.camunda.config","pro.taskana.adapter.taskanaconnector.config"})
// @ComponentScan(basePackages = {"pro.taskana.adapter","pro.taskana.adapter.configuration","pro.taskana"})

@Import({AdapterConfiguration.class, CamundaSystemConnectorConfiguration.class, TaskanaSystemConnectorConfiguration.class })
public class TaskanaAdapterWildFlyApplication extends SpringBootServletInitializer {

    @Value("${taskana.adapter.schemaName:TCA}")
    private String schemaName;

      public static void main(String[] args) {

        Enumeration<URL> urls;
        try {
            urls = TaskanaAdapterWildFlyApplication.class.getClassLoader().getResources("application.properties");
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                System.out.println("#### URL for appProps: " + url);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        SpringApplication.run(TaskanaAdapterWildFlyApplication.class, args);
    }

//     @Bean
//    PropertyPlaceholderConfigurer propertyPlaceholderConfigurer() {
//
//         PropertyPlaceholderConfigurer configurer= new PropertyPlaceholderConfigurer();
//         Resource appProps = new ClassPathResource("/WEB-INF/classes/application.properties");
//         try {
//             System.out.println("#### Resource.file = " + appProps.getFile());
//             System.out.println("#### Resource.uri  = " + appProps.getURI());
//             System.out.println("#### Resource.stri = " + appProps.toString());
//             System.out.println("#### Resource.isFile = " + appProps.isFile());
//      } catch (IOException e) {
//          // TODO Auto-generated catch block
//          e.printStackTrace();
//      }
//         configurer.setLocation(appProps);
//         return configurer;
//     }
//
//      <property name="location">
//          <value>/WEB-INF/classes/social.properties</value>
//      </property>
//  </bean>

//    @Bean
//    @Primary
//    @ConfigurationProperties(prefix = "datasource")
//    public DataSourceProperties dataSourceProperties() {
//        DataSourceProperties props = new DataSourceProperties();
//        props.setUrl("jdbc:h2:mem:taskana;IGNORECASE=TRUE;LOCK_MODE=0;INIT=CREATE SCHEMA IF NOT EXISTS " + schemaName);
//        return props;
//    }
//
//    @Bean
//    public DataSource dataSource(DataSourceProperties dsProperties) {
//        // First try to load Properties and get Datasource via jndi lookup
//        Context ctx;
//        DataSource dataSource;
//        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
//        try (InputStream propertyStream = classloader.getResourceAsStream("application.properties")) {
//            Properties properties = new Properties();
//            ctx = new InitialContext();
//            properties.load(propertyStream);
//            dataSource = (DataSource) ctx.lookup(properties.getProperty("datasource.jndi"));
//            return dataSource;
//        } catch (Exception e) {
//            LOGGER.error(
//                "Caught exception {} when attempting to start Taskana with Datasource from Jndi. Using default H2 datasource. ",
//                e);
//            return dsProperties.initializeDataSourceBuilder().build();
//        }
//    }



}
