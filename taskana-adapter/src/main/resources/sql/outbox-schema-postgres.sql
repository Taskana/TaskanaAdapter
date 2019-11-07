CREATE SCHEMA IF NOT EXISTS %schemaName%;

CREATE TABLE %schemaName%.OUTBOX_SCHEMA_VERSION(
        ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1 INCREMENT BY 1),
        VERSION VARCHAR(255) NOT NULL,
        CREATED TIMESTAMP NOT NULL,
        PRIMARY KEY (ID)
);

INSERT INTO %schemaName%.OUTBOX_SCHEMA_VERSION (VERSION, CREATED) VALUES ('0.0.1', CURRENT_TIMESTAMP);


CREATE SEQUENCE IF NOT EXISTS  %schemaName%.event_store_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 2147483647
    CACHE 1;

CREATE TABLE IF NOT EXISTS  %schemaName%.event_store
(
    id integer NOT NULL DEFAULT nextval('%schemaName%.event_store_id_seq'::regclass),
    type text COLLATE pg_catalog."default",
    created timestamp(4) without time zone,
    payload text COLLATE pg_catalog."default",
    CONSTRAINT event_store_pkey PRIMARY KEY (id)
);
