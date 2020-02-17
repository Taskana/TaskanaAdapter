# TaskanaAdapter

[![Build Status](https://travis-ci.org/Taskana/TaskanaAdapter.svg?branch=master)](https://travis-ci.org/Taskana/taskana)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/pro.taskana/taskana-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/pro.taskana/taskana-adapter)
[![License](http://img.shields.io/:license-apache-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

Adapter to sync tasks between TASKANA and an external workflow system, e.g. Camunda BPM. 

## Components
The Taskana Adapter repository consists of the taskana adapter, sample connectors to camunda and taskana as well as
an outbox REST-service and it's SpringBoot-Starter and listeners for camunda. In addition to that there are various example and test modules

The sample implementation of the camunda-system-connector uses the outbox-pattern.
The general concept of this pattern can be found here: 
<a href="https://microservices.io/patterns/data/transactional-outbox.html"> Transactional-Outbox-Pattern</a>. 
How the Camunda BPM Connector uses this pattern can be found here: <a href="https://taskana.atlassian.net/wiki/spaces/TAS/pages/881164289/Camunda+BPM+Connector"> Camunda BPM Connector</a>



    +------------------------------------------+--------------------------------------------------------------+
    | Component                                | Description                                                  |
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter                          | The adapter. Defines the service provider SPIs and APIs for  |
    |                                          | - SystemConnector (connects to the external system)          |
    |                                          | - TaskanaConnector (connects to taskana)                     |
    |                                          | These connectors are plugged in at runtime via SPI mechanisms|
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-camunda-system-connector | Sample implementation of SystemConnector SPI.                |
    |                                          | Connects to a camunda systems via camunda's REST API         |
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-taskana-connector        | Sample implementation of TaskanaConnector SPI. Connects      |
    |                                          | to one taskana system via taskana's java api which           |
    |                                          | accesses the database directly.                              |
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-camunda-listener         | Contains a TaskListener, ParseListener,                      |
    |                                          | ParseListenerProcessEnginePlugin and OutboxSchemaCreator as  |
    |                                          | client-side components of camunda-system-connector           |
    |                                          |                                   |
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-camunda-outbox-rest      | An outbox REST-Service which allows the adapter to           | 
    |                                          | query events from the outbox tables, implemented using JAX-RS|
    |                                          | The concept of the outbox-pattern can be found under the     |
    |                                          | "Notes for sample implementation of camunda-system-connector"|  
    |                                          | section                                                      |  
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-camunda-outbox-rest-     | SpringBoot-Starter in case the REST-Service is used within a |
    | spring-boot-starter                      | SpringBoot-Application                                       |
    |                                          |                                                              | 
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-camunda-spring-boot      | SpringBoot-Application containg the adapter with the sample  |
    |  -sample                                 | camunda-system-connector implementation                      |
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-camunda-spring-boot-test | SpringBoot-Application containing camunda, the adapter and   | 
    |                                          |   the outbox REST-Service to test a complete scenario        |
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-camunda-wildfly-example  | Example that can be deployed on Wildfly and contains         |
    |                                          |  the adapter with the sample camunda-system-connector        |
    |                                          |  implementation                                              |
    +------------------------------------------+--------------------------------------------------------------+
    | taskana-adapter-camunda-listener-example | Example Process-Application that can be deployed to camunda  |
    +------------------------------------------+--------------------------------------------------------------+

    

## The Adapter defines two SPIs
- **SystemConnector SPI**  connects the adapter to an external system like e.g. camunda.
- **TaskanaConnector SPI** connects the adapter to taskana.

Both SPI implementations are loaded by the adapter at initialization time via the Java SPI mechanism.  They provide plug points where custom code can be plugged in.\
Please note, that the term ‘referenced task’ is used in this document to refer to tasks in the external system that is accessed via the SystemConnector

## Overall Function

The adapter performs periodically the following tasks

*         retrieveNewReferencedTasksAndCreateCorrespondingTaskanaTasks
          *       retrieve newly created referenced tasks via SystemConnector.retrieveNewStartedReferencedTasks
          *       get the task’s variables via SystemConnector.retrieveVariables
          *       map referenced task to TASKANA task via TaskanaConnector.convertToTaskanaTask
          *       create an associated TASKANA task via TaskanaConnector.createTaskanaTask
          *	    clean the corresponding create-event in the outbox via SystemConnector.taskanaTasksHaveBeenCreatedForNewReferencedTasks

*       retrieveFinishedReferencedTasksAndTerminateCorrespondingTaskanaTasks
          *       retrieve finished referenced tasks via SystemConnector.retrieveFinishedTasks
          *       terminate corresponding TASKANA tasks via TaskanaConnector.terminateTaskanaTask
          * 	    clean the corresponding complete/delete-event in the outbox via SystemConnector.taskanaTasksHaveBeenCompletedForTerminatedReferencedTasks

*       retrieveFinishedTaskanaTasksAndCompleteCorrespondingReferencedTasks
          *       retrieve finished TASKANA tasks via TaskanaConnector.retrieveCompletedTaskanaTasksAsReferencedTasks
          *       complete the corresponding referenced tasks in the external system via SystemConnector.completeReferencedTask
          *       change the CallbackState of the corresponding task in TASKANA to completed via TaskanaConnector.changeReferencedTaskCallbackState

*       retrieveClaimedTaskanaTasksAndClaimCorrespondingReferencedTasks
          *       retrieve claimed TASKANA tasks via TaskanaConnector.retrieveCompletedTaskanaTasksAsReferencedTasks
          *       claim the corresponding referenced tasks in the external system via SystemConnector.claimReferencedTask
          *       change the CallbackState of the corresponding task in TASKANA to claimed via TaskanaConnector.changeReferencedTaskCallbackState
          
*       retrieveCancelledClaimTaskanaTasksAndCancelClaimCorrespondingReferencedTasks
          *       retrieve cancel claimed TASKANA tasks via TaskanaConnector.retrieveCancelledClaimTaskanaTasksAsReferencedTasks
          *       cancel the claim of the corresponding referenced tasks in the external system via SystemConnector.cancelClaimReferencedTask
          *       change the CallbackState of the corresponding task in TASKANA to processing required via TaskanaConnector.changeReferencedTaskCallbackState         

## Notes


1.  Variables \
    When the adapter finds a referenced task for which a taskana task must be started, it checks the variables of the referenced task's process. If they are not already present due to retrieval from the outbox it will attempt to retrieve them from the referenced task's process.
    These variables are stored in the **custom attributes** of the corresponding taskana task in a HashMap with key **referenced_task_variables** and value of type String that contains the Json representation of the variables.
2.  Workbaskets \
    The Adapter does not perform routing of tasks to workbaskets but instead relies on a SPI. A detailed description can be found here:
    <a href="https://taskana.atlassian.net/wiki/spaces/TAS/pages/995524621/TaskRouting+Service+Provider+Interface">TaskRouting Service Provider Interface</a>
    
        
## Properties

A detailed overview of the adapter properties can be found here: <a href="https://taskana.atlassian.net/wiki/spaces/TAS/pages/996966415/Adapter+Properties"> Adapter Properties</a>
