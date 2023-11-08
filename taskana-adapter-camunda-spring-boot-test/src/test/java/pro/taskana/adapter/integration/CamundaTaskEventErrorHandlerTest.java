package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import pro.taskana.adapter.manager.AdapterManager;
import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.impl.configuration.DbCleaner;
import pro.taskana.impl.configuration.DbCleaner.ApplicationDatabaseType;

@SpringBootTest(
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
public class CamundaTaskEventErrorHandlerTest extends AbsIntegrationTest {

  @Autowired private AdapterManager adapterManager;

  @BeforeEach
  void init() {
    adapterManager.init();
  }

  @AfterEach
  @WithAccessId(user = "taskadmin")
  void increaseCounter() {
    DbCleaner cleaner = new DbCleaner();
    cleaner.clearDb(camundaBpmDataSource, ApplicationDatabaseType.OUTBOX);
  }

  @Test
  void should_CreateErrorLogWithOneCause_When_ExceptionWithOneCauseOccurred() {
    Exception testException = new NumberFormatException("exception");
    Exception testCause = new NumberFormatException("cause");
    testException.initCause(testCause);
    final JSONObject expectedErrorJson =
        new JSONObject()
            .put(
                "exception",
                new JSONObject()
                    .put("name", testException.getClass().getName())
                    .put("message", testException.getMessage()))
            .put(
                "cause",
                new org.json.JSONArray()
                    .put(
                        new JSONObject()
                            .put("name", testCause.getClass().getName())
                            .put("message", testCause.getMessage())));

    // Start process with task to have an entry in OutboxDB
    this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
        "simple_user_task_process", "");
    System.out.println("Systemconnectors: " + this.adapterManager.getSystemConnectors().toString());
    this.adapterManager
        .getSystemConnectors()
        .forEach(
            (name, connector) ->
                connector
                    .retrieveNewStartedReferencedTasks()
                    .forEach(
                        referencedTask ->
                            connector.taskanaTaskFailedToBeCreatedForNewReferencedTask(
                                referencedTask, testException)));

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    CamundaTaskEvent camundaTaskEvent = taskanaOutboxRequester.getAllEvents().get(0);
    JSONObject errorJson = new JSONObject(camundaTaskEvent.getError());

    assertThat(errorJson).hasToString(expectedErrorJson.toString());
  }

  @Test
  void should_CutErrorLog_When_ExceptionCauseTreeIsTooLong() {
    final Exception testException = new NumberFormatException("exception");
    final Exception testCause = new NumberFormatException("cause");
    final Exception testCauseVeryLong = new NumberFormatException(StringUtils.repeat("x", 1000));
    testCause.initCause(testCauseVeryLong);
    testException.initCause(testCause);
    final JSONObject expectedErrorJson =
        new JSONObject()
            .put(
                "exception",
                new JSONObject()
                    .put("name", testException.getClass().getName())
                    .put("message", testException.getMessage()))
            .put(
                "cause",
                new org.json.JSONArray()
                    .put(
                        new JSONObject()
                            .put("name", testCause.getClass().getName())
                            .put("message", testCause.getMessage()))
                    .put("..."));

    // Start process with task to have an entry in OutboxDB
    this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
        "simple_user_task_process", "");
    this.adapterManager
        .getSystemConnectors()
        .forEach(
            (name, connector) ->
                connector
                    .retrieveNewStartedReferencedTasks()
                    .forEach(
                        referencedTask ->
                            connector.taskanaTaskFailedToBeCreatedForNewReferencedTask(
                                referencedTask, testException)));
    CamundaTaskEvent camundaTaskEvent = taskanaOutboxRequester.getAllEvents().get(0);
    JSONObject errorJson = new JSONObject(camundaTaskEvent.getError());

    assertThat(errorJson).hasToString(expectedErrorJson.toString());
  }

  @Test
  void should_CutErrorLogProperly_When_AddingDotDotDotToErrorLog() {
    final Exception testException =
        new Exception(
            "exception",
            new Exception(
                "cause",
                new Exception(
                    // We need an exception message with a length of 825 characters, so that the
                    // overall length of the output string is 999 characters. Adding "..." would
                    // yield into >1000, thus this exception should not be included in the errorLog
                    StringUtils.repeat("x", 825),
                    new NumberFormatException(StringUtils.repeat("x", 1000)))));
    final JSONObject expectedErrorJson =
        new JSONObject()
            .put(
                "exception",
                new JSONObject()
                    .put("name", testException.getClass().getName())
                    .put("message", testException.getMessage()))
            .put(
                "cause",
                new org.json.JSONArray()
                    .put(
                        new JSONObject()
                            .put("name", testException.getCause().getClass().getName())
                            .put("message", testException.getCause().getMessage()))
                    .put("..."));

    // Start process with task to have an entry in OutboxDB
    this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
        "simple_user_task_process", "");
    this.adapterManager
        .getSystemConnectors()
        .forEach(
            (name, connector) ->
                connector
                    .retrieveNewStartedReferencedTasks()
                    .forEach(
                        referencedTask ->
                            connector.taskanaTaskFailedToBeCreatedForNewReferencedTask(
                                referencedTask, testException)));
    CamundaTaskEvent camundaTaskEvent = taskanaOutboxRequester.getAllEvents().get(0);
    JSONObject errorJson = new JSONObject(camundaTaskEvent.getError());
    assertThat(errorJson).hasToString(expectedErrorJson.toString());
  }

  @Test
  void should_CreateErrorLogWithoutCause_When_ExceptionWithoutCauseOccurred() {
    Exception testException = new NumberFormatException("exception");
    JSONObject expectedErrorJson =
        new JSONObject()
            .put(
                "exception",
                new JSONObject()
                    .put("name", testException.getClass().getName())
                    .put("message", testException.getMessage()))
            .put("cause", new JSONArray());

    // Start process with task to have an entry in OutboxDB
    this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
        "simple_user_task_process", "");
    this.adapterManager
        .getSystemConnectors()
        .forEach(
            (name, connector) ->
                connector
                    .retrieveNewStartedReferencedTasks()
                    .forEach(
                        referencedTask ->
                            connector.taskanaTaskFailedToBeCreatedForNewReferencedTask(
                                referencedTask, testException)));
    CamundaTaskEvent camundaTaskEvent = taskanaOutboxRequester.getAllEvents().get(0);
    JSONObject errorJson = new JSONObject(camundaTaskEvent.getError());

    assertThat(errorJson).hasToString(expectedErrorJson.toString());
  }
}
