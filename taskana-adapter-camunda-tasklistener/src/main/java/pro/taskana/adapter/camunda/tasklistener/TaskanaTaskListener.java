package pro.taskana.adapter.camunda.tasklistener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import java.sql.*;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class TaskanaTaskListener implements TaskListener {

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    private static TaskanaTaskListener instance = null;

    public static TaskanaTaskListener getInstance() {
        if (instance == null) {
            instance = new TaskanaTaskListener();
        }
        return instance;
    }

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

            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }
    }

    private void insertCreateEvent(DelegateTask delegateTask, Connection connection) throws Exception {

        String camundaSchema = connection.getSchema();
        connection.setSchema("taskana_tables");

        Timestamp eventCreationTimestamp = Timestamp.from(Instant.now());
        String referencedTaskJson = getReferencedTaskJson(delegateTask);

        String insertCreateEventSql = "INSERT INTO event_store (TYPE,CREATED,PAYLOAD) VALUES (?,?,?)";

        PreparedStatement preparedStatement = connection.prepareStatement(insertCreateEventSql, Statement.RETURN_GENERATED_KEYS);
        preparedStatement.setString(1, delegateTask.getEventName());
        preparedStatement.setTimestamp(2, eventCreationTimestamp);
        preparedStatement.setString(3, referencedTaskJson);

        preparedStatement.execute();

        connection.setSchema(camundaSchema);

    }

    private String getReferencedTaskJson(DelegateTask delegateTask) throws Exception {

        String referencedTaskJson = "{" +
                "\"id\":" + "\"" + delegateTask.getId() + "\"," +
                "\"name\":" + "\"" + delegateTask.getName() + "\"," +
                "\"assignee\":" + "\"" + delegateTask.getAssignee() + "\"," +
                "\"created\":" + "\"" + delegateTask.getCreateTime() + "\"," +
                "\"due\":" + "\"" + delegateTask.getDueDate() + "\"," +
                "\"description\":" + "\"" + delegateTask.getDescription() + "\"," +
                "\"owner\":" + "\"" + delegateTask.getOwner() + "\"," +
                "\"priority\":" + "\"" + delegateTask.getPriority() + "\"," +
                "\"taskDefinitionKey\":" + "\"" + delegateTask.getTaskDefinitionKey() + "\"," +
                "\"classificationKey\":" + "\"" + getClassification(delegateTask) + "\"," +
                "\"domain\":" + "\"" + getDomain(delegateTask) + "\""
                + "}";

        return referencedTaskJson;
    }

    private String getDomain(DelegateTask delegateTask) throws Exception {

            BpmnModelInstance model = delegateTask.getExecution().getBpmnModelInstance();

            String domain = model.getModelElementsByType(CamundaProperty.class).stream().filter(camundaProperty ->
            camundaProperty.getCamundaName()
                           .equals("domain"))
                           .collect(Collectors.toList())
                           .get(0)
                           .getCamundaValue();

            return domain;

    }

    private String getClassification(DelegateTask delegateTask) throws Exception {

        CamundaProperties camundaProperties = delegateTask.getExecution()
                                              .getBpmnModelElementInstance()
                                              .getExtensionElements()
                                              .getElementsQuery()
                                              .filterByType(CamundaProperties.class)
                                              .singleResult();

        String classification = camundaProperties.getCamundaProperties()
                                                 .stream()
                                                 .filter(camundaProperty -> camundaProperty.getCamundaName()
                                                         .equals("classification"))
                                                         .collect(Collectors.toList())
                                                         .get(0)
                                                         .getCamundaValue();

        return classification;
    }

    //TO-DO
    private void insertDeleteEvent(DelegateTask delegateTask, Connection connection) {

    }

    //TO-DO
    private void insertCompleteEvent(DelegateTask delegateTask, Connection connection) {

    }
}

