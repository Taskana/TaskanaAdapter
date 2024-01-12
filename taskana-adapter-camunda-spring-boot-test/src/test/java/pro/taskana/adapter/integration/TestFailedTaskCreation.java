package pro.taskana.adapter.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.hamcrest.Matchers.hasSize;
import static pro.taskana.utils.AwaitilityUtils.getDuration;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;
import pro.taskana.adapter.camunda.outbox.rest.model.CamundaTaskEvent;
import pro.taskana.adapter.test.TaskanaAdapterTestApplication;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.impl.configuration.DbCleaner;
import pro.taskana.impl.configuration.DbCleaner.ApplicationDatabaseType;

/** Test class to test failed task creation scenarios from camunda to TASKANA. */
@SpringBootTest(
    classes = TaskanaAdapterTestApplication.class,
    webEnvironment = WebEnvironment.DEFINED_PORT)
@AutoConfigureWebTestClient
@ExtendWith(JaasExtension.class)
@ContextConfiguration
@SuppressWarnings("checkstyle:LineLength")
class TestFailedTaskCreation extends AbsIntegrationTest {

  @AfterEach
  @WithAccessId(user = "taskadmin")
  void resetOutbox() {
    DbCleaner cleaner = new DbCleaner();
    cleaner.clearDb(camundaBpmDataSource, ApplicationDatabaseType.OUTBOX);
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_CountDownRetriesAndAddToFailedEvents_When_TaskCreationFailedInTaskana() {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    // retries still above 0
    await()
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .pollDelay(getDuration(adapterTaskPollingInterval))
        .until(() -> taskanaOutboxRequester.getFailedEvents(), hasSize(0));

    // adapter makes retries
    // retries = 0, no retries left
    await()
        .atMost(getDuration(adapterRetryAndBlockingInterval))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(() -> taskanaOutboxRequester.getFailedEvents(), hasSize(3));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_LogError_When_TaskCreationFailedInTaskana() {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    // retries still above 0
    await()
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .pollDelay(getDuration(adapterTaskPollingInterval))
        .until(() -> taskanaOutboxRequester.getFailedEvents(), hasSize(0));

    // adapter makes retries
    // retries = 0, no retries left
    List<CamundaTaskEvent> failedEvents =
        await()
            .atMost(getDuration(adapterRetryAndBlockingInterval))
            .with()
            .pollInterval(ONE_HUNDRED_MILLISECONDS)
            .until(() -> taskanaOutboxRequester.getFailedEvents(), hasSize(3));

    assertThat(failedEvents)
        .extracting(CamundaTaskEvent::getCamundaTaskId)
        .containsExactlyInAnyOrderElementsOf(camundaTaskIds);

    assertThat(failedEvents)
        .extracting(CamundaTaskEvent::getError)
        .allMatch(
            error ->
                error.contains(
                    "{\"name\":"
                        + "\"pro.taskana.workbasket.api.exceptions.WorkbasketNotFoundException\","
                        + "\"message\":"
                        + "\"Workbasket with key 'invalidWorkbasketKey' "
                        + "and domain 'null' was not found.\"}"));
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_DeleteFailedEvent_When_CallingDeleteEndpoint() {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    // adapter makes retries
    List<CamundaTaskEvent> failedEvents =
        await()
            .atMost(getDuration((long) (adapterTaskPollingInterval * 1.2 + adapterRetryAndBlockingInterval)))
            .with()
            .pollInterval(ONE_HUNDRED_MILLISECONDS)
            .until(() -> taskanaOutboxRequester.getFailedEvents(), hasSize(3));

    boolean eventDeleted = taskanaOutboxRequester.deleteFailedEvent(failedEvents.get(0).getId());

    assertThat(eventDeleted).isTrue();

    assertThat(taskanaOutboxRequester.getFailedEvents()).hasSize(2);
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_DeleteAllFailedEvents_When_CallingDeleteAllFailedEndpoint() {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    // adapter makes retries
    await()
        .atMost(getDuration((long) (adapterTaskPollingInterval * 1.2 + adapterRetryAndBlockingInterval)))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(() -> taskanaOutboxRequester.getFailedEvents(), hasSize(3));

    boolean eventsDeleted = taskanaOutboxRequester.deleteAllFailedEvents();

    assertThat(eventsDeleted).isTrue();

    assertThat(taskanaOutboxRequester.getFailedEvents()).isEmpty();
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_SetRetryForFailedEvent_When_CallingSetRetriesEndpoint() {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    // adapter makes retries
    List<CamundaTaskEvent> failedEvents =
        await()
            .atMost(getDuration((long) (adapterTaskPollingInterval * 1.2 + adapterRetryAndBlockingInterval)))
            .with()
            .pollInterval(ONE_HUNDRED_MILLISECONDS)
            .until(() -> taskanaOutboxRequester.getFailedEvents(), hasSize(3));

    // reset specific failedEvent
    boolean remainingRetriesSet =
        taskanaOutboxRequester.setRemainingRetries(failedEvents.get(0).getId(), 3);

    assertThat(remainingRetriesSet).isTrue();

    failedEvents = taskanaOutboxRequester.getFailedEvents();

    assertThat(failedEvents).hasSize(2);
  }

  @WithAccessId(
      user = "teamlead_1",
      groups = {"taskadmin"})
  @Test
  void should_SetRetryForAllFailedEvents_When_CallingSetRetriesForAllFailedEndpoint() {

    String processInstanceId =
        this.camundaProcessengineRequester.startCamundaProcessAndReturnId(
            "simple_user_task_process_with_incorrect_workbasket_key", "");

    List<String> camundaTaskIds =
        this.camundaProcessengineRequester.getTaskIdsFromProcessInstanceId(processInstanceId);

    assertThat(camundaTaskIds).hasSize(3);

    // adapter makes retries
    await()
        .atMost(getDuration((long) (adapterTaskPollingInterval * 1.2 + adapterRetryAndBlockingInterval)))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(() -> taskanaOutboxRequester.getFailedEvents(), hasSize(3));

    // reset specific failedEvent
    boolean remainingRetriesSet = taskanaOutboxRequester.setRemainingRetriesForAll(3);

    assertThat(remainingRetriesSet).isTrue();

    assertThat(taskanaOutboxRequester.getFailedEvents()).isEmpty();
  }
}
