@echo off
title AdvanceFoodnest Launcher
setlocal

echo ========================================================
echo  Launching AdvanceFoodnest Full-Stack Application
echo ========================================================

start "AdvanceFoodnest Backend" cmd /k "cd /d %~dp0 && call run-backend.bat"

echo Waiting for backend server to initialize (8 seconds)...
timeout /t 8 /nobreak > nul

echo Opening AdvanceFoodnest in default web browser...
start http://localhost:8080/index.html

echo.
echo Application is running!
echo Keep the backend console window open while using the application.
