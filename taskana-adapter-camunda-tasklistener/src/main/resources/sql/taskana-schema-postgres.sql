CREATE SCHEMA IF NOT EXISTS %schemaName%;
SET search_path TO %schemaName%;

CREATE TABLE IF NOT EXISTS event_store
(
    id integer GENERATED ALWAYS AS IDENTITY (START WITH 1),
    type text COLLATE pg_catalog."default",
    created timestamp(4) without time zone,
    payload text COLLATE pg_catalog."default",
    CONSTRAINT event_store_pkey PRIMARY KEY (id)
);
