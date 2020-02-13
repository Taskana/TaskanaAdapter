package pro.taskana.adapter.camunda;

public interface TaskanaConfigurationProperties {

  static final String TASKANA_ADAPTER_CREATE_OUTBOX_SCHEMA = "taskana.adapter.create_outbox_schema";
  static final String TASKANA_OUTBOX_DEFAULT_SCHEMA = "taskana_tables";
  public static final String TASKANA_OUTBOX_PROPERTIES = "taskana-outbox.properties";

  static final String TASKANA_ADAPTER_OUTBOX_SCHEMA = "taskana.adapter.outbox.schema";
  static final String TASKANA_ADAPTER_OUTBOX_DATASOURCE_JNDI =
      "taskana.adapter.outbox.datasource.jndi";
  static final String TASKANA_ADAPTER_OUTBOX_DATASOURCE_DRIVER =
      "taskana.adapter.outbox.datasource.driver";
  static final String TASKANA_ADAPTER_OUTBOX_DATASOURCE_URL =
      "taskana.adapter.outbox.datasource.url";
  static final String TASKANA_ADAPTER_OUTBOX_DATASOURCE_USERNAME =
      "taskana.adapter.outbox.datasource.username";
  static final String TASKANA_ADAPTER_OUTBOX_DATASOURCE_PASSWORD =
      "taskana.adapter.outbox.datasource.password";
}
