CREATE SCHEMA IF NOT EXISTS %schemaName%;

CREATE SEQUENCE %schemaName%.event_store_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 2147483647
    CACHE 1;

CREATE TABLE %schemaName%.event_store
(
    id integer NOT NULL DEFAULT nextval('%schemaName%.event_store_id_seq'::regclass),
    type text COLLATE pg_catalog."default",
    created timestamp(4) without time zone,
    payload text COLLATE pg_catalog."default",
    CONSTRAINT event_store_pkey PRIMARY KEY (id)
);





