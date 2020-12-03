package pro.taskana.adapter.integration;

import com.zaxxer.hikari.HikariDataSource;
import javax.annotation.Resource;
import javax.sql.DataSource;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;

import pro.taskana.TaskanaEngineConfiguration;
import pro.taskana.classification.api.ClassificationService;
import pro.taskana.classification.api.exceptions.ClassificationAlreadyExistException;
import pro.taskana.classification.api.exceptions.ClassificationNotFoundException;
import pro.taskana.classification.api.models.Classification;
import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.common.api.TaskanaEngine.ConnectionManagementMode;
import pro.taskana.common.api.exceptions.DomainNotFoundException;
import pro.taskana.common.api.exceptions.InvalidArgumentException;
import pro.taskana.common.api.exceptions.NotAuthorizedException;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.impl.configuration.DbCleaner;
import pro.taskana.task.api.TaskService;
import pro.taskana.workbasket.api.WorkbasketPermission;
import pro.taskana.workbasket.api.WorkbasketService;
import pro.taskana.workbasket.api.WorkbasketType;
import pro.taskana.workbasket.api.exceptions.InvalidWorkbasketException;
import pro.taskana.workbasket.api.exceptions.WorkbasketAccessItemAlreadyExistException;
import pro.taskana.workbasket.api.exceptions.WorkbasketAlreadyExistException;
import pro.taskana.workbasket.api.exceptions.WorkbasketNotFoundException;
import pro.taskana.workbasket.api.models.Workbasket;
import pro.taskana.workbasket.api.models.WorkbasketAccessItem;

/** Parent class for integrationtests for the TASKANA-Adapter. */
@ExtendWith(JaasExtension.class)
@SuppressWarnings("checkstyle:LineLength")
public abstract class AbsIntegrationTest {

  protected static TaskanaEngine taskanaEngine;

  private static boolean isInitialised = false;

  @Value("${taskana.adapter.scheduler.run.interval.for.start.taskana.tasks.in.milliseconds}")
  protected long adapterTaskPollingInterval;

  @Value("${taskana.adapter.scheduler.run.interval.for.complete.referenced.tasks.in.milliseconds}")
  protected long adapterCompletionPollingInterval;

  @Value(
      "${taskana.adapter.scheduler.run.interval.for.check.finished.referenced."
          + "tasks.in.milliseconds}")
  protected long adapterCancelledClaimPollingInterval;

  @Value("${taskana.adapter.scheduler.run.interval.for.claim.referenced.tasks.in.milliseconds}")
  protected long adapterClaimPollingInterval;

  @Value(
      "${taskana.adapter.scheduler.run.interval.for.cancel.claim.referenced.tasks.in.milliseconds}")
  protected long adapterCancelPollingInterval;

  @Value("${adapter.polling.inverval.adjustment.factor}")
  protected double adapterPollingInvervalAdjustmentFactor;

  @Value(
      "${taskana.adapter.scheduler.run.interval.for.retries.and.blocking.taskevents.in.milliseconds}")
  protected long adapterRetryAndBlockingInterval;

  protected CamundaProcessengineRequester camundaProcessengineRequester;

  protected TaskanaOutboxRequester taskanaOutboxRequester;

  protected TaskService taskService;

  @Resource(name = "camundaBpmDataSource")
  protected DataSource camundaBpmDataSource;

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private ProcessEngineConfiguration processEngineConfiguration;

  @Resource(name = "taskanaDataSource")
  private DataSource taskanaDataSource;

  @BeforeEach
  @WithAccessId(user = "admin")
  public void setUp() throws Exception {
    // set up database connection staticly and only once.
    if (!isInitialised) {
      // setup Taskana engine and clear Taskana database
      TaskanaEngineConfiguration taskanaEngineConfiguration =
          new TaskanaEngineConfiguration(
              this.taskanaDataSource, false, ((HikariDataSource) taskanaDataSource).getSchema());

      taskanaEngine = taskanaEngineConfiguration.buildTaskanaEngine();
      taskanaEngine.setConnectionManagementMode(ConnectionManagementMode.AUTOCOMMIT);

      DbCleaner cleaner = new DbCleaner();
      cleaner.clearDb(taskanaDataSource, DbCleaner.ApplicationDatabaseType.TASKANA);
      cleaner.clearDb(camundaBpmDataSource, DbCleaner.ApplicationDatabaseType.CAMUNDA);

      isInitialised = true;
    }

    // set up camunda requester and taskanaEngine-Taskservice
    this.camundaProcessengineRequester =
        new CamundaProcessengineRequester(
            this.processEngineConfiguration.getProcessEngineName(), this.restTemplate);
    this.taskanaOutboxRequester = new TaskanaOutboxRequester(this.restTemplate);
    this.taskService = taskanaEngine.getTaskService();

    // adjust polling interval, give adapter a little more time
    this.adapterTaskPollingInterval =
        (long) (this.adapterTaskPollingInterval * adapterPollingInvervalAdjustmentFactor);
    this.adapterCompletionPollingInterval =
        (long) (this.adapterCompletionPollingInterval * adapterPollingInvervalAdjustmentFactor);
    this.adapterCancelPollingInterval =
        (long) (this.adapterCancelPollingInterval * adapterPollingInvervalAdjustmentFactor);
    initInfrastructure();
  }

  public void initInfrastructure() throws Exception {
    // create workbaskets and classifications needed by the test cases.
    // since this is no testcase we cannot set a JAAS context. To be able to create workbaskets
    // and classifications anyway we use for this purpose an engine with security disabled ...
    TaskanaEngineConfiguration taskanaEngineConfiguration =
        new TaskanaEngineConfiguration(
            this.taskanaDataSource,
            false,
            true,
            ((HikariDataSource) taskanaDataSource).getSchema());

    TaskanaEngine taskanaEngineUnsecure = taskanaEngineConfiguration.buildTaskanaEngine();
    taskanaEngineUnsecure.setConnectionManagementMode(ConnectionManagementMode.AUTOCOMMIT);

    createWorkbasket(taskanaEngineUnsecure, "GPK_KSC", "DOMAIN_A");
    createWorkbasket(taskanaEngineUnsecure, "GPK_B_KSC", "DOMAIN_B");
    createClassification(taskanaEngineUnsecure, "T6310", "DOMAIN_A");
    createClassification(taskanaEngineUnsecure, "L1050", "DOMAIN_A");
    createClassification(taskanaEngineUnsecure, "L110102", "DOMAIN_A");
    createClassification(taskanaEngineUnsecure, "T2000", "DOMAIN_A");
    createClassification(taskanaEngineUnsecure, "L1050", "DOMAIN_B");
  }

  public void createWorkbasket(TaskanaEngine engine, String workbasketKey, String domain)
      throws NotAuthorizedException, DomainNotFoundException, InvalidWorkbasketException,
          WorkbasketAlreadyExistException, WorkbasketNotFoundException, InvalidArgumentException,
          WorkbasketAccessItemAlreadyExistException {
    WorkbasketService workbasketService = engine.getWorkbasketService();
    Workbasket wb;
    try {
      wb = workbasketService.getWorkbasket(workbasketKey, domain);
    } catch (WorkbasketNotFoundException e) {
      wb = workbasketService.newWorkbasket(workbasketKey, domain);
      wb.setName(workbasketKey);
      wb.setOwner("teamlead_1");
      wb.setType(WorkbasketType.PERSONAL);
      wb = workbasketService.createWorkbasket(wb);
      createWorkbasketAccessList(engine, wb);
    }
  }

  private void createWorkbasketAccessList(TaskanaEngine engine, Workbasket wb)
      throws WorkbasketNotFoundException, InvalidArgumentException, NotAuthorizedException,
          WorkbasketAccessItemAlreadyExistException {
    WorkbasketService workbasketService = engine.getWorkbasketService();
    WorkbasketAccessItem workbasketAccessItem =
        workbasketService.newWorkbasketAccessItem(wb.getId(), wb.getOwner());
    workbasketAccessItem.setAccessName(wb.getOwner());
    workbasketAccessItem.setPermission(WorkbasketPermission.APPEND, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.TRANSFER, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.READ, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.OPEN, true);
    workbasketAccessItem.setPermission(WorkbasketPermission.DISTRIBUTE, true);
    workbasketService.createWorkbasketAccessItem(workbasketAccessItem);
  }

  private Classification createClassification(
      TaskanaEngine engine, String classificationKey, String domain)
      throws DomainNotFoundException, ClassificationAlreadyExistException, NotAuthorizedException,
          InvalidArgumentException {
    ClassificationService myClassificationService = engine.getClassificationService();

    Classification classification;
    try {
      classification = myClassificationService.getClassification(classificationKey, domain);
    } catch (ClassificationNotFoundException e) {
      classification = myClassificationService.newClassification(classificationKey, domain, "TASK");
      classification.setServiceLevel("P1D");
      classification = myClassificationService.createClassification(classification);
    }
    return classification;
  }
}
