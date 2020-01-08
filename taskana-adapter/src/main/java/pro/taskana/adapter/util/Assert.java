package pro.taskana.adapter.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pro.taskana.adapter.exceptions.AssertionViolationException;

/**
 * utility class that allows to assert specific conditions.
 *
 * @author bbr
 */
public final class Assert {

  private static final Logger LOGGER = LoggerFactory.getLogger(Assert.class);

  private Assert() {}

  public static void assertion(boolean isCondition, String condition) {
    if (!isCondition) {
      String assertion = "Assertion violation !(" + condition + ") ";
      LOGGER.error(assertion);
      throw new AssertionViolationException(assertion);
    }
  }
}
