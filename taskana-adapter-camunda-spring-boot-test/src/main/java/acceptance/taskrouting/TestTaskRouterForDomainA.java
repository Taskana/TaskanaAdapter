package acceptance.taskrouting;

import java.util.List;

import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.spi.routing.api.TaskRoutingProvider;
import pro.taskana.task.api.models.Task;
import pro.taskana.workbasket.api.models.WorkbasketSummary;

/** This is a sample implementation of TaskRouter. */
public class TestTaskRouterForDomainA implements TaskRoutingProvider {

  TaskanaEngine theEngine;

  @Override
  public void initialize(TaskanaEngine taskanaEngine) {
    theEngine = taskanaEngine;
  }

  @Override
  public String determineWorkbasketId(Task task) {
    if ("DOMAIN_A".equals(task.getDomain())) {
      List<WorkbasketSummary> wbs =
          theEngine.getWorkbasketService().createWorkbasketQuery().domainIn("DOMAIN_A").list();
      System.out.println("TestTaskRouterForDomainA Routing to " + wbs.get(0));
      return wbs.get(0).getId();
    } else {
      return null;
    }
  }
}
