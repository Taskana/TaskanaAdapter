SET SCHEMA %schemaName%;

begin
    declare continue handler for sqlstate '42710' begin end;
    execute immediate 'CREATE TABLE OUTBOX_SCHEMA_VERSION('
        ||'    ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1),'
        ||'    VERSION VARCHAR(255) NOT NULL,'
        ||'    CREATED TIMESTAMP NOT NULL,'
        ||'    PRIMARY KEY (ID)'
        ||')';

    execute immediate 'INSERT INTO OUTBOX_SCHEMA_VERSION (VERSION, CREATED) '
        ||'VALUES ("0.0.1", CURRENT_TIMESTAMP)';

    execute immediate 'CREATE TABLE EVENT_STORE '
        || '('
        || '    ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1),'
        || '    TYPE VARCHAR(32),'
        || '    CREATED TIMESTAMP,'
        || '    PAYLOAD BLOB,'
        || '    PRIMARY KEY (ID)'
        || ')';
end
