# OpenCentauri Build Tools

This directory contains tools for building and testing OpenCentauri firmware images.

## Directory Structure

| Directory | Description |
|-----------|-------------|
| `local-build/` | Docker-based Yocto build environment for creating SWU update images |
| `local-mainsail/` | Mainsail web interface for HH-Minimal (required - no frontend on printer) |

## Quick Start

### Build SWU Images
```bash
cd local-build
./build.sh
```

### Run Local Mainsail for Minimal Image Testing
```bash
cd local-mainsail
./start.sh
```

See the README in each subdirectory for detailed instructions.