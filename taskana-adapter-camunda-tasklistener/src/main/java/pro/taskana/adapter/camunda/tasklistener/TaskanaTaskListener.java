package pro.taskana.adapter.camunda.tasklistener;

import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.context.Context;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;

import java.sql.*;
import java.time.Instant;
import java.util.Collection;
import java.util.logging.Logger;

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

        try {

            Connection connection = Context.getProcessEngineConfiguration().getDataSource().getConnection();

            try {
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

            } finally {
                connection.close();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void insertCreateEvent(DelegateTask delegateTask, Connection connection) {

        try {

            String camundaSchema = connection.getSchema();

            connection.setSchema("taskana_tables");


            String insertCreateEventSql = "INSERT INTO event_store (TYPE,CREATED,PAYLOAD) VALUES (?,?,?)";
            Timestamp eventCreationTimestamp = Timestamp.from(Instant.now());

            /** this gives us the userTask extension Properties, not yet used
             *
            CamundaProperties camundaProperties = delegateTask.getExecution().getBpmnModelElementInstance().getExtensionElements().getElementsQuery().filterByType(CamundaProperties.class).singleResult();
            Collection<CamundaProperty> properties = camundaProperties.getCamundaProperties();

             */

            String referencedTaskJson = "{" +
                    "\"id\":" + "\""+ delegateTask.getId()+ "\"" + "," +
                    "\"name\":" + "\"" + delegateTask.getName() + "\"" + "," +
                    "\"assignee\":" + "\""+delegateTask.getAssignee()+"\"" + "," +
                    "\"created\":" + "\""+ delegateTask.getCreateTime()+ "\"" + "," +
                    "\"due\":" + "\"" + delegateTask.getDueDate() + "\"" + "," +
                    "\"description\":" + "\"" + delegateTask.getDescription() + "\"" + "," +
                    "\"owner\":" + "\"" + delegateTask.getOwner() + "\"" + "," +
                    "\"priority\":"+ "\"" + delegateTask.getPriority()+ "\"" + "," +
                    "\"taskDefinitionKey\":" + "\"" + delegateTask.getTaskDefinitionKey()+ "\""
                    + "}";

            PreparedStatement preparedStatement = connection.prepareStatement(insertCreateEventSql, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, delegateTask.getEventName());
            preparedStatement.setTimestamp(2, eventCreationTimestamp);
            preparedStatement.setString(3, referencedTaskJson);


            preparedStatement.execute();

            connection.setSchema(camundaSchema);


        } catch (SQLException e) {

            e.printStackTrace();
        }
    }

    //TO-DO
    private void insertDeleteEvent(DelegateTask delegateTask, Connection connection) {

    }

    //TO-DO
    private void insertCompleteEvent(DelegateTask delegateTask, Connection connection) {

    }
}

