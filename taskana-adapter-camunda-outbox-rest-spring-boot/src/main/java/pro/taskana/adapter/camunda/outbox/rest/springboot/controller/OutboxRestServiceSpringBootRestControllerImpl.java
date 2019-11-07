package pro.taskana.adapter.camunda.outbox.rest.springboot.controller;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import pro.taskana.adapter.camunda.outbox.rest.core.OutboxRestServiceCoreImpl;
import pro.taskana.adapter.camunda.outbox.rest.core.dto.ReferencedTaskDTO;

@RestController
public class OutboxRestServiceSpringBootRestControllerImpl implements OutboxRestServiceSpringBootRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxRestServiceCoreImpl.class);

    @Autowired
    OutboxRestServiceCoreImpl outboxRestServiceCore;

    @Resource(name = "camundaBpmDataSource")
    DataSource camundaBpmDataSource;

    @Override
    public List<ReferencedTaskDTO> getCreateEvents() {

        try (Connection connection = camundaBpmDataSource.getConnection()) {

            LOGGER.debug("### OutboxRestCtrl uses Database with url {} ", connection.getMetaData().getURL());
            LOGGER.debug("### OutboxRestCtrl uses Connection with schema {} ", connection.getSchema());

            return outboxRestServiceCore.getCreateEvents(connection);

        } catch (SQLException e) {
            LOGGER.warn("Caught {} while trying to retrieve create Events from the outbox table", e);

        }

        return null;
    }

    @Override
    public void deleteEvents(String ids) {

        try (Connection connection = camundaBpmDataSource.getConnection()) {

            outboxRestServiceCore.deleteEvents(connection, ids);

        } catch (SQLException e) {
            LOGGER.warn("Caught {} while trying to delete events from the outbox table", e);
        }
    }
}