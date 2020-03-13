package pro.taskana.adapter.camunda.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReadPropertiesHelper {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReadPropertiesHelper.class);

  private ReadPropertiesHelper() {
    throw new IllegalStateException("Utility class");
  }

  public static String getPropertyValueFromFile(String propertiesFileName, String propertyName) {
    InputStream propertiesStream =
        ReadPropertiesHelper.class.getClassLoader().getResourceAsStream(propertiesFileName);

    Properties properties = new Properties();
    String propertyValue = null;

    try {

      properties.load(propertiesStream);
      propertyValue = properties.getProperty(propertyName);

    } catch (IOException | NullPointerException e) {
      LOGGER.warn(
          String.format(
              "Caught Exception while trying to retrieve the %s  "
                  + " property from the provided properties file %s ",
              propertyName, propertiesFileName),
          e);
    }

    return propertyValue;
  }
}
