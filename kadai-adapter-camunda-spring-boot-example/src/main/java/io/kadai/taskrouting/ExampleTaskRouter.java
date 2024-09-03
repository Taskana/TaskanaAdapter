package io.kadai.taskrouting;

import io.kadai.common.api.KadaiEngine;
import io.kadai.spi.routing.api.TaskRoutingProvider;
import io.kadai.task.api.models.Task;

/** This is a sample implementation of TaskRouter. */
public class ExampleTaskRouter implements TaskRoutingProvider {

  @Override
  public void initialize(KadaiEngine kadaiEngine) {
    // no-op
  }

  @Override
  public String determineWorkbasketId(Task task) {
    return "WBI:100000000000000000000000000000000001";
  }
}
