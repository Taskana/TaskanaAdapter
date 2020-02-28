package pro.taskana.sample.taskrouting;

import java.security.SecureRandom;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.spi.routing.api.TaskRoutingProvider;
import pro.taskana.task.api.models.Task;
import pro.taskana.workbasket.api.models.WorkbasketSummary;

public class ExampleTaskRoutingProviderForAdapterTest implements TaskRoutingProvider {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ExampleTaskRoutingProviderForAdapterTest.class);

  TaskanaEngine theEngine;

  @Value("${taskana.sample.taskrouter.random:false}")
  private String routeRandomly;

  @Override
  public void initialize(TaskanaEngine taskanaEngine) {
    theEngine = taskanaEngine;
  }

  @Override
  public String determineWorkbasketId(Task task) {
    if (routeRandomly != null && "true".equalsIgnoreCase(routeRandomly)) {
      return determineRandomWorkbasket();
    } else {
      return "WBI:100000000000000000000000000000000001";
    }
  }

  private String determineRandomWorkbasket() {
    List<WorkbasketSummary> wbs = theEngine.getWorkbasketService().createWorkbasketQuery().list();
    if (wbs != null && !(wbs.isEmpty())) {
      // select a random workbasket
      SecureRandom random = new SecureRandom();
      int n = random.nextInt(wbs.size());
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(String.format("ExampleTaskRoutingProvider Routs to %s", wbs.get(n)));
      }
      return wbs.get(n).getId();
    } else {
      return null;
    }
  }
}
