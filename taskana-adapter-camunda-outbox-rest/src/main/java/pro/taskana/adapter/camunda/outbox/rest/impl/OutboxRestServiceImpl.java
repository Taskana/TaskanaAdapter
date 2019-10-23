package pro.taskana.adapter.camunda.outbox.rest.impl;

import com.google.gson.Gson;
import pro.taskana.adapter.camunda.outbox.rest.OutboxRestService;
import pro.taskana.adapter.camunda.outbox.rest.dto.ReferencedTaskDTO;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class OutboxRestServiceImpl implements OutboxRestService {

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    public List<ReferencedTaskDTO> getCreateEvents() {

        List<ReferencedTaskDTO> referencedTaskDTOS = new ArrayList<ReferencedTaskDTO>();

        try {

            Connection connection = getConnection();

            String getCreateEventQuery  = "SELECT id, payload FROM taskana_tables.event_store WHERE type = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(getCreateEventQuery);
            preparedStatement.setString(1, "create");
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {

                String creationEventId = Integer.toString(resultSet.getInt(1));
                String referencedTaskJson = resultSet.getString(2);

                Gson gson = new Gson();
                ReferencedTaskDTO referencedTaskDTO = gson.fromJson(referencedTaskJson, ReferencedTaskDTO.class);
                referencedTaskDTO.setCreationEventId(creationEventId);
                referencedTaskDTOS.add(referencedTaskDTO);
            }

            connection.close();

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return referencedTaskDTOS;
    }


    //Currently deleting only one specific event, will be changed to be able to process list and bulk delete
    public void deleteEvents(int id) {

        try {

            Connection connection = getConnection();

            String deleteEventSql = "DELETE FROM taskana_tables.event_store WHERE id = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(deleteEventSql);
            preparedStatement.setInt(1,id);
            preparedStatement.execute();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    Connection getConnection() {

        Connection connection = null;

        try {

            DataSource ds = (DataSource) new InitialContext().lookup("java:jboss/datasources/TaskanaTaskListenerDS");

            connection = ds.getConnection();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return connection;
    }
}
