package pro.taskana.utils;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class ResourceUtils {
  private ResourceUtils() {
    // empty default constructor because all methods are static
  }

  public static String getResourcesAsString(Class clazz, String fileName) {
    try (InputStream is = clazz.getResourceAsStream(clazz.getSimpleName() + "-" + fileName)) {
      if (is == null) {
        throw new RuntimeException("File not found");
      }
      try (InputStreamReader isr = new InputStreamReader(is, UTF_8);
          BufferedReader reader = new BufferedReader(isr)) {
        return reader.lines().collect(joining(lineSeparator()));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
