----------------------------------------------------------------------
-- The following variables need to be changed for customization and
-- before running this script.
-- %SCHEMA% = Schema name
-- %SCHEMAPWD% = password
----------------------------------------------------------------------
-- Example: sqlplus scott/tiger@mydb @outbox-schema-oracle-create-user.sql
-- or, at the sqlplus prompt, enter
-- SQL> @outbox-schema-oracle-create-user.sql

ALTER SESSION SET "_ORACLE_SCRIPT"= TRUE;
CREATE USER %SCHEMA% IDENTIFIED BY %SCHEMAPWD%;

 GRANT CONNECT, RESOURCE to %SCHEMA% ;
 GRANT UNLIMITED TABLESPACE TO %SCHEMA% ;
 GRANT CREATE VIEW TO %SCHEMA%;
 GRANT JAVAUSERPRIV TO %SCHEMA%;

