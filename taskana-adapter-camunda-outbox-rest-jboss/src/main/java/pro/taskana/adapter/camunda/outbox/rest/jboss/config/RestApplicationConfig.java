package pro.taskana.adapter.camunda.outbox.rest.jboss.config;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * Used to bootstrap JAX-RS.  Otherwise this class is
 * not directly used.
 *
 */
@ApplicationPath("/rest")
public class RestApplicationConfig extends Application {
    // intentionally empty
}