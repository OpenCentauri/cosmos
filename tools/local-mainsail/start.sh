#!/bin/bash

# Start Mainsail container for minimal image frontend

cd "$(dirname "$0")"

echo "Starting Mainsail container..."
docker-compose up -d

echo ""
echo "Mainsail is available at: http://localhost:8080"
echo ""
echo "Configure your printer's moonraker address in config.json"