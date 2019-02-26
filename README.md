# TaskanaAdapter
Adapter to sync tasks between TASKANA and an external workflow system, e.g. Camunda BPM
## Components
The Taskana Adapter repository consists of the taskana adapter plus sample connectors to camunda and taskana.

    +------------------------------------------+--------------------------------------------------------------+
    | Component                                | Description                                                  |
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter                          | The adapter. Defines the service provider SPIs and APIs for  |
    |                                          | - SystemConnector (connects to the external system)          |
    |                                          | - TaskanaConnector (connects to taskana)                     |
    |                                          | These connectors are plugged in at runtime via SPI mechanisms|
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-sample                   | contains Application main class and properties               |
    |                                          | for taskana-adapter                                          |
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-camunda-system-connector | Sample implementation of SystemConnector SPI.                |
    |                                          | Connects to a camunda systems via camunda's REST API         |
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-taskana-connector        | Sample implementation of TaskanaConnector SPI. Connects      |
    |                                          | to one taskana system via taskana's java api which           |
    |                                          | accesses the database directly.                              |
    +------------------------------------------+--------------------------------------------------------------+

## The Adapter defines two SPIs
- **SystemConnector SPI**  connects the adapter to an external system like e.g. camunda.
- **TaskanaConnector SPI** connects the adapter to taskana.

Both SPI implementations are loaded by the adapter at initialization time via the Java SPI mechanism.  They provide plug points where custom code can be plugged in.\
Please note, that the term ‘referenced task’ is used in this document to refer to tasks in the external system that is accessed via the SystemConnector

## Overall Function

The adapter performs periodically the following tasks

*       retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks
        *       retrieve newly created referenced tasks via SystemConnector.retrieveReferencedTasksStartedAfter
        *       get the task’s variables via SystemConnector.retrieveVariables
        *       map referenced task to taskana task via TaskanaConnector.convertToTaskanaTask
        *       create an associated taskana task via TaskanaConnector.createTaskanaTask
        *       remember the tasks created in the adapter’s database

*       retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks
        *       retrieve finished referenced tasks via SystemConnector.retrieveFinishedTasks.
        *       terminate corresponding taskana tasks via TaskanaConnector.terminateTaskanaTask()

*       retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTasks
        *       retrieve finished Taskana tasks via TaskanaConnector.retrieveCompletedTaskanaTasks.
        *       complete the corresponding referenced tasks in the external system via SystemConnector.completeReferencedTask.

*       cleanup Adapter’s database tables
        *       delete aged entries from the adapter’s database tables

## Notes

1.  Duplicate tasks \
    Method retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks periodically queries the external system, to retrieve tasks that were created in a specific interval. \
    To determine this interval, transactional behavior must be taken into account. Due to transactions, a task that was created at a specific instant may become visible only when 
    the transaction is committed.\
    In the extreme case this is the maximum transaction lifetime. As a consequence, the specified interval is not between the last query time and now, 
    but between (the last query time – maximum transaction lifetime) and now.\
    Using default values to illustrate: Queries are performed every 10 seconds. The default maximum transaction lifetime is 120 seconds. This is, the adapter has to retrieve all tasks
    that were created in the last 130 seconds. \
    In the result, the query returns many tasks that have already been processed by the adapter. To cope with this problem, the adapter uses the TASKS table of its database to keep track
    of the tasks that are already handled.\ 
    Tasks that are not found in this table are added to the table and a corresponding taskana task is started. Tasks that are found in the table are
    ignored, they are duplicates.
2.  Variables \
    When the adapter finds a referenced task for which a taskana task must be started, it retrieves the variables of the referenced task's process.
    These variables are stored in the **custom attributes** of the corresponding taskana task in a HashMap with key **referenced_task_variables** and value of type String that contains the Json representation of the variables.
3.  Workbaskets \
    Task / workbasket mapping has been kept to a minimum. If the adapter creates a taskana task, it puts it into the workbasket of the referenced task’s assignee. If this workbasket doesn't exist, it is created (together with some workbasket_access_items). If the task has no assignee, it is put into a default workbasket with name DEFAULT_WORKBASKET.

## Properties
*       Adapter properties
        * taskana.adapter.datasource.url                                - jdbc URL of adapater's database
        * taskana.adapter.datasource.driverClassName                    - fully qualified name of driver class
        * taskana.adapter.datasource.username                           - user name to connect to adapter's db
        * taskana.adapter.datasource.password                           - password to connect to adapter's db
        * taskana.adapter.schemaName                                    - schema name for adapters tables

        * taskana.adapter.total.transaction.lifetime.in.seconds         - total transaction lifetime in seconds. Default: 120
        * taskana.adapter.scheduler.run.interval.for.cleanup.tasks.cron - cron expression that controls when cleanup tasks are run. Default: 0 0/10 * * * *
        * taskana.adapter.scheduler.task.age.for.cleanup.in.hours       - determines the number of hours, tasks must be finished before they are removed from the adapter's tables. Default: 10

        * taskana.adapter.scheduler.run.interval.for.start.taskana.tasks.in.milliseconds                - controls how often the external system is checked to create taskana tasks. Default 10000 ms
        * taskana.adapter.scheduler.run.interval.for.complete.referenced.tasks.in.milliseconds          - controls how often taskana is queried to find finished tasks that must be completed in
                                                                                                          the external system. Default: 10000 ms
        * taskana.adapter.scheduler.run.interval.for.check.cancelled.referenced.tasks.in.milliseconds   - controls how often the external system ich checked to find cancelled task that
                                                                                                          have a corresponding taskana task running. This taskana task must be terminated. Default 10000 ms

*        Camunda System connector properties
        * taskana-system-connector-camundaSystemURLs - Rest endpoint of camunda. e.g. http://localhost:8080/engine-rest

*        Taskana-connector properties
        * taskana.domains                          - valid taskana domains
        * taskana.classification.types             - valid classification types
        * taskana.classification.categories        - valid classification categories
        * taskana.datasource.url                   - jdbc url of taskana's database
        * taskana.datasource.driverClassName       - fully qualified name of driver class
        * taskana.datasource.username              - user name to connect to taskana db
        * taskana.datasource.password              - password to connect to taskana db
        * taskana.schemaName=TASKANA               - schema name of taskana's tables
