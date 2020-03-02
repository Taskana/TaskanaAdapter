package pro.taskana.adapter.camunda.exceptions;

/**
 * This exception will be thrown if the database name doesn't match to one of the desired databases.
 */
public class UnsupportedDatabaseException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UnsupportedDatabaseException(String name) {
    super(String.format("Database with name %s not supported", name));
  }
}
