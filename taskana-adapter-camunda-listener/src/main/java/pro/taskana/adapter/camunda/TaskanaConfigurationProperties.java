package pro.taskana.adapter.camunda;

public interface TaskanaConfigurationProperties {

  String TASKANA_ADAPTER_CREATE_OUTBOX_SCHEMA = "taskana.adapter.create_outbox_schema";
  String TASKANA_OUTBOX_DEFAULT_SCHEMA = "taskana_tables";
  String TASKANA_OUTBOX_PROPERTIES = "taskana-outbox.properties";

  String TASKANA_ADAPTER_OUTBOX_SCHEMA = "taskana.adapter.outbox.schema";
  String TASKANA_ADAPTER_OUTBOX_DATASOURCE_JNDI = "taskana.adapter.outbox.datasource.jndi";
  String TASKANA_ADAPTER_OUTBOX_DATASOURCE_DRIVER = "taskana.adapter.outbox.datasource.driver";
  String TASKANA_ADAPTER_OUTBOX_DATASOURCE_URL = "taskana.adapter.outbox.datasource.url";
  String TASKANA_ADAPTER_OUTBOX_DATASOURCE_USERNAME = "taskana.adapter.outbox.datasource.username";
  String TASKANA_ADAPTER_OUTBOX_DATASOURCE_PASSWORD = "taskana.adapter.outbox.datasource.password";
}
