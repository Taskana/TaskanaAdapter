package pro.taskana.adapter.camunda.outbox.rest.springboot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;
import pro.taskana.adapter.camunda.outbox.rest.core.OutboxRestServiceCoreImpl;
import pro.taskana.adapter.camunda.outbox.rest.core.dto.ReferencedTaskDTO;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@RestController
public class OutboxRestServiceSpringBootRestControllerImpl implements OutboxRestServiceSpringBootRestController {

    @Autowired
    OutboxRestServiceCoreImpl outboxRestServiceCore;

    @Autowired
    DataSource dataSource;

    @Override
    public List<ReferencedTaskDTO> getCreateEvents() {

        try (Connection connection = dataSource.getConnection()){
            return outboxRestServiceCore.getCreateEvents(connection);

        } catch (SQLException e) {

        }

        return null;
    }

    @Override
    public void deleteEvents(String ids) {

        try (Connection connection = dataSource.getConnection()) {

            outboxRestServiceCore.deleteEvents(connection, ids);

        } catch (SQLException e){
            
        }
    }
}
