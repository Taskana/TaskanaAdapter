
/**
package config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;


@Configuration
public class SimpleSpringBootTestConfig {


    @Bean
    @ConfigurationProperties(prefix = "spring.datasource")
    DataSource dataSource (){

        DataSource dataSource = DataSourceBuilder.create().build();

        System.out.println("################################################################"+dataSource.toString());

        return null;
    }
    @Bean
    public PlatformTransactionManager platformTransactionManager() {
        return new DataSourceTransactionManager(dataSource());
    }
}*/
