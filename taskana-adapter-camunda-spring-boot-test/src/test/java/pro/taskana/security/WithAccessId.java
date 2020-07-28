package pro.taskana.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Specify user id for JUnit JaasRunner. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
@Repeatable(WithAccessIds.class)
public @interface WithAccessId {

  String userName();

  String[] groupNames() default {};
}
