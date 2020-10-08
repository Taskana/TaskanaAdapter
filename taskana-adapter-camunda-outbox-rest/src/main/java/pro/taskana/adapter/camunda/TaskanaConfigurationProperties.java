package pro.taskana.adapter.camunda;

public interface TaskanaConfigurationProperties {

  String TASKANA_OUTBOX_PROPERTIES = "taskana-outbox.properties";

  String TASKANA_ADAPTER_OUTBOX_SCHEMA = "taskana.adapter.outbox.schema";
  String TASKANA_ADAPTER_OUTBOX_DATASOURCE_JNDI = "taskana.adapter.outbox.datasource.jndi";
  String TASKANA_ADAPTER_OUTBOX_DATASOURCE_DRIVER = "taskana.adapter.outbox.datasource.driver";
  String TASKANA_ADAPTER_OUTBOX_DATASOURCE_URL = "taskana.adapter.outbox.datasource.url";
  String TASKANA_ADAPTER_OUTBOX_DATASOURCE_USERNAME = "taskana.adapter.outbox.datasource.username";
  String TASKANA_ADAPTER_OUTBOX_DATASOURCE_PASSWORD = "taskana.adapter.outbox.datasource.password";
  String TASKANA_ADAPTER_OUTBOX_MAX_NUMBER_OF_EVENTS =
      "taskana.adapter.outbox.max.number.of.events";
  String TASKANA_ADAPTER_OUTBOX_DURATION_BETWEEN_TASK_CREATION_RETRIES =
      "taskana.adapter.outbox.duration.between.task.creation.retries";
}
