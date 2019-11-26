package pro.taskana.adapter.camunda.schemacreator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TaskanaOutboxSchemaCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaOutboxSchemaCreator.class);
    private static final String SQL = "/sql";
    private static final String DB_SCHEMA = SQL + "/taskana-outbox-schema.sql";
    private static final String DB_SCHEMA_DB2 = SQL + "/taskana-outbox-schema-db2.sql";
    private static final String DB_SCHEMA_POSTGRES = SQL + "/taskana-outbox-schema-postgres.sql";


    private DataSource dataSource;
    private String schemaName;
    private StringWriter outWriter = new StringWriter();
    private PrintWriter logWriter = new PrintWriter(outWriter);
    private StringWriter errorWriter = new StringWriter();
    private PrintWriter errorLogWriter = new PrintWriter(errorWriter);

    public TaskanaOutboxSchemaCreator(DataSource dataSource, String schemaName) {
        super();
        this.dataSource = dataSource;
        this.schemaName = schemaName;
    }

    private static String selectDbScriptFileName(String dbProductName) {
        return "PostgreSQL".equals(dbProductName)
                ? DB_SCHEMA_POSTGRES
                : "H2".equals(dbProductName) ? DB_SCHEMA : DB_SCHEMA_DB2;
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
        runner.setStopOnError(true);
        runner.setLogWriter(logWriter);
        runner.setErrorLogWriter(errorLogWriter);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(this.getClass()
                    .getResourceAsStream(selectDbScriptFileName(databaseProductName))));
            runner.runScript(getSqlSchemaNameParsed(reader, databaseProductName));
        } finally {
            runner.closeConnection();
        }
        LOGGER.debug(outWriter.toString());
        if (!errorWriter.toString().trim().isEmpty()) {
            LOGGER.error(errorWriter.toString());
        }
    }

    private StringReader getSqlSchemaNameParsed(BufferedReader reader, String dbProductName) {
        boolean isPostGres = "PostgreSQL".equals(dbProductName);
        StringBuffer content = new StringBuffer();
        String effectiveSchemaName = isPostGres ? schemaName.toLowerCase() : schemaName.toUpperCase();
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