package pro.taskana.adapter.exceptions;

/**
 * This exception will be thrown if the database name doesn't match to one of the desired databases.
 */
public class UnsupportedDatabaseException extends RuntimeException {

  public UnsupportedDatabaseException(String name) {
    super("Database with '" + name + "' not found");
  }
}
