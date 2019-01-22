package pro.taskana.adapter.taskanaconnector.config;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import pro.taskana.ClassificationService;
import pro.taskana.TaskService;
import pro.taskana.TaskanaEngine;
import pro.taskana.WorkbasketService;
import pro.taskana.configuration.SpringTaskanaEngineConfiguration;
import pro.taskana.configuration.TaskanaEngineConfiguration;

@Configuration
@DependsOn(value= {"springContextProvider"})
public class TaskanaSystemConnectorConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaSystemConnectorConfiguration.class);


    @Value("${taskana.schemaName:TASKANA}")
    public String schemaName;

    @Value("${taskana.domains}")
    private String domainNames;

    @Value("${taskana.classification.types}")
    private String classificationTypeNames;

    @Value("${taskana.classification.categories}")
    private String categoryNames;


    @Value("${generateSampleData:true}")
    public boolean generateSampleData;


    @Bean
    @Primary
    @ConfigurationProperties(prefix = "datasource")
    public DataSourceProperties dataSourceProperties() {
        DataSourceProperties props = new DataSourceProperties();
        props.setUrl("jdbc:h2:mem:taskana;IGNORECASE=TRUE;LOCK_MODE=0;INIT=CREATE SCHEMA IF NOT EXISTS TASKANA");
        return props;
    }

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }

    @Bean
    public TaskService getTaskService(TaskanaEngine taskanaEngine) {
        return taskanaEngine.getTaskService();
    }

    @Bean
    public WorkbasketService getWorkbasketService(TaskanaEngine taskanaEngine) {
        return taskanaEngine.getWorkbasketService();
    }

    @Bean
    public ClassificationService getClassificationService(TaskanaEngine taskanaEngine) {
        return taskanaEngine.getClassificationService();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public TaskanaEngine taskanaEngine(TaskanaEngineConfiguration taskanaEngineConfiguration) {
        return taskanaEngineConfiguration.buildTaskanaEngine();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public SpringTaskanaEngineConfiguration taskanaEngineConfiguration(DataSource dataSource) throws SQLException {
//        ,
//        @Value("${taskana.domains}") final String domainNames,
//        @Value("${taskana.classification.types}") final String classificationTypeNames,
//        @Value("${taskana.classification.categories}") final String categoryNames) throws SQLException {
//        List<String> domains = initDomains(domainNames);
//        List<String> classificationTypes = initClassificationTypes(classificationTypeNames);
        SpringTaskanaEngineConfiguration configuration = new SpringTaskanaEngineConfiguration(dataSource, true, false, "TASKANA");
//        configuration.setDomains(domains);
//        configuration.setClassificationTypes(classificationTypes);
        return configuration;
    }

    private List<String> initDomains(String domainNames) {
        List<String> domains = new ArrayList<String>();
        if (domainNames != null && !domainNames.isEmpty()) {
            StringTokenizer st = new StringTokenizer(domainNames, ",");
            while (st.hasMoreTokens()) {
                domains.add(st.nextToken().trim().toUpperCase());
            }
        }
        LOGGER.info("Configured domains: {}", domains);
        return domains;
    }

    private List<String> initClassificationTypes(String classificationTypeNames) {
        List<String> classificationTypes = new ArrayList<String>();
        if (classificationTypeNames != null && !classificationTypeNames.isEmpty()) {
            StringTokenizer st = new StringTokenizer(classificationTypeNames, ",");
            while (st.hasMoreTokens()) {
                classificationTypes.add(st.nextToken().trim().toUpperCase());
            }
        }
        LOGGER.info("Configured classification types: {}", classificationTypes);
        return classificationTypes;
    }

    private List<String> initClassificationCategories(String categoryNames) {
        List<String> classificationCategories = new ArrayList<String>();
        if (categoryNames != null && !categoryNames.isEmpty()) {
            StringTokenizer st = new StringTokenizer(categoryNames, ",");
            while (st.hasMoreTokens()) {
                classificationCategories.add(st.nextToken().trim().toUpperCase());
            }
        }
        LOGGER.info("Configured classification categories: {}", classificationCategories);
        return classificationCategories;
    }

}
