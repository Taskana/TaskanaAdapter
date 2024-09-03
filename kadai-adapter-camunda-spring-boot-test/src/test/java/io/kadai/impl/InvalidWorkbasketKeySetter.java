package io.kadai.impl;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for setting an invalid String variable "kadai.workbasket-key" in a test
 * scenario.
 */
public class InvalidWorkbasketKeySetter implements JavaDelegate {

  private static final Logger LOGGER = LoggerFactory.getLogger(WorkbasketKeySetter.class);

  @Override
  public void execute(DelegateExecution execution) {
    LOGGER.info("Setting workbasket key for testing purposes to \"invalidWorkbasketKey\" ");
    execution.setVariable("kadai.workbasket-key", "invalidWorkbasketKey");
  }
}
