package pro.taskana.sample.taskrouting;

import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Value;

import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.spi.routing.api.TaskRoutingProvider;
import pro.taskana.task.api.Task;
import pro.taskana.workbasket.api.WorkbasketSummary;

public class ExampleTaskRoutingProviderForAdapterTest implements TaskRoutingProvider {

  TaskanaEngine theEngine;

  @Value("${taskana.sample.taskrouter.random:false}")
  private String routeRandomly;

  @Override
  public void initialize(TaskanaEngine taskanaEngine) {
    theEngine = taskanaEngine;
  }

  @Override
  public String determineWorkbasketId(Task task) {
    if (routeRandomly != null && "true".equals(routeRandomly.toLowerCase())) {
      return determineRandomWorkbasket();
    } else {
      return "WBI:100000000000000000000000000000000001";
    }
  }

  private String determineRandomWorkbasket() {
    List<WorkbasketSummary> wbs =
        theEngine.getWorkbasketService().createWorkbasketQuery().list();
    if (wbs != null && !(wbs.isEmpty())) {
      // select a random workbasket
      Random random = new Random();
      int n = random.nextInt(wbs.size());
      System.out.println("ExampleTaskRoutingProvider Routs to " + wbs.get(n));
      return wbs.get(n).getId();
    } else {
      return null;
    }
  }


}
