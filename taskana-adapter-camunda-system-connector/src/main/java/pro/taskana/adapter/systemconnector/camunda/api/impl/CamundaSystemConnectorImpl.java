package pro.taskana.adapter.systemconnector.camunda.api.impl;

import java.util.List;

import pro.taskana.adapter.configuration.AdapterSpringContextProvider;
import pro.taskana.adapter.systemconnector.api.ReferencedTask;
import pro.taskana.adapter.systemconnector.api.SystemConnector;
import pro.taskana.adapter.systemconnector.api.SystemResponse;
import pro.taskana.adapter.systemconnector.camunda.config.CamundaSystemUrls;

/** Sample Implementation of SystemConnector. */
public class CamundaSystemConnectorImpl implements SystemConnector {

  static final String URL_GET_CAMUNDA_TASKS = "/task/";

  static final String URL_GET_CAMUNDA_CREATE_EVENTS = "/events?type=create";
  static final String URL_GET_CAMUNDA_FINISHED_EVENTS = "/events?type=complete&type=delete";
  static final String URL_DELETE_CAMUNDA_EVENTS = "/events/delete-successful-events";
  static final String URL_CAMUNDA_EVENT_DECREASE_REMAINING_RETRIES =
      "/events/%d/decrease-remaining-retries";

  static final String BODY_SET_CAMUNDA_VARIABLES = "{\"variables\":{";
  static final String LOCAL_VARIABLE_PATH = "/localVariables";
  static final String EMPTY_REQUEST_BODY = "{}";

  static final String COMPLETE_TASK = "/complete/";
  static final String SET_ASSIGNEE = "/assignee/";
  static final String BODY_SET_ASSIGNEE = "{\"userId\":";
  static final String UNCLAIM_TASK = "/unclaim/";

  private CamundaSystemUrls.SystemUrlInfo camundaSystemUrl;

  private CamundaTaskRetriever taskRetriever;

  private CamundaTaskCompleter taskCompleter;

  private CamundaTaskClaimer taskClaimer;

  private CamundaTaskClaimCanceler taskClaimCanceler;

  private CamundaTaskEventCleaner taskEventCleaner;

  private CamundaTaskEventErrorHandler taskEventErrorHandler;

  public CamundaSystemConnectorImpl(CamundaSystemUrls.SystemUrlInfo camundaSystemUrl) {
    this.camundaSystemUrl = camundaSystemUrl;
    taskRetriever = AdapterSpringContextProvider.getBean(CamundaTaskRetriever.class);
    taskCompleter = AdapterSpringContextProvider.getBean(CamundaTaskCompleter.class);
    taskClaimer = AdapterSpringContextProvider.getBean(CamundaTaskClaimer.class);
    taskClaimCanceler = AdapterSpringContextProvider.getBean(CamundaTaskClaimCanceler.class);
    taskEventCleaner = AdapterSpringContextProvider.getBean(CamundaTaskEventCleaner.class);
    taskEventErrorHandler =
        AdapterSpringContextProvider.getBean(CamundaTaskEventErrorHandler.class);
  }

  @Override
  public List<ReferencedTask> retrieveNewStartedReferencedTasks() {
    return taskRetriever.retrieveNewStartedCamundaTasks(camundaSystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public void taskanaTasksHaveBeenCreatedForNewReferencedTasks(
      List<ReferencedTask> referencedTasks) {
    taskEventCleaner.cleanEventsForReferencedTasks(
        referencedTasks, camundaSystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public List<ReferencedTask> retrieveFinishedReferencedTasks() {
    return taskRetriever.retrieveFinishedCamundaTasks(camundaSystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public void taskanaTasksHaveBeenTerminatedForFinishedReferencedTasks(
      List<ReferencedTask> referencedTasks) {
    taskEventCleaner.cleanEventsForReferencedTasks(
        referencedTasks, camundaSystemUrl.getSystemTaskEventUrl());
  }

  @Override
  public String retrieveReferencedTaskVariables(String taskId) {
    return null;
  }

  @Override
  public SystemResponse completeReferencedTask(ReferencedTask camundaTask) {
    return taskCompleter.completeCamundaTask(camundaSystemUrl, camundaTask);
  }

  @Override
  public SystemResponse claimReferencedTask(ReferencedTask camundaTask) {
    return taskClaimer.claimCamundaTask(camundaSystemUrl, camundaTask);
  }

  @Override
  public SystemResponse cancelClaimReferencedTask(ReferencedTask camundaTask) {
    return taskClaimCanceler.cancelClaimOfCamundaTask(camundaSystemUrl, camundaTask);
  }

  @Override
  public String getSystemUrl() {
    return camundaSystemUrl.getSystemRestUrl();
  }

  @Override
  public void taskanaTaskFailedToBeCreatedForNewReferencedTask(
      ReferencedTask referencedTask, Exception e) {
    taskEventErrorHandler.decreaseRemainingRetriesAndLogErrorForReferencedTask(
        referencedTask, e, camundaSystemUrl.getSystemTaskEventUrl());
  }
}
