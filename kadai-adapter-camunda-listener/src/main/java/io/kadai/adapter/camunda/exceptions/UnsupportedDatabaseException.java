package io.kadai.adapter.camunda.exceptions;

/**
 * This exception will be thrown if the database name doesn't match to one of the desired databases.
 */
public class UnsupportedDatabaseException extends RuntimeException {

  public UnsupportedDatabaseException(String name) {
    super(String.format("Database with name %s not supported", name));
  }
}
