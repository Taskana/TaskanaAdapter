package pro.taskana.adapter.example.wildfly;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;

/** This test class is configured to run with postgres DB. */
@RunWith(Arquillian.class)
public class TaskanaAdapterWildflyTest extends AbstractAccTest {

  @Deployment(testable = false)
  public static Archive<?> createTestArchive() {
    File[] files =
        Maven.resolver()
            .loadPomFromFile("pom.xml")
            .importCompileAndRuntimeDependencies()
            .resolve()
            .withTransitivity()
            .asFile();

    return ShrinkWrap.create(WebArchive.class, "TaskanaAdapter.war")
        .addPackages(true, "pro.taskana")
        .addAsResource("taskana.properties")
        .addAsResource("application.properties")
        .addAsWebInfResource("int-test-web.xml", "web.xml")
        .addAsWebInfResource("int-test-jboss-web.xml", "jboss-web.xml")
        .addAsLibraries(files);
  }

  @Test
  @RunAsClient
  public void deploy() throws Exception {
    assertThat(parseServerLog()).contains(
            "Caught exception while trying to retrieve CamundaTaskEvents"
                + " from system with URL http://localhost:7001"
    );
  }
}
