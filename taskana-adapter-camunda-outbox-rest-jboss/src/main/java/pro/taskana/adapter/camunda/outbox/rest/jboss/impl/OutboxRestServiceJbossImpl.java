package pro.taskana.adapter.camunda.outbox.rest.jboss.impl;

import pro.taskana.adapter.camunda.outbox.rest.core.OutboxRestServiceCoreImpl;
import pro.taskana.adapter.camunda.outbox.rest.core.dto.ReferencedTaskDTO;
import pro.taskana.adapter.camunda.outbox.rest.jboss.OutboxRestServiceJboss;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class OutboxRestServiceJbossImpl implements OutboxRestServiceJboss {

    @Override
    public List<ReferencedTaskDTO> getCreateEvents() {

        List<ReferencedTaskDTO> referencedTaskDTOS = new ArrayList<ReferencedTaskDTO>();

        //Can we use dependency injection here?
        OutboxRestServiceCoreImpl outboxRestService = new OutboxRestServiceCoreImpl();

        try (Connection connection = getConnection()) {

            referencedTaskDTOS = outboxRestService.getCreateEvents(connection);

        } catch (Exception e) {

        }
        return referencedTaskDTOS;
    }

    @Override
    public void deleteEvents(String ids) {

        //Can we use dependency injection here?
        OutboxRestServiceCoreImpl outboxRestService = new OutboxRestServiceCoreImpl();

        try (Connection connection = getConnection()) {

            outboxRestService.deleteEvents(connection, ids);

        }catch (Exception e){

        }
    }

    private Connection getConnection() {

        Connection connection = null;

        try (InputStream config = OutboxRestServiceJbossImpl.class.getClassLoader().getResourceAsStream("datasource.properties")) {

            Properties properties = new Properties();
            properties.load(config);
            DataSource ds = (DataSource) new InitialContext().lookup(properties.getProperty("datasource"));
            connection = ds.getConnection();

        } catch (Exception ex) {
        }

        return connection;
    }
}
