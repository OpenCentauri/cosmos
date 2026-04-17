# Local Yocto Build Environment

Docker-based build environment for creating OpenCentauri SWU update images.

## Prerequisites

- Docker installed and running
- Sufficient disk space (~50GB for full build)
- Internet connection for downloading sources

## Usage

### Build All SWU Images
```bash
./build.sh
```

This builds 3 SWU files:
- `opencentauri-upgrade-*.swu` - Standard (as defined in main, no Happy Hare)
- `opencentauri-upgrade-hh-full-*.swu` - Standard + Happy Hare + Mainsail
- `opencentauri-upgrade-hh-minimal-*.swu` - Happy Hare without web interface

### Build Specific Image
```bash
./build.sh opencentauri-upgrade-hh-full
```

## Output Location

Built images are located in:
```
../../build/tmp/deploy/images/elegoo-centauri-carbon1/*.swu
```

## Cache

Build cache is stored in `../../cache/` to speed up subsequent builds.

## Clean Build

To start a fresh build:
```bash
rm -rf ../../build ../../cache
./build.sh
```

## Files

| File | Description |
|------|-------------|
| `Dockerfile` | Ubuntu 22.04 with Yocto build dependencies |
| `build.sh` | Main build script |
| `.dockerignore` | Excludes unnecessary files from Docker context |