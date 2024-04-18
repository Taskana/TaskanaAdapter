package pro.taskana.adapter.integration;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.Resource;
import javax.sql.DataSource;
import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import pro.taskana.TaskanaConfiguration;
import pro.taskana.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import pro.taskana.classification.api.ClassificationService;
import pro.taskana.classification.api.exceptions.ClassificationNotFoundException;
import pro.taskana.classification.api.models.Classification;
import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.common.api.TaskanaEngine.ConnectionManagementMode;
import pro.taskana.common.test.security.JaasExtension;
import pro.taskana.common.test.security.WithAccessId;
import pro.taskana.impl.configuration.DbCleaner;
import pro.taskana.task.api.TaskService;
import pro.taskana.workbasket.api.WorkbasketPermission;
import pro.taskana.workbasket.api.WorkbasketService;
import pro.taskana.workbasket.api.WorkbasketType;
import pro.taskana.workbasket.api.exceptions.WorkbasketNotFoundException;
import pro.taskana.workbasket.api.models.Workbasket;
import pro.taskana.workbasket.api.models.WorkbasketAccessItem;

/** Parent class for integrationtests for the TASKANA-Adapter. */
@ExtendWith(JaasExtension.class)
@SuppressWarnings("checkstyle:LineLength")
abstract class AbsIntegrationTest {

  protected static TaskanaEngine taskanaEngine;

  private static boolean isInitialised = false;

  @LocalServerPort private Integer port;

  @Value("${taskana.adapter.scheduler.run.interval.for.start.taskana.tasks.in.milliseconds}")
  protected long adapterTaskPollingInterval;

  @Value("${taskana.adapter.scheduler.run.interval.for.complete.referenced.tasks.in.milliseconds}")
  protected long adapterCompletionPollingInterval;

  @Value(
      "${taskana.adapter.scheduler.run.interval.for.check.finished.referenced.tasks.in.milliseconds}")
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

  private TestRestTemplate restTemplate;

  @Autowired private ProcessEngineConfiguration processEngineConfiguration;

  @Autowired private HttpHeaderProvider httpHeaderProvider;

  @Resource(name = "taskanaDataSource")
  private DataSource taskanaDataSource;

  @BeforeEach
  @WithAccessId(user = "admin")
  void setUp() throws Exception {
    // set up database connection staticly and only once.
    if (!isInitialised) {
      String schema =
          ((HikariDataSource) taskanaDataSource).getSchema() == null
              ? "TASKANA"
              : ((HikariDataSource) taskanaDataSource).getSchema();
      // setup Taskana engine and clear Taskana database
      TaskanaConfiguration taskanaConfiguration =
          new TaskanaConfiguration.Builder(this.taskanaDataSource, false, schema)
              .initTaskanaProperties()
              .build();

      taskanaEngine =
          TaskanaEngine.buildTaskanaEngine(
              taskanaConfiguration, ConnectionManagementMode.AUTOCOMMIT);

      DbCleaner cleaner = new DbCleaner();
      cleaner.clearDb(taskanaDataSource, DbCleaner.ApplicationDatabaseType.TASKANA);
      cleaner.clearDb(camundaBpmDataSource, DbCleaner.ApplicationDatabaseType.CAMUNDA);

      isInitialised = true;
    }

    this.restTemplate =
        new TestRestTemplate(
            new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .requestFactory(HttpComponentsClientHttpRequestFactory.class));
    // set up camunda requester and taskanaEngine-Taskservice
    this.camundaProcessengineRequester =
        new CamundaProcessengineRequester(
            this.processEngineConfiguration.getProcessEngineName(),
            this.restTemplate,
            this.httpHeaderProvider);
    this.taskanaOutboxRequester =
        new TaskanaOutboxRequester(this.restTemplate, this.httpHeaderProvider);
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

  void initInfrastructure() throws Exception {
    // create workbaskets and classifications needed by the test cases.
    // since this is no testcase we cannot set a JAAS context. To be able to create workbaskets
    // and classifications anyway we use for this purpose an engine with security disabled ...
    String schema =
        ((HikariDataSource) taskanaDataSource).getSchema() == null
            ? "TASKANA"
            : ((HikariDataSource) taskanaDataSource).getSchema();
    TaskanaConfiguration taskanaConfiguration =
        new TaskanaConfiguration.Builder(this.taskanaDataSource, false, schema)
            .initTaskanaProperties()
            .build();

    TaskanaEngine taskanaEngineUnsecure =
        TaskanaEngine.buildTaskanaEngine(taskanaConfiguration, ConnectionManagementMode.AUTOCOMMIT);

    createWorkbasket(taskanaEngineUnsecure, "GPK_KSC", "DOMAIN_A");
    createWorkbasket(taskanaEngineUnsecure, "GPK_B_KSC", "DOMAIN_B");
    createClassification(taskanaEngineUnsecure, "T6310", "DOMAIN_A");
    createClassification(taskanaEngineUnsecure, "L1050", "DOMAIN_A");
    createClassification(taskanaEngineUnsecure, "L110102", "DOMAIN_A");
    createClassification(taskanaEngineUnsecure, "T2000", "DOMAIN_A");
    createClassification(taskanaEngineUnsecure, "L1050", "DOMAIN_B");
  }

  void createWorkbasket(TaskanaEngine engine, String workbasketKey, String domain)
      throws Exception {
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

  private void createWorkbasketAccessList(TaskanaEngine engine, Workbasket wb) throws Exception {
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
      TaskanaEngine engine, String classificationKey, String domain) throws Exception {
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
