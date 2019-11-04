package pro.taskana.adapter.camunda.outbox.rest.springboot.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import pro.taskana.adapter.camunda.outbox.rest.core.OutboxRestServiceCoreImpl;
import pro.taskana.adapter.camunda.outbox.rest.core.dto.ReferencedTaskDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@RestController
public class OutboxRestServiceSpringBootRestControllerImpl implements OutboxRestServiceSpringBootRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRestServiceCoreImpl.class);

    @Autowired
    OutboxRestServiceCoreImpl outboxRestServiceCore;

    @Autowired
    DataSource outboxRestServiceDataSource;

    @Override
    public List<ReferencedTaskDTO> getCreateEvents() {

        try (Connection connection = outboxRestServiceDataSource.getConnection()){
            return outboxRestServiceCore.getCreateEvents(connection);

        } catch (SQLException e) {
            LOGGER.warn("Caught {} while trying to retrieve create Events from the outbox table",e);

        }

        return null;
    }

    @Override
    public void deleteEvents(String ids) {

        try (Connection connection = outboxRestServiceDataSource.getConnection()) {

            outboxRestServiceCore.deleteEvents(connection, ids);

        } catch (SQLException e){
            LOGGER.warn("Caught {} while trying to delete events from the outbox table",e);
        }
    }
}
