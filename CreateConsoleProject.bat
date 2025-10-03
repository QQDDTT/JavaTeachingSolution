@echo off
REM 用法: CreateConsoleProject.bat Example
if "%~1"=="" (
    echo Usage: %0 ProjectName
    exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0CreateConsoleProject.ps1" "%~1"
