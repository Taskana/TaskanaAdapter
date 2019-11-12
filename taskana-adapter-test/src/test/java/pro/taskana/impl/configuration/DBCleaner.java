package pro.taskana.impl.configuration;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.ibatis.jdbc.ScriptRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to clean up databases. Can be used to clean Camunda, Taskana or Taskana-Adapter-Databases.
 */
public class DBCleaner {

    public enum ApplicationDatabaseType {
        TASKANA,
        TASKANA_ADAPTER,
        CAMUNDA,
        OUTBOX
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DBCleaner.class);

    private static final String TASKANA_DB_CLEAR_SCRIPT = "/sql/clear-taskana-db.sql";

    private static final String TASKANA_ADAPTER_DB_CLEAR_SCRIPT = "/sql/clear-taskana-adapter-db.sql";
    private static final String CAMUNDA_DB_CLEAR_SCRIPT = "/sql/clear-camunda-db.sql";
    private static final String OUTBOX_DB_CLEAR_SCRIPT = "/sql/clear-outbox-db.sql";

    private static final String TASKANA_ADAPTER_DB_CLEAR_POSTGRES = "/sql/clear-taskana-adapter-db-postgres.sql";
    private static final String OUTBOX_DB_CLEAR_POSTGRES = "/sql/clear-outbox-db-postgres.sql";

    private Map<ApplicationDatabaseType, String> typeScriptMap = new HashMap<ApplicationDatabaseType, String>();
    private Map<ApplicationDatabaseType, String> typeScriptMapPostgres = new HashMap<ApplicationDatabaseType, String>();

    private StringWriter outWriter = new StringWriter();
    private PrintWriter logWriter = new PrintWriter(outWriter);
    private StringWriter errorWriter = new StringWriter();
    private PrintWriter errorLogWriter = new PrintWriter(errorWriter);

    public DBCleaner() {
        this.typeScriptMap.put(ApplicationDatabaseType.TASKANA, TASKANA_DB_CLEAR_SCRIPT);
        this.typeScriptMap.put(ApplicationDatabaseType.TASKANA_ADAPTER, TASKANA_ADAPTER_DB_CLEAR_SCRIPT);
        this.typeScriptMap.put(ApplicationDatabaseType.CAMUNDA, CAMUNDA_DB_CLEAR_SCRIPT);
        this.typeScriptMap.put(ApplicationDatabaseType.OUTBOX, OUTBOX_DB_CLEAR_SCRIPT);

        this.typeScriptMapPostgres.put(ApplicationDatabaseType.TASKANA, TASKANA_DB_CLEAR_SCRIPT);
        this.typeScriptMapPostgres.put(ApplicationDatabaseType.TASKANA_ADAPTER, TASKANA_ADAPTER_DB_CLEAR_POSTGRES);
        this.typeScriptMapPostgres.put(ApplicationDatabaseType.CAMUNDA, CAMUNDA_DB_CLEAR_SCRIPT);
        this.typeScriptMapPostgres.put(ApplicationDatabaseType.OUTBOX, OUTBOX_DB_CLEAR_POSTGRES);

    }

    /**
     * Clears the db.
     *
     * @param dropTables
     *            if true drop tables, else clean tables
     */
    public void clearDb(DataSource dataSource, ApplicationDatabaseType applicationDatabaseType) {
        try (Connection connection = dataSource.getConnection()) {
            ScriptRunner runner = new ScriptRunner(connection);
            LOGGER.debug(connection.getMetaData().toString());

            runner.setStopOnError(false);
            runner.setLogWriter(logWriter);
            runner.setErrorLogWriter(errorLogWriter);

            String dbProductName = connection.getMetaData().getDatabaseProductName();
            String scriptName = this.typeScriptMap.get(applicationDatabaseType);
            if ("PostgreSQL".equals(dbProductName)) {
                scriptName = this.typeScriptMapPostgres.get(applicationDatabaseType);
            }
            LOGGER.debug("using script {} to clear database", scriptName);
            runner.runScript(new InputStreamReader(this.getClass().getResourceAsStream(scriptName)));

        } catch (Exception e) {
            LOGGER.error("caught Exception " + e);
        }
        LOGGER.debug(outWriter.toString());
        String errorMsg = errorWriter.toString().trim();

        if (!errorMsg.isEmpty() && errorMsg.indexOf("SQLCODE=-204, SQLSTATE=42704") == -1) {
            LOGGER.error(errorWriter.toString());
        }
    }
}