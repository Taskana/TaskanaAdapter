package pro.taskana.adapter.camunda.util;

import java.util.HashMap;
import java.util.Map;

public class PrimitiveWrapperChecker {

  private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new HashMap<>(8);

  public static boolean isPrimitiveWrapper(Class<?> clazz) {

    return primitiveWrapperTypeMap.containsKey(clazz);
  }

  static {
    primitiveWrapperTypeMap.put(Boolean.class, Boolean.TYPE);
    primitiveWrapperTypeMap.put(Byte.class, Byte.TYPE);
    primitiveWrapperTypeMap.put(Character.class, Character.TYPE);
    primitiveWrapperTypeMap.put(Double.class, Double.TYPE);
    primitiveWrapperTypeMap.put(Float.class, Float.TYPE);
    primitiveWrapperTypeMap.put(Integer.class, Integer.TYPE);
    primitiveWrapperTypeMap.put(Long.class, Long.TYPE);
    primitiveWrapperTypeMap.put(Short.class, Short.TYPE);
  }

}
