package pro.taskana.adapter.configuration;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionManager;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import pro.taskana.adapter.mappings.TimestampMapper;
import pro.taskana.exceptions.SystemException;
import pro.taskana.exceptions.UnsupportedDatabaseException;
import pro.taskana.impl.TaskanaEngineImpl;

/**
 * Configures the REST client.
 */
@Configuration
public class RestClientConfiguration {

    private DataSource dataSource;
    private SqlSessionManager sqlSessionManager;

    @Value("${taskanaAdapter.schemaName}")
    private String schemaName;

    @Bean
    public String schemaName() {
        return schemaName;
    }

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "datasource")
    public DataSourceProperties dataSourceProperties() {
        return  new DataSourceProperties();
//        DataSourceProperties props = new DataSourceProperties();
//        props.setUrl("jdbc:h2:mem:taskana;IGNORECASE=TRUE;LOCK_MODE=0;INIT=CREATE SCHEMA IF NOT EXISTS TASKANA");
//        return props;
    }

    @Bean
    public DataSource dataSource() {
        return getOrCreateDataSource();
    }

    private DataSource getOrCreateDataSource() {
        if (dataSource == null) {
            dataSource = dataSourceProperties().initializeDataSourceBuilder().build();
        }
        return dataSource;
    }

    @Bean
    TimestampMapper timestampMapper() {
        return getOrCreateSqlSessionManager().getMapper(TimestampMapper.class);
    }

    @Bean
    public SqlSessionManager sqlSessionManager() {
        return getOrCreateSqlSessionManager();
    }
    /**
     * This method creates the sqlSessionManager of myBatis. It integrates all the SQL mappers and sets the databaseId
     * attribute.
     *
     * @return a {@link SqlSessionFactory}
     */
    protected SqlSessionManager getOrCreateSqlSessionManager() {
        if (sqlSessionManager == null) {
            dataSource = getOrCreateDataSource();
            Environment environment = new Environment("taskanaAdapterEnvironment",  new SpringManagedTransactionFactory(),
                dataSource);
            org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration(environment);

            // set databaseId
            String databaseProductName;
            try (Connection con = dataSource.getConnection()) {
                databaseProductName = con.getMetaData().getDatabaseProductName();
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

            configuration.addMapper(TimestampMapper.class);
            SqlSessionFactory localSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
            sqlSessionManager = SqlSessionManager.newInstance(localSessionFactory);
        }
        return sqlSessionManager;
    }
}
