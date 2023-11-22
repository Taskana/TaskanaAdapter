package pro.taskana.camunda.spring.boot;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = CamundaSpringBootExample.class, webEnvironment = RANDOM_PORT)
class CamundaSpringBootExampleIntegrationTest {

  @Test
  void module_context_can_be_started() {
    // there is no test code here,
    // because creating the configuration to run this method is the actual test
  }
}
