-- this script updates the tables OUTBOX_SCHEMA_VERSION and event_store.
SET SCHEMA %schemaName%;

INSERT INTO OUTBOX_SCHEMA_VERSION (VERSION, CREATED) VALUES ('1.12.0', CURRENT_TIMESTAMP);

-- update the schema to have a column for lock with type TIMESTAMP and allow null values
ALTER TABLE event_store ADD COLUMN LOCK_EXPIRE TIMESTAMP NULL;