package pro.taskana.adapter.camunda.outbox.rest.springboot.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.jdbc.SqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class create the schema for the taskana adapter.
 */
public class OutboxSchemaCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxSchemaCreator.class);
    private static final String SQL = "/sql";
    private static final String DB_SCHEMA = SQL + "/outbox-schema.sql";
    private static final String DB_SCHEMA_DB2 = SQL + "/outbox-schema-db2.sql";
    private static final String DB_SCHEMA_POSTGRES = SQL + "/outbox-schema-postgres.sql";
    private static final String DB_SCHEMA_DETECTION = SQL + "/outbox-schema-detection.sql";
    private static final String DB_SCHEMA_DETECTION_POSTGRES = SQL + "/outbox-schema-detection-postgres.sql";

    private DataSource dataSource;
    private String schemaName;
    private String effectiveSchemaName;
    private boolean isPostGres;
    private StringWriter outWriter = new StringWriter();
    private PrintWriter logWriter = new PrintWriter(outWriter);
    private StringWriter errorWriter = new StringWriter();
    private PrintWriter errorLogWriter = new PrintWriter(errorWriter);

    public OutboxSchemaCreator(DataSource dataSource, String schemaName) {
        super();
        this.dataSource = dataSource;
        this.schemaName = schemaName;
        this.effectiveSchemaName = schemaName;
    }

    private static String selectDbScriptFileName(String dbProductName) {
        return "PostgreSQL".equals(dbProductName)
            ? DB_SCHEMA_POSTGRES
            : "H2".equals(dbProductName) ? DB_SCHEMA : DB_SCHEMA_DB2;
    }

    private static String selectDbSchemaDetectionScript(String dbProductName) {
        return "PostgreSQL".equals(dbProductName) ? DB_SCHEMA_DETECTION_POSTGRES : DB_SCHEMA_DETECTION;
    }

    /**
     * Run all db scripts.
     *
     * @throws SQLException
     *             will be thrown if there will be some incorrect SQL statements invoked.
     */
    public void run() throws SQLException {
        Connection connection = dataSource.getConnection();
        ScriptRunner runner = new ScriptRunner(connection);
        LOGGER.debug(connection.getMetaData().toString());
        String databaseProductName = connection.getMetaData().getDatabaseProductName();
        LOGGER.info("outbox uses database product {} ", databaseProductName);
        isPostGres = "PostgreSQL".equals(databaseProductName);
        effectiveSchemaName = isPostGres ? schemaName.toLowerCase() : schemaName.toUpperCase();

        runner.setStopOnError(true);
        runner.setLogWriter(logWriter);
        runner.setErrorLogWriter(errorLogWriter);
        try {
            if (!isSchemaPreexisting(runner, databaseProductName)) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass()
                    .getResourceAsStream(selectDbScriptFileName(databaseProductName))));
                runner.runScript(getSqlSchemaNameParsed(reader, databaseProductName));
            }
        } finally {
            runner.closeConnection();
        }
        LOGGER.debug(outWriter.toString());
        if (!errorWriter.toString().trim().isEmpty()) {
            LOGGER.error(errorWriter.toString());
        }
    }

    private boolean isSchemaPreexisting(ScriptRunner runner, String productName) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass()
                .getResourceAsStream(selectDbSchemaDetectionScript(productName))));
            runner.runScript(getSqlSchemaNameParsed(reader, productName));
        } catch (Exception e) {
            LOGGER.debug("Schema does not exist.");
            return false;
        }
        LOGGER.debug("Schema does exist.");
        return true;
    }

    public boolean isValidSchemaVersion(String expectedVersion) {
        SqlRunner runner = null;
        try {
            Connection connection = dataSource.getConnection();
            // connection.setSchema(this.schemaName);

            runner = new SqlRunner(connection);
            LOGGER.debug(connection.getMetaData().toString());

            String query = "select VERSION from " + effectiveSchemaName + ".OUTBOX_SCHEMA_VERSION where "
                + "VERSION = (select max(VERSION) from TASKANA_TABLES.OUTBOX_SCHEMA_VERSION) "
                + "AND VERSION = ?";

            Map<String, Object> queryResult = runner.selectOne(query, expectedVersion);
            if (queryResult == null || queryResult.isEmpty()) {
                LOGGER.error(
                    "Schema version not valid. The VERSION property in table TASKANA_SCHEMA_VERSION has not the expected value {}",
                    expectedVersion);
                return false;
            } else {
                LOGGER.debug("Schema version is valid.");
                return true;
            }

        } catch (Exception e) {
            LOGGER.error(
                "Schema version not valid. The VERSION property in table TASKANA_SCHEMA_VERSION has not the expected value {}",
                expectedVersion);
            return false;
        } finally {
            if (runner != null) {
                runner.closeConnection();
            }
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    private StringReader getSqlSchemaNameParsed(BufferedReader reader, String dbProductName) {
        StringBuffer content = new StringBuffer();
        try {
            String line = "";
            while (line != null) {
                line = reader.readLine();
                if (line != null) {
                    content.append(line.replaceAll("%schemaName%", effectiveSchemaName) + System.lineSeparator());
                }
            }
        } catch (IOException e) {
            LOGGER.error("SchemaName sql parsing failed for schemaName {}. Caught {}", effectiveSchemaName, e);
        }
        return new StringReader(content.toString());
    }
}
