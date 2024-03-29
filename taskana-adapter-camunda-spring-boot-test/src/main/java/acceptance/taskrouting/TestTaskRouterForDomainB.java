package acceptance.taskrouting;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.taskana.common.api.TaskanaEngine;
import pro.taskana.spi.routing.api.TaskRoutingProvider;
import pro.taskana.task.api.models.Task;
import pro.taskana.workbasket.api.models.WorkbasketSummary;

/** This is a sample implementation of TaskRouter. */
public class TestTaskRouterForDomainB implements TaskRoutingProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestTaskRouterForDomainB.class);

  private TaskanaEngine theEngine;

  @Override
  public void initialize(TaskanaEngine taskanaEngine) {
    theEngine = taskanaEngine;
  }

  @Override
  public String determineWorkbasketId(Task task) {
    if ("DOMAIN_B".equals(task.getDomain())) {
      List<WorkbasketSummary> wbs =
          theEngine.getWorkbasketService().createWorkbasketQuery().domainIn("DOMAIN_B").list();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(String.format("TestTaskRouterForDomainB Routing to %s", wbs.get(0)));
      }
      return wbs.get(0).getId();
    } else {
      return null;
    }
  }
}
