-- this script updates the tables OUTBOX_SCHEMA_VERSION and event_store.
ALTER SESSION SET CURRENT_SCHEMA = %schemaName%;

INSERT INTO OUTBOX_SCHEMA_VERSION (VERSION, CREATED) VALUES ('1.12.0', CURRENT_TIMESTAMP);

-- add a new column to the event_store table named lock_expire with type TIMESTAMP and allow null values for oracle
ALTER TABLE EVENT_STORE ADD (LOCK_EXPIRE TIMESTAMP NULL);