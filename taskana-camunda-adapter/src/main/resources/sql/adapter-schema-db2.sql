SET SCHEMA TASKANA;

CREATE TABLE TCA_SCHEMA_VERSION(
        ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
        VERSION VARCHAR(255) NOT NULL,
        CREATED TIMESTAMP NOT NULL,
        PRIMARY KEY (ID)
);

INSERT INTO TCA_SCHEMA_VERSION (VERSION, CREATED) VALUES ('1.0.4', CURRENT_TIMESTAMP);


CREATE TABLE TASKS_CREATED(
        ID VARCHAR(64) NOT NULL,
        CREATED TIMESTAMP NOT NULL,
        CAMUNDA_SYSTEM_URL VARCHAR(1024) NOT NULL,
        PRIMARY KEY (ID)
);

CREATE TABLE TASKS_COMPLETED(
        ID VARCHAR(64) NOT NULL,
        COMPLETED TIMESTAMP NOT NULL,
        CAMUNDA_SYSTEM_URL VARCHAR(1024) NOT NULL,
        PRIMARY KEY (ID)
);
