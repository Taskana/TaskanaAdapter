package pro.taskana.adapter.camunda.outbox.rest.impl;

import pro.taskana.adapter.camunda.outbox.rest.OutboxRestService;
import pro.taskana.adapter.camunda.outbox.rest.dto.ReferencedTaskDTO;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.camunda.spin.Spin.*;

public class OutboxRestServiceImpl implements OutboxRestService {

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    public List<ReferencedTaskDTO> getCreateEvents() {

        List<ReferencedTaskDTO> referencedTaskDTOS = new ArrayList<ReferencedTaskDTO>();

        try (Connection connection = getConnection()) {

            ResultSet createEventsResultSet = getCreateEventsResultSet(connection);

            referencedTaskDTOS = getReferencedTaskDtos(createEventsResultSet, referencedTaskDTOS);

        } catch (Exception e) {

            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        return referencedTaskDTOS;
    }


    public void deleteEvents(String idsAsString) {


        try (Connection connection = getConnection()) {

            List<Integer> idsAsIntegers = getIdsAsIntegers(idsAsString);

            String DeleteEventsSqlWithoutPlaceholders = "DELETE FROM taskana_tables.event_store WHERE id in (%s)";
            String DeleteEventsSqlWithPlaceholders = String.format(DeleteEventsSqlWithoutPlaceholders, preparePlaceHolders(idsAsIntegers.size()));

            PreparedStatement preparedStatement = connection.prepareStatement(DeleteEventsSqlWithPlaceholders);
            setValues(preparedStatement, idsAsIntegers);
            preparedStatement.execute();


        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

    }

    private List<Integer> getIdsAsIntegers(String idsAsString) {


        String[] idsAsStringArray = idsAsString.trim().split("\\s*,\\s*");

        List<Integer> idsAsIntegers = new ArrayList<Integer>();
        for (String id : idsAsStringArray) {
            idsAsIntegers.add(Integer.parseInt(id));
        }

        return idsAsIntegers;
    }

    private String preparePlaceHolders(int length) {
        return String.join(",", Collections.nCopies(length, "?"));

    }

    private void setValues(PreparedStatement preparedStatement, List<Integer> ids) throws SQLException {
        for (int i = 0; i < ids.size(); i++) {
            preparedStatement.setObject(i + 1, ids.get(i));
        }
    }

    private ResultSet getCreateEventsResultSet(Connection connection) throws SQLException {

        String getCreateEventQuery = "SELECT id, payload FROM taskana_tables.event_store WHERE type = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(getCreateEventQuery);
        preparedStatement.setString(1, "create");
        ResultSet resultSet = preparedStatement.executeQuery();

        return resultSet;
    }

    private List<ReferencedTaskDTO> getReferencedTaskDtos(ResultSet resultSet, List<ReferencedTaskDTO> referencedTaskDTOS) throws SQLException {

        while (resultSet.next()) {

            String creationEventId = Integer.toString(resultSet.getInt(1));
            String referencedTaskJson = resultSet.getString(2);

            ReferencedTaskDTO referencedTaskDTO = JSON(referencedTaskJson).mapTo(ReferencedTaskDTO.class);
            referencedTaskDTO.setCreationEventId(creationEventId);
            referencedTaskDTOS.add(referencedTaskDTO);
        }

        return referencedTaskDTOS;
    }


    private Connection getConnection() {

        Connection connection = null;

        try (InputStream config = OutboxRestServiceImpl.class.getClassLoader().getResourceAsStream("config.properties")) {


            Properties properties = new Properties();

            properties.load(config);

            DataSource ds = (DataSource) new InitialContext().lookup(properties.getProperty("datasource"));

            connection = ds.getConnection();

        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, ex.getMessage(), ex);
        }

        return connection;
    }
}
