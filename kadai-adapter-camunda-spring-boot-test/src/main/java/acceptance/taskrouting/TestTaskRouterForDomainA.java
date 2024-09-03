package acceptance.taskrouting;

import io.kadai.common.api.KadaiEngine;
import io.kadai.spi.routing.api.TaskRoutingProvider;
import io.kadai.task.api.models.Task;
import io.kadai.workbasket.api.models.WorkbasketSummary;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This is a sample implementation of TaskRouter. */
public class TestTaskRouterForDomainA implements TaskRoutingProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestTaskRouterForDomainA.class);

  private KadaiEngine theEngine;

  @Override
  public void initialize(KadaiEngine kadaiEngine) {
    theEngine = kadaiEngine;
  }

  @Override
  public String determineWorkbasketId(Task task) {
    if ("DOMAIN_A".equals(task.getDomain())) {
      List<WorkbasketSummary> wbs =
          theEngine.getWorkbasketService().createWorkbasketQuery().domainIn("DOMAIN_A").list();
      if (LOGGER.isInfoEnabled()) {
        LOGGER.info(String.format("TestTaskRouterForDomainA Routing to %s", wbs.get(0)));
      }
      return wbs.get(0).getId();
    } else {
      return null;
    }
  }
}
