# Mainsail Frontend for HH-Minimal Image

**Required** - Provides the web interface for HH-Minimal, which has no frontend installed on the printer.

## Purpose

The HH-Minimal image includes:
- Kalico + Happy Hare + Moonraker (API backend on port 80)
- **No web frontend** (Mainsail/Fluidd removed to save RAM)

This container runs Mainsail locally on your PC and connects to the printer's moonraker API.

## Prerequisites

- Docker and docker-compose installed
- Printer running minimal image with moonraker accessible
- Network connection to printer

## Configuration

**IMPORTANT:** Edit `config.json` to set your printer's moonraker IP address before starting!

Default config (adjust IP to your printer):
```json
{
  "instances": [
    {
      "hostname": "192.168.1.100",
      "port": 7125
    }
  ]
}
```

## Usage

### Start Mainsail
```bash
./start.sh
```

### Access Web Interface
Open browser: http://localhost:8080

### Stop Container
```bash
docker-compose down
```

## Files

| File | Description |
|------|-------------|
| `docker-compose.yml` | Container configuration |
| `nginx.conf` | Nginx reverse proxy config |
| `config.json` | Moonraker host connection settings |
| `start.sh` | Startup script |

## Benefits

- Minimal image saves storage/RAM (no frontend)
- Full Mainsail interface available locally
- Can run on PC/laptop instead of printer
- Multiple printers can be configured