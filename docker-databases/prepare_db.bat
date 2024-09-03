@ECHO OFF
SETLOCAL

:MENU
    ECHO.
    ECHO -----------------------------------------------------
    ECHO PRESS a number to select your task - anything to EXIT.
    ECHO -----------------------------------------------------
    ECHO.
    ECHO 1 - Start POSTGRES 14
    ECHO 2 - Stop  POSTGRES 14
    ECHO.
    ECHO 3 - Stop all
    ECHO.
    SET /P MENU=Select task then press ENTER:
    ECHO.
    IF [%MENU%]==[1] GOTO START_POSTGRES_14
    IF [%MENU%]==[2] GOTO STOP_POSTGRES_14
    IF [%MENU%]==[3] GOTO STOP_ALL
    EXIT /B

:START_POSTGRES_14
    ECHO ---
    ECHO docker compose -f %~dp0/docker-compose.yml up -d kadai-postgres_14
    docker compose -f %~dp0/docker-compose.yml up -d kadai-postgres_14

    ECHO ---
    GOTO MENU

:STOP_POSTGRES_14
    ECHO ---
    ECHO docker stop kadai-postgres_14
    ECHO docker compose -f %~dp0/docker-compose.yml rm -f -s -v kadai-postgres_14
    docker compose -f %~dp0/docker-compose.yml rm -f -s -v kadai-postgres_14
    ECHO ---
    GOTO MENU

:STOP_ALL
    ECHO ---
    ECHO docker compose -f %~dp0/docker-compose.yml down -v
    docker compose -f %~dp0/docker-compose.yml down -v
    ECHO ---
    GOTO MENU
