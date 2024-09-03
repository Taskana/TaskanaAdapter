package io.kadai.adapter.camunda.outbox.rest.config;

import io.kadai.adapter.camunda.outbox.rest.controller.CamundaTaskEventsController;
import io.kadai.adapter.camunda.outbox.rest.exception.CamundaTaskEventNotFoundExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.InvalidArgumentExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.JsonParseExceptionMapper;
import io.kadai.adapter.camunda.outbox.rest.exception.MismatchedInputExceptionMapper;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/** Configures the outbox REST service. */
@ApplicationPath("/outbox-rest")
public class OutboxRestServiceConfig extends Application {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classesToBeScanned = new HashSet<>();
    classesToBeScanned.add(CamundaTaskEventsController.class);
    classesToBeScanned.add(InvalidArgumentExceptionMapper.class);
    classesToBeScanned.add(CamundaTaskEventNotFoundExceptionMapper.class);
    classesToBeScanned.add(JsonParseExceptionMapper.class);
    classesToBeScanned.add(MismatchedInputExceptionMapper.class);
    return classesToBeScanned;
  }
}
