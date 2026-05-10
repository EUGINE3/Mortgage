#!/bin/bash

# Quick Start Script for Mortgage Service

echo "======================================"
echo "Mortgage Service - Quick Start"
echo "======================================"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed. Please install Maven 3.9.8 or later."
    exit 1
fi

echo "Building the project..."
cd backend
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo ""
echo "======================================"
echo "Build successful!"
echo "======================================"
echo ""
echo "Running with H2 database (no Kafka, no Docker required)..."
echo ""

java -jar target/mortgage-service-1.0.0.jar --spring.profiles.active=h2

