package pro.taskana.utils;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Durations.ONE_HUNDRED_MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.hasSize;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.logrecorder.api.LogRecord;
import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.Callable;
import org.camunda.bpm.engine.impl.jobexecutor.JobExecutor;
import pro.taskana.adapter.integration.CamundaProcessengineRequester;
import pro.taskana.task.api.models.TaskSummary;

public class AwaitilityUtils {

  private AwaitilityUtils() {
    // empty default constructor because all methods are static
  }

  public static Duration getDuration(long time) {
    return Duration.of(time * 2, MILLIS);
  }

  public static TaskSummary getTaskSummary(
      long adapterTaskPollingInterval, Callable<Collection<TaskSummary>> supplier) {
    return await()
        .atMost(getDuration(adapterTaskPollingInterval))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(supplier, hasSize(1))
        .iterator()
        .next();
  }

  public static String getCamundaTaskId(long pollingIntervall, Callable<String> supplier) {
    return await()
        .atMost(getDuration(pollingIntervall))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(supplier, notNullValue());
  }

  public static boolean verifyAssigneeForCamundaTask(
      long adapterClaimPollingInterval, Callable<Boolean> supplier) {
    return await()
        .atMost(getDuration(adapterClaimPollingInterval))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(supplier, is(true));
  }

  public static boolean verifyLogMessage(
      long pollingIntervall, ListAppender<ILoggingEvent> appender, String logMessage) {
    return await()
        .atMost(getDuration(pollingIntervall))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(
            () ->
                !appender.list.stream()
                    .filter(
                        loggingEvent ->
                            loggingEvent.getFormattedMessage().contentEquals(logMessage))
                    .toList()
                    .isEmpty(),
            is(true));
  }

  public static boolean verifyLogMessage(long pollingIntervall, LogRecord log, String logMessage) {
    return await()
        .atMost(getDuration(pollingIntervall))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(
            () ->
                !log.getMessages().stream()
                    .filter(message -> message.contentEquals(logMessage))
                    .toList()
                    .isEmpty(),
            is(true));
  }

  public static void checkCamundaTaskIsCompleted(
      JobExecutor jobExecutor,
      CamundaProcessengineRequester camundaProcessengineRequester,
      String camundaTaskId) {
    await()
        .atMost(getDuration(jobExecutor.getMaxWait()))
        .with()
        .pollInterval(ONE_HUNDRED_MILLISECONDS)
        .until(() -> camundaProcessengineRequester.getTaskFromTaskId(camundaTaskId), is(false));
  }
}
