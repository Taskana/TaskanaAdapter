-- this script updates the tables OUTBOX_SCHEMA_VERSION and event_store.
SET SCHEMA %schemaName%;

INSERT INTO OUTBOX_SCHEMA_VERSION (VERSION, CREATED) VALUES ('1.11.0', CURRENT_TIMESTAMP);

ALTER TABLE event_store ADD COLUMN SYSTEM_ENGINE_IDENTIFIER VARCHAR(128) DEFAULT 'default';
