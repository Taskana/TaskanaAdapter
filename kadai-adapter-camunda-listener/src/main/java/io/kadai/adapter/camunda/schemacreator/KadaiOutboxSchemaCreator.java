package io.kadai.adapter.camunda.schemacreator;

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
import org.apache.ibatis.jdbc.RuntimeSqlException;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.jdbc.SqlRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Creates the outbox schema in the Camunda database. */
public class KadaiOutboxSchemaCreator {

  private static final Logger LOGGER = LoggerFactory.getLogger(KadaiOutboxSchemaCreator.class);
  private static final String POSTGRESQL = "PostgreSQL";
  private static final String H2 = "H2";
  private static final String SQL = "/sql";
  private static final String DB_SCHEMA = SQL + "/h2/kadai-outbox-schema.sql";
  private static final String DB_SCHEMA_DB2 = SQL + "/db2/kadai-outbox-schema-db2.sql";
  private static final String DB_SCHEMA_POSTGRES =
      SQL + "/postgres/kadai-outbox-schema-postgres.sql";
  private static final String DB_SCHEMA_ORACLE = SQL + "/oracle/kadai-outbox-schema-oracle.sql";

  private DataSource dataSource;
  private String schemaName;
  private StringWriter outWriter = new StringWriter();
  private PrintWriter logWriter = new PrintWriter(outWriter);
  private StringWriter errorWriter = new StringWriter();
  private PrintWriter errorLogWriter = new PrintWriter(errorWriter);

  public KadaiOutboxSchemaCreator(DataSource dataSource, String schemaName) {
    super();
    this.dataSource = dataSource;
    this.schemaName = schemaName;
  }

  /**
   * Run all db scripts.
   *
   * @return true if successful
   */
  public boolean createSchema() {
    try (Connection connection = dataSource.getConnection()) {
      ScriptRunner runner = new ScriptRunner(connection);
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(connection.getMetaData().toString());
      }
      runner.setStopOnError(true);
      runner.setLogWriter(logWriter);
      runner.setErrorLogWriter(errorLogWriter);
      final String databaseProductName = connection.getMetaData().getDatabaseProductName();
      BufferedReader reader =
          new BufferedReader(
              new InputStreamReader(
                  this.getClass()
                      .getResourceAsStream(selectDbScriptFileName(databaseProductName))));
      runner.runScript(getSqlSchemaNameParsed(reader, databaseProductName));

    } catch (Exception ex) {
      return false;
    } finally {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(outWriter.toString());
      }
      if (!errorWriter.toString().trim().isEmpty() && LOGGER.isErrorEnabled()) {
        LOGGER.error(errorWriter.toString());
      }
    }
    LOGGER.info("KadaiOutbox schema created successfully");
    return true;
  }

  public boolean isSchemaPreexisting() {

    try {

      Map<String, Object> queryResult = querySchema();

      if (queryResult == null || queryResult.isEmpty()) {
        LOGGER.debug("KadaiOutbox does not exist");
        return false;
      } else {
        LOGGER.debug("KadaiOutbox does exist.");
        return true;
      }

    } catch (RuntimeSqlException | SQLException e) {
      LOGGER.debug("KadaiOutbox schema doesn't exist. (Exception: " + e.getMessage() + ")");
      return false;
    }
  }

  public boolean isValidSchemaVersion(String expectedOutboxSchemaVersion) {

    try {

      Map<String, Object> queryResult = querySchema();

      if (queryResult == null || queryResult.isEmpty()) {
        LOGGER.error(
            "Schema version not valid. The VERSION property in table OUTBOX_SCHEMA_VERSION "
                + "has not the expected value {}",
            expectedOutboxSchemaVersion);
        return false;

      } else if (queryResult.get("VERSION").equals(expectedOutboxSchemaVersion)) {
        LOGGER.debug("Schema version is valid.");
        return true;
      }

    } catch (RuntimeSqlException | SQLException e) {
      LOGGER.error(
          "Schema version not valid. The VERSION property in table OUTBOX_SCHEMA_VERSION "
              + "has not the expected value {}",
          expectedOutboxSchemaVersion);
      return false;
    }

    return false;
  }

  private Map<String, Object> querySchema() throws SQLException {

    SqlRunner runner;

    try (Connection connection = dataSource.getConnection()) {

      final String originalSchema = connection.getSchema();

      connection.setSchema(this.schemaName);
      runner = new SqlRunner(connection);

      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(connection.getMetaData().toString());
      }

      String query =
          "select VERSION from OUTBOX_SCHEMA_VERSION where "
              + "VERSION = (select max(VERSION) from OUTBOX_SCHEMA_VERSION) ";

      try {
        Map<String, Object> queryResult = runner.selectOne(query);
        connection.setSchema(originalSchema);
        return queryResult;
      } finally {
        connection.setSchema(originalSchema);
      }
    }
  }

  private static String selectDbScriptFileName(String dbProductName) {
    if (POSTGRESQL.equals(dbProductName)) {
      return DB_SCHEMA_POSTGRES;
    } else if (H2.equals(dbProductName)) {
      return DB_SCHEMA;
    } else if (dbProductName != null && dbProductName.toLowerCase().startsWith("oracle")) {
      return DB_SCHEMA_ORACLE;
    } else {
      return DB_SCHEMA_DB2;
    }
  }

  private StringReader getSqlSchemaNameParsed(BufferedReader reader, String dbProductName) {
    boolean isPostGres = POSTGRESQL.equals(dbProductName);
    StringBuilder content = new StringBuilder();
    String effectiveSchemaName = isPostGres ? schemaName.toLowerCase() : schemaName.toUpperCase();
    try {
      String line = "";
      while (line != null) {
        line = reader.readLine();
        if (line != null) {
          content
              .append(line.replace("%schemaName%", effectiveSchemaName))
              .append(System.lineSeparator());
        }
      }
    } catch (IOException e) {
      LOGGER.error(
          "SchemaName sql parsing failed for schemaName {}. Caught exception",
          effectiveSchemaName,
          e);
    }
    return new StringReader(content.toString());
  }
}
