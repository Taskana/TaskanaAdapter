package io.kadai.adapter.example.wildfly;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/** This test class is configured to run with postgres DB. */
@ExtendWith(ArquillianExtension.class)
class KadaiAdapterWildflyTest extends AbstractAccTest {

  @Deployment(testable = false)
  static Archive<?> createTestArchive() {
    File[] files =
        Maven.resolver()
            .loadPomFromFile("pom.xml")
            .importCompileAndRuntimeDependencies()
            .resolve()
            .withTransitivity()
            .asFile();

    return ShrinkWrap.create(WebArchive.class, "KadaiAdapter.war")
        .addPackages(true, "io.kadai")
        .addAsResource("kadai.properties")
        .addAsResource("application.properties")
        .addAsWebInfResource("int-test-web.xml", "web.xml")
        .addAsWebInfResource("int-test-jboss-web.xml", "jboss-web.xml")
        .addAsLibraries(files);
  }

  @Test
  @RunAsClient
  void should_HaveConnectionErrorInLogs_When_ApplicationIsDeployedCorrectly() throws Exception {
    assertThat(parseServerLog())
        .contains(
            "Caught exception while trying to retrieve CamundaTaskEvents"
                + " from system with URL http://localhost:7001");
  }
}
