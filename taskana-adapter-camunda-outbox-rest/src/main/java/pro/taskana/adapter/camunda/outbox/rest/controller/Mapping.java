package pro.taskana.adapter.camunda.outbox.rest.controller;

/** Collection of Url to Controller mappings. */
public class Mapping {

  public static final String URL_EVENTS = "/events";
  public static final String URL_EVENT = "/{eventId}";
  public static final String URL_DELETE_EVENTS = "/delete";
  public static final String URL_DECREASE_REMAINING_RETRIES =
      URL_EVENT + "/decrease-remaining-retries";
  public static final String DELETE_FAILED_EVENTS = "/deleteFailedEvents";
  public static final String URL_COUNT_FAILED_EVENTS = "/count";
}
