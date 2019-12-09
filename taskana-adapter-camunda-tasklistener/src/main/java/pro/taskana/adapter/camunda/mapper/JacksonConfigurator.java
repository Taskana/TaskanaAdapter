package pro.taskana.adapter.camunda.mapper;

import java.text.SimpleDateFormat;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JacksonConfigurator {

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static String dateFormatString = DEFAULT_DATE_FORMAT;

    public static ObjectMapper configureObjectMapper(ObjectMapper mapper) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
        mapper.setDateFormat(dateFormat);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

        return mapper;
    }
}
