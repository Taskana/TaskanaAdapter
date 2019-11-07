package pro.taskana.adapter.camunda.outbox.rest.springboot.config;

import java.sql.Connection;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import pro.taskana.adapter.camunda.outbox.rest.core.OutboxRestServiceCoreImpl;

@Configuration
public class OutboxRestServiceSpringBootConfig {

    private final Logger LOGGER = LoggerFactory.getLogger(OutboxRestServiceSpringBootConfig.class);
    private static final String OUTBOX_SCHEMA_VERSION = "0.0.1";

    @Resource(name = "camundaBpmDataSource")
    private DataSource camundaBpmDataSource;

    private String outboxSchemaName = "TASKANA_TABLES";

    @Bean
    public OutboxRestServiceCoreImpl outboxRestServiceCore() {
        return new OutboxRestServiceCoreImpl();
    }

    @PostConstruct
    private void initOutboxDatabase() {
        try {
            LOGGER.debug("### AdapterManager.initOutboxDatabase called");

            Connection connection = camundaBpmDataSource.getConnection();
            LOGGER.debug("### InitOutboxDatabase uses Database with url {} ", connection.getMetaData().getURL());

            String databaseProductName = connection.getMetaData().getDatabaseProductName();
            if ("PostgreSQL".equals(databaseProductName)) {
                this.outboxSchemaName = this.outboxSchemaName.toLowerCase();
            } else {
                this.outboxSchemaName = this.outboxSchemaName.toUpperCase();
            }
            LOGGER.info("starting AdapterSchemaCreator");
            OutboxSchemaCreator schemaCreator = new OutboxSchemaCreator(camundaBpmDataSource, outboxSchemaName);
            schemaCreator.run();
            LOGGER.info("AdapterSchemaCreator is done");

            if (!schemaCreator.isValidSchemaVersion(OUTBOX_SCHEMA_VERSION)) {
                throw new RuntimeException(
                    "The Database Schema Version doesn't match the expected version " + OUTBOX_SCHEMA_VERSION);
            }

        } catch (SQLException ex) {
            LOGGER.error("Caught {} when attempting to initialize the database", ex);
        } finally {
            LOGGER.debug("### AdapterManager.initOutboxDatabase returned");

        }
    }

}