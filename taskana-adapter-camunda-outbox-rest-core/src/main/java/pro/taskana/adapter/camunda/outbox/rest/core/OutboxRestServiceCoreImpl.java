package pro.taskana.adapter.camunda.outbox.rest.core;


import pro.taskana.adapter.camunda.outbox.rest.core.dto.ReferencedTaskDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.camunda.spin.Spin.JSON;

public class OutboxRestServiceCoreImpl {

    private final Logger LOGGER = Logger.getLogger(this.getClass().getName());

    public List<ReferencedTaskDTO> getCreateEvents(Connection connection) {

        List<ReferencedTaskDTO> referencedTaskDTOS = new ArrayList<ReferencedTaskDTO>();

        try (ResultSet createEventsResultSet = getCreateEventsResultSet(connection)) {

            referencedTaskDTOS = getReferencedTaskDtos(createEventsResultSet, referencedTaskDTOS);

        } catch (Exception e) {

            LOGGER.log(Level.WARNING, e.getMessage(), e);
        }

        return referencedTaskDTOS;
    }


    public void deleteEvents(Connection connection, String idsAsString) {

        List<Integer> idsAsIntegers = getIdsAsIntegers(idsAsString);

        String DeleteEventsSqlWithoutPlaceholders = "DELETE FROM taskana_tables.event_store WHERE id in (%s)";
        String DeleteEventsSqlWithPlaceholders = String.format(DeleteEventsSqlWithoutPlaceholders, preparePlaceHolders(idsAsIntegers.size()));

        try (PreparedStatement preparedStatement = connection.prepareStatement(DeleteEventsSqlWithPlaceholders)) {

            setValues(preparedStatement, idsAsIntegers);
            preparedStatement.execute();

        } catch (Exception e) {

        }
    }

    private List<Integer> getIdsAsIntegers(String idsAsString) {

        String[] idsAsStringArray = idsAsString.trim().split("\\s*,\\s*");

        List<Integer> idsAsIntegers = new ArrayList<Integer>();

        for (String id : idsAsStringArray) {
            try {

                idsAsIntegers.add(Integer.parseInt(id));

            } catch (NumberFormatException e) {

            }
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

    private ResultSet getCreateEventsResultSet(Connection connection) {

        String getCreateEventQuery = "SELECT id, payload FROM taskana_tables.event_store WHERE type = ?";

        ResultSet resultSet = null;

        try {

            PreparedStatement preparedStatement = connection.prepareStatement(getCreateEventQuery);
            preparedStatement.setString(1, "create");
            resultSet = preparedStatement.executeQuery();

        } catch (Exception e) {

        }

        return resultSet;
    }

    private List<ReferencedTaskDTO> getReferencedTaskDtos(ResultSet rs, List<ReferencedTaskDTO> referencedTaskDTOS) throws SQLException {

        try (ResultSet resultSet = rs) {


            while (resultSet.next()) {

                String creationEventId = Integer.toString(resultSet.getInt(1));
                String referencedTaskJson = resultSet.getString(2);
                ReferencedTaskDTO referencedTaskDTO = JSON(referencedTaskJson).mapTo(ReferencedTaskDTO.class);
                referencedTaskDTO.setCreationEventId(creationEventId);

                referencedTaskDTOS.add(referencedTaskDTO);

            }

        } catch (Exception e) {

        }

        return referencedTaskDTOS;
    }

}
