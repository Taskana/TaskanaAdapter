package io.kadai.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;

import io.kadai.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import io.kadai.adapter.manager.AdapterManager;
import io.kadai.adapter.test.KadaiAdapterTestApplication;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.impl.configuration.DbCleaner;
import io.kadai.impl.configuration.DbCleaner.ApplicationDatabaseType;
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

@SpringBootTest(
    classes = KadaiAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
class CamundaTaskEventErrorHandlerTest extends AbsIntegrationTest {

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
    this.adapterManager
        .getSystemConnectors()
        .forEach(
            (name, connector) ->
                connector
                    .retrieveNewStartedReferencedTasks()
                    .forEach(
                        referencedTask ->
                            connector.kadaiTaskFailedToBeCreatedForNewReferencedTask(
                                referencedTask, testException)));

    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    CamundaTaskEvent camundaTaskEvent = kadaiOutboxRequester.getAllEvents().get(0);
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
                            connector.kadaiTaskFailedToBeCreatedForNewReferencedTask(
                                referencedTask, testException)));
    CamundaTaskEvent camundaTaskEvent = kadaiOutboxRequester.getAllEvents().get(0);
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
                            connector.kadaiTaskFailedToBeCreatedForNewReferencedTask(
                                referencedTask, testException)));
    CamundaTaskEvent camundaTaskEvent = kadaiOutboxRequester.getAllEvents().get(0);
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
                            connector.kadaiTaskFailedToBeCreatedForNewReferencedTask(
                                referencedTask, testException)));
    CamundaTaskEvent camundaTaskEvent = kadaiOutboxRequester.getAllEvents().get(0);
    JSONObject errorJson = new JSONObject(camundaTaskEvent.getError());

    assertThat(errorJson).hasToString(expectedErrorJson.toString());
  }
}
