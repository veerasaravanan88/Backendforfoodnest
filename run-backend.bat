@echo off
title AdvanceFoodnest Spring Boot Backend (Port 8080)
setlocal

cd /d "%~dp0backend"

if "%DB_PASSWORD%"=="" (
    set DB_PASSWORD=veera8610
)

if exist "C:\Program Files\Java\jdk-21.0.11" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"
    set "PATH=C:\Program Files\Java\jdk-21.0.11\bin;%PATH%"
)

echo ========================================================
echo  AdvanceFoodnest Backend Server
echo  Listening on: http://localhost:8080
echo  Database: advance_foodnest (PostgreSQL 5432)
echo ========================================================

if exist "target\advance-foodnest-1.0.0.jar" (
    java -jar target\advance-foodnest-1.0.0.jar
) else (
    mvn.cmd spring-boot:run
)

pause
