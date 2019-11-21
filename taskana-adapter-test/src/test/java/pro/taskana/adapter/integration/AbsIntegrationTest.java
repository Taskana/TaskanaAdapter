package pro.taskana.adapter.integration;

import java.sql.SQLException;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.camunda.bpm.engine.ProcessEngineConfiguration;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import com.zaxxer.hikari.HikariDataSource;

import pro.taskana.TaskService;
import pro.taskana.TaskanaEngine;
import pro.taskana.TaskanaEngine.ConnectionManagementMode;
import pro.taskana.configuration.TaskanaEngineConfiguration;
import pro.taskana.impl.configuration.DBCleaner;

/**
 * Parent class for integrationtests for the taskana adapter.
 *
 * @author Ben Fuernrohr
 */
public abstract class AbsIntegrationTest {

    // use rules instead of running with SpringRunner to allow for running with JAASRunner
    @ClassRule
    public static final SpringClassRule springClassRule = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Value("${taskana.adapter.scheduler.run.interval.for.start.taskana.tasks.in.milliseconds}")
    protected long adapterTaskPollingInterval;

    @Value("${taskana.adapter.scheduler.run.interval.for.complete.referenced.tasks.in.milliseconds}")
    protected long adapterCompletionPollingInterval;

    @Value("${taskana.adapter.scheduler.run.interval.for.check.cancelled.referenced.tasks.in.milliseconds}")
    protected long adapterCancelPollingInterval;

    @Value("${adapter.polling.inverval.adjustment.factor}")
    protected double adapterPollingInvervalAdjustmentFactor;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    protected static TaskanaEngine taskanaEngine;

    protected CamundaProcessengineRequester camundaProcessengineRequester;

    protected TaskService taskService;

    private static boolean isInitialised = false;

    @Resource(name = "taskanaDataSource")
    private DataSource taskanaDataSource;

    @Resource(name = "camundaBpmDataSource")
    private DataSource camundaBpmDataSource;

    @Before
    public void setUp() throws SQLException {
        // set up database connection staticly and only once.
        if (!isInitialised) {
            // setup Taskana engine and clear Taskana database
            TaskanaEngineConfiguration taskanaEngineConfiguration = new TaskanaEngineConfiguration(
                this.taskanaDataSource, false,
                ((HikariDataSource) taskanaDataSource).getSchema());

            taskanaEngine = taskanaEngineConfiguration.buildTaskanaEngine();
            taskanaEngine.setConnectionManagementMode(ConnectionManagementMode.AUTOCOMMIT);

            DBCleaner cleaner = new DBCleaner();
            cleaner.clearDb(taskanaDataSource, DBCleaner.ApplicationDatabaseType.TASKANA);
            cleaner.clearDb(camundaBpmDataSource, DBCleaner.ApplicationDatabaseType.CAMUNDA);

            isInitialised = true;
        }
        // set up camunda requester and taskanaEngine-Taskservice
        this.camundaProcessengineRequester = new CamundaProcessengineRequester(
            this.processEngineConfiguration.getProcessEngineName(), this.restTemplate);
        this.taskService = taskanaEngine.getTaskService();

        // adjust polling interval, give adapter a little more time
        this.adapterTaskPollingInterval = (long) (this.adapterTaskPollingInterval
            * adapterPollingInvervalAdjustmentFactor);
        this.adapterCompletionPollingInterval = (long) (this.adapterCompletionPollingInterval
            * adapterPollingInvervalAdjustmentFactor);
        this.adapterCancelPollingInterval = (long) (this.adapterCancelPollingInterval
            * adapterPollingInvervalAdjustmentFactor);
    }
}
