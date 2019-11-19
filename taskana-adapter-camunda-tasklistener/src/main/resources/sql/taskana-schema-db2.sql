SET SCHEMA %schemaName%;

begin
    declare continue handler for sqlstate '42710' begin end;
    execute immediate 'CREATE TABLE EVENT_STORE '
        || '('
        || '    ID INT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1),'
        || '    TYPE VARCHAR(32),'
        || '    CREATED TIMESTAMP,'
        || '    PAYLOAD BLOB,'
        || '    PRIMARY KEY (ID)'
        || ')';
end