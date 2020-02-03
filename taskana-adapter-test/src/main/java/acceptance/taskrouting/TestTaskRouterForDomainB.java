package acceptance.taskrouting;

import java.util.List;

import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.task.api.Task;
import pro.taskana.task.api.TaskRoutingProvider;
import pro.taskana.workbasket.api.WorkbasketSummary;


/** This is a sample implementation of TaskRouter. */
public class TestTaskRouterForDomainB implements TaskRoutingProvider {

  TaskanaEngine theEngine;

  @Override
  public void initialize(TaskanaEngine taskanaEngine) {
    theEngine = taskanaEngine;
  }

  @Override
  public String determineWorkbasketId(Task task) {
    if ("DOMAIN_B".equals(task.getDomain())) {
      List<WorkbasketSummary> wbs =
          theEngine.getWorkbasketService().createWorkbasketQuery().domainIn("DOMAIN_B").list();
      System.out.println("TestTaskRouterForDomainB Routing to " + wbs.get(0));
      return wbs.get(0).getId();
    } else {
      return null;
    }
  }
}
