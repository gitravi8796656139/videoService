#!/bin/bash
# -------------------------------------------------------
# Video Service - Start Script
# Usage: ./start.sh
# -------------------------------------------------------

JAR="target/video-service-1.0.0.jar"

if [ ! -f "$JAR" ]; then
  echo "Building project..."
  mvn clean package -DskipTests -q
fi

echo "Starting Video Service on port 8080..."
java -jar "$JAR" \
  --video.storage.path=./video-storage \
  --server.port=8080
