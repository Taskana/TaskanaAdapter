package pro.taskana.adapter.configuration;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionManager;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Scope;

import pro.taskana.adapter.impl.ReferencedTaskCompleter;
import pro.taskana.adapter.impl.TaskanaTaskStarter;
import pro.taskana.adapter.impl.TaskanaTaskTerminator;
import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.mappings.AdapterMapper;
import pro.taskana.exceptions.SystemException;
import pro.taskana.exceptions.UnsupportedDatabaseException;
import pro.taskana.impl.TaskanaEngineImpl;

/**
 * Configures the adapter .
 */
@Configuration
public class AdapterConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdapterConfiguration.class);

    @Value("${.jndi-name:no-jndi-configured}")
    private String jndiName;

    @Bean(name = "adapterDataSource")
    @ConfigurationProperties(prefix = "taskana.adapter.datasource")
    public DataSource adapterDataSource() throws NamingException {
        if ("no-jndi-configured".equals(jndiName)) {
            return DataSourceBuilder.create().build();
        } else {
            Context ctx = new InitialContext();
            return (DataSource) ctx.lookup(jndiName);
        }
    }

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

    @Bean
    @DependsOn(value = {"adapterDataSource"})
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SqlSessionManager sqlSessionManager() throws NamingException {
        DataSource adapterDataSource = adapterDataSource();
        Environment environment = new Environment("taskanaAdapterEnvironment", new SpringManagedTransactionFactory(),
            adapterDataSource);
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration(
            environment);

        // set databaseId
        String databaseProductName;
        try (Connection con = adapterDataSource.getConnection()) {
            databaseProductName = con.getMetaData().getDatabaseProductName();
            LOGGER.info("adapterDataSource uses database product {} ", databaseProductName);
            if (TaskanaEngineImpl.isDb2(databaseProductName)) {

                configuration.setDatabaseId("db2");
            } else if (TaskanaEngineImpl.isH2(databaseProductName)) {
                configuration.setDatabaseId("h2");
            } else if (TaskanaEngineImpl.isPostgreSQL(databaseProductName)) {
                configuration.setDatabaseId("postgres");
            } else {
                throw new UnsupportedDatabaseException(databaseProductName);
            }

        } catch (SQLException e) {
            throw new SystemException(
                "Method createSqlSessionManager() could not open a connection to the database. No databaseId has been set.",
                e.getCause());
        }

        configuration.addMapper(AdapterMapper.class);
        SqlSessionFactory localSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        return SqlSessionManager.newInstance(localSessionFactory);
    }

}
