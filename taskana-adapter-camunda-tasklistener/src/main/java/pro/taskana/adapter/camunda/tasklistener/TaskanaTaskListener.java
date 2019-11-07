package pro.taskana.adapter.camunda.tasklistener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskanaTaskListener implements TaskListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskanaTaskListener.class);

    private static final String SQL_INSERT_CREATE_EVENT = "INSERT INTO event_store (TYPE,CREATED,PAYLOAD) VALUES (?,?,?)";

    private static TaskanaTaskListener instance = null;

    public static TaskanaTaskListener getInstance() {
        if (instance == null) {
            instance = new TaskanaTaskListener();
        }
        return instance;
    }

    @Override
    public void notify(DelegateTask delegateTask) {

        try (Connection connection = Context.getProcessEngineConfiguration().getDataSource().getConnection()) {

            switch (delegateTask.getEventName()) {

                case "create":
                    insertCreateEvent(delegateTask, connection);
                    break;
                case "complete":
                    insertCompleteEvent(delegateTask, connection);
                    break;
                case "delete":
                    insertDeleteEvent(delegateTask, connection);
                    break;
            }

        } catch (Exception e) {

            LOGGER.warn("Caught {} while trying to process a delegate task", e);
        }

    }

    private void insertCreateEvent(DelegateTask delegateTask, Connection connection) {

        try {
            LOGGER.debug("TaskanaTaskListener uses Database with url {} ", connection.getMetaData().getURL());

            String camundaSchema = connection.getSchema();
            LOGGER.debug("camundaSchema in taskListener is {}", camundaSchema);
            String dbProductName = connection.getMetaData().getDatabaseProductName();
            if ("PostgreSQL".equals(dbProductName)) {
                connection.setSchema("taskana_tables");
            } else {
                connection.setSchema("TASKANA_TABLES");
            }

            Timestamp eventCreationTimestamp = Timestamp.from(Instant.now());
            String referencedTaskJson = getReferencedTaskJson(delegateTask);

            PreparedStatement preparedStatement = connection.prepareStatement(SQL_INSERT_CREATE_EVENT,
                Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, delegateTask.getEventName());
            preparedStatement.setTimestamp(2, eventCreationTimestamp);
            preparedStatement.setString(3, referencedTaskJson);

            preparedStatement.execute();
            preparedStatement.close();

            connection.setSchema(camundaSchema);
        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to insert a create event into the outbox table", e);

        }
    }

    private String getReferencedTaskJson(DelegateTask delegateTask) {

        LOGGER.debug("getting referenced task for id {}" + delegateTask.getId());

        String referencedTaskJson = "{" +
            "\"id\":" + "\"" + delegateTask.getId() + "\"," +
            "\"name\":" + "\"" + delegateTask.getName() + "\"," +
            "\"assignee\":" + "\"" + delegateTask.getAssignee() + "\"," +
            "\"created\":" + "\"" + formatDate(delegateTask.getCreateTime()) + "\"," +
            "\"due\":" + "\"" + formatDate(delegateTask.getDueDate()) + "\"," +
            "\"description\":" + "\"" + delegateTask.getDescription() + "\"," +
            "\"owner\":" + "\"" + delegateTask.getOwner() + "\"," +
            "\"priority\":" + "\"" + delegateTask.getPriority() + "\"," +
            "\"taskDefinitionKey\":" + "\"" + delegateTask.getTaskDefinitionKey() + "\"," +
            "\"classificationKey\":" + "\"" + getClassification(delegateTask) + "\"," +
            "\"domain\":" + "\"" + getDomain(delegateTask) + "\"" +
            "}";

        return referencedTaskJson;
    }

    private String getDomain(DelegateTask delegateTask) {

        String domain = null;

        BpmnModelInstance model = delegateTask.getExecution().getBpmnModelInstance();

        try {
            domain = model.getModelElementsByType(CamundaProperty.class)
                .stream()
                .filter(camundaProperty -> camundaProperty.getCamundaName()
                    .equals("domain"))
                .collect(Collectors.toList())
                .get(0)
                .getCamundaValue();
        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to retrieve the \"domain\" property from task {}", e,
                delegateTask.getName());

        }

        return domain;

    }

    private String getClassification(DelegateTask delegateTask) {

        String classification = null;

        try {
            CamundaProperties camundaProperties = delegateTask.getExecution()
                .getBpmnModelElementInstance()
                .getExtensionElements()
                .getElementsQuery()
                .filterByType(CamundaProperties.class)
                .singleResult();

            classification = camundaProperties.getCamundaProperties()
                .stream()
                .filter(camundaProperty -> camundaProperty.getCamundaName()
                    .equals("classification"))
                .collect(Collectors.toList())
                .get(0)
                .getCamundaValue();

        } catch (Exception e) {
            LOGGER.warn("Caught {} while trying to retrieve the classification of task {}", e, delegateTask.getName());
        }

        return classification;
    }

    // TO-DO
    private void insertDeleteEvent(DelegateTask delegateTask, Connection connection) {

    }

    // TO-DO
    private void insertCompleteEvent(DelegateTask delegateTask, Connection connection) {

    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        } else {
            return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
                .withZone(ZoneId.systemDefault())
                .format(date.toInstant());

        }
    }
}