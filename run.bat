@echo off

REM Quick Start Script for Mortgage Service (Windows)

echo ======================================
echo Mortgage Service - Quick Start
echo ======================================
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed. Please install Maven 3.9.8 or later.
    exit /b 1
)

echo Building the project...
cd backend
call mvn clean package -DskipTests

if %ERRORLEVEL% NEQ 0 (
    echo Build failed!
    exit /b 1
)

echo.
echo ======================================
echo Build successful!
echo ======================================
echo.
echo Running with H2 database (no Kafka, no Docker required)...
echo.

java -jar target/mortgage-service-1.0.0.jar --spring.profiles.active=h2

