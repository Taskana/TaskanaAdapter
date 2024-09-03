package io.kadai.adapter.integration;

import com.zaxxer.hikari.HikariDataSource;
import io.kadai.KadaiConfiguration;
import io.kadai.adapter.systemconnector.camunda.api.impl.HttpHeaderProvider;
import io.kadai.classification.api.ClassificationService;
import io.kadai.classification.api.exceptions.ClassificationNotFoundException;
import io.kadai.classification.api.models.Classification;
import io.kadai.common.api.KadaiEngine;
import io.kadai.common.api.KadaiEngine.ConnectionManagementMode;
import io.kadai.common.test.security.JaasExtension;
import io.kadai.common.test.security.WithAccessId;
import io.kadai.impl.configuration.DbCleaner;
import io.kadai.task.api.TaskService;
import io.kadai.workbasket.api.WorkbasketPermission;
import io.kadai.workbasket.api.WorkbasketService;
import io.kadai.workbasket.api.WorkbasketType;
import io.kadai.workbasket.api.exceptions.WorkbasketNotFoundException;
import io.kadai.workbasket.api.models.Workbasket;
import io.kadai.workbasket.api.models.WorkbasketAccessItem;
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

/** Parent class for integrationtests for the KADAI-Adapter. */
@ExtendWith(JaasExtension.class)
@SuppressWarnings("checkstyle:LineLength")
abstract class AbsIntegrationTest {

  protected static KadaiEngine kadaiEngine;

  private static boolean isInitialised = false;

  @LocalServerPort private Integer port;

  @Value("${kadai.adapter.scheduler.run.interval.for.start.kadai.tasks.in.milliseconds}")
  protected long adapterTaskPollingInterval;

  @Value("${kadai.adapter.scheduler.run.interval.for.complete.referenced.tasks.in.milliseconds}")
  protected long adapterCompletionPollingInterval;

  @Value(
      "${kadai.adapter.scheduler.run.interval.for.check.finished.referenced.tasks.in.milliseconds}")
  protected long adapterCancelledClaimPollingInterval;

  @Value("${kadai.adapter.scheduler.run.interval.for.claim.referenced.tasks.in.milliseconds}")
  protected long adapterClaimPollingInterval;

  @Value(
      "${kadai.adapter.scheduler.run.interval.for.cancel.claim.referenced.tasks.in.milliseconds}")
  protected long adapterCancelPollingInterval;

  @Value("${adapter.polling.inverval.adjustment.factor}")
  protected double adapterPollingInvervalAdjustmentFactor;

  @Value(
      "${kadai.adapter.scheduler.run.interval.for.retries.and.blocking.taskevents.in.milliseconds}")
  protected long adapterRetryAndBlockingInterval;

  protected CamundaProcessengineRequester camundaProcessengineRequester;

  protected KadaiOutboxRequester kadaiOutboxRequester;

  protected TaskService taskService;

  @Resource(name = "camundaBpmDataSource")
  protected DataSource camundaBpmDataSource;

  private TestRestTemplate restTemplate;

  @Autowired private ProcessEngineConfiguration processEngineConfiguration;

  @Autowired private HttpHeaderProvider httpHeaderProvider;

  @Resource(name = "kadaiDataSource")
  private DataSource kadaiDataSource;

  @BeforeEach
  @WithAccessId(user = "admin")
  void setUp() throws Exception {
    // set up database connection staticly and only once.
    if (!isInitialised) {
      String schema =
          ((HikariDataSource) kadaiDataSource).getSchema() == null
              ? "KADAI"
              : ((HikariDataSource) kadaiDataSource).getSchema();
      // setup Kadai engine and clear Kadai database
      KadaiConfiguration kadaiConfiguration =
          new KadaiConfiguration.Builder(this.kadaiDataSource, false, schema)
              .initKadaiProperties()
              .build();

      kadaiEngine =
          KadaiEngine.buildKadaiEngine(
              kadaiConfiguration, ConnectionManagementMode.AUTOCOMMIT);

      DbCleaner cleaner = new DbCleaner();
      cleaner.clearDb(kadaiDataSource, DbCleaner.ApplicationDatabaseType.KADAI);
      cleaner.clearDb(camundaBpmDataSource, DbCleaner.ApplicationDatabaseType.CAMUNDA);

      isInitialised = true;
    }

    this.restTemplate =
        new TestRestTemplate(
            new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .requestFactory(HttpComponentsClientHttpRequestFactory.class));
    // set up camunda requester and kadaiEngine-Taskservice
    this.camundaProcessengineRequester =
        new CamundaProcessengineRequester(
            this.processEngineConfiguration.getProcessEngineName(),
            this.restTemplate,
            this.httpHeaderProvider);
    this.kadaiOutboxRequester =
        new KadaiOutboxRequester(this.restTemplate, this.httpHeaderProvider);
    this.taskService = kadaiEngine.getTaskService();

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
        ((HikariDataSource) kadaiDataSource).getSchema() == null
            ? "KADAI"
            : ((HikariDataSource) kadaiDataSource).getSchema();
    KadaiConfiguration kadaiConfiguration =
        new KadaiConfiguration.Builder(this.kadaiDataSource, false, schema)
            .initKadaiProperties()
            .build();

    KadaiEngine kadaiEngineUnsecure =
        KadaiEngine.buildKadaiEngine(kadaiConfiguration, ConnectionManagementMode.AUTOCOMMIT);

    createWorkbasket(kadaiEngineUnsecure, "GPK_KSC", "DOMAIN_A");
    createWorkbasket(kadaiEngineUnsecure, "GPK_B_KSC", "DOMAIN_B");
    createClassification(kadaiEngineUnsecure, "T6310", "DOMAIN_A");
    createClassification(kadaiEngineUnsecure, "L1050", "DOMAIN_A");
    createClassification(kadaiEngineUnsecure, "L110102", "DOMAIN_A");
    createClassification(kadaiEngineUnsecure, "T2000", "DOMAIN_A");
    createClassification(kadaiEngineUnsecure, "L1050", "DOMAIN_B");
  }

  void createWorkbasket(KadaiEngine engine, String workbasketKey, String domain)
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

  private void createWorkbasketAccessList(KadaiEngine engine, Workbasket wb) throws Exception {
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
      KadaiEngine engine, String classificationKey, String domain) throws Exception {
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
