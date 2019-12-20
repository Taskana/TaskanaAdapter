package pro.taskana.adapter.camunda.outbox.rest.config;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import pro.taskana.adapter.camunda.outbox.rest.controller.CamundaTaskEventsController;

/**
 * Configures the outbox REST service.
 *
 */
@ApplicationPath("")
public class OutboxRestServiceConfig extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classesToBeScanned = new HashSet<Class<?>>();
        classesToBeScanned.add(CamundaTaskEventsController.class);
        return classesToBeScanned;
    }
}
