package pro.taskana.adapter.camunda.mapper;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.text.SimpleDateFormat;

/** This class is responsible for configuring the ObjectMapper of Jackson. */
public final class JacksonConfigurator {

  public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
  public static String dateFormatString = DEFAULT_DATE_FORMAT;

  private JacksonConfigurator() {}

  public static ObjectMapper createAndConfigureObjectMapper() {

    ObjectMapper mapper = new ObjectMapper();
    SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
    mapper.setDateFormat(dateFormat);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    return mapper;
  }
}
