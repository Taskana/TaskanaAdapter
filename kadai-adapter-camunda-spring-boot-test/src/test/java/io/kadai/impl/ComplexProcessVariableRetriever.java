package io.kadai.impl;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for retrieving complex process variables containing Objects set by a
 * camunda user task.
 */
public class ComplexProcessVariableRetriever implements JavaDelegate {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(ComplexProcessVariableRetriever.class);

  @Override
  public void execute(DelegateExecution execution) {

    ProcessVariableTestObject processVariableTestObject =
        (ProcessVariableTestObject) execution.getVariable("attribute1");

    if (LOGGER.isInfoEnabled()) {
      LOGGER.info(
          String.format(
              "Successfully deserialized complex process variable"
                  + " \"ProcessVariableTestObject\", "
                  + "retrieving sample value via Getter-method : doubleField value = %3.2f ",
              processVariableTestObject.getDoubleField()));
    }
  }
}
