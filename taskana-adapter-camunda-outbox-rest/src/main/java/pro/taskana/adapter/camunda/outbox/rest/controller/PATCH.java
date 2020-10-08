package pro.taskana.adapter.camunda.outbox.rest.controller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.ws.rs.HttpMethod;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@HttpMethod("PATCH")
@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public @interface PATCH {}
