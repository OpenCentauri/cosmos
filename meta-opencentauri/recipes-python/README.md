# Python Packages in meta-opencentauri

This directory contains Yocto recipes for Python packages that are required by OpenCentauri applications but are not available in upstream layers (poky / meta-openembedded / meta-python).

## Package Inventory

### Core Dependencies
| Package | Version | Source | Notes |
|---------|---------|--------|-------|
| `python3-scipy` | 1.14.1 | `meta-python-ai` (scarthgap) | Cherry-picked with deps. OpenBLAS backend. |
| `python3-pythran` | 0.16.1 | `meta-python-ai` (scarthgap) | Scipy C++ code generator. |
| `python3-beniget` | 0.4.2.post1 | `meta-python-ai` (scarthgap) | Pythran dependency. |
| `python3-pybind11` | 2.12.0 | `meta-python-ai` (scarthgap) | bbappend bump (required by scipy). |
| `openblas` | 0.3.28 | `meta-python-ai` (scarthgap) | LAPACK + BLAS. OpenMP disabled for ARM. |
| `xsimd` | 13.0.0 | `meta-python-ai` (scarthgap) | C++ SIMD for pythran. |

### Application Dependencies
| Package | Version | Used By | Notes |
|---------|---------|---------|-------|
| `python3-msgspec` | 0.19.0 | Kalico | Fast msgpack/JSON serialization. |
| `python3-uvloop` | 0.19.0 | Moonraker | Async event loop. |
| `python3-tornado` | 6.5.4 | Moonraker | Web framework. |
| `python3-apprise` | 1.9.6 | Moonraker | Notification service. |
| `python3-smart-open` | 7.5.1 | Moonraker | Streaming file I/O. |
| `python3-streaming-form-data` | 1.13.0 | Moonraker | Form data parser. |
| `python3-preprocess-cancellation` | 0.2.1 | Moonraker | G-code preprocessor. |
| `python3-importlib-metadata` | 7.2.1 | Moonraker | Backport. |
| `python3-inotify-simple` | 1.3.5 | Moonraker | File watching. |
| `python3-libnacl` | 2.1.0 | Moonraker | NaCl crypto. |
| `python3-ldap3` | 2.9.1 | Moonraker | LDAP auth. |

## Why not add `meta-python-ai` as a layer?

The `meta-python-ai` layer contains 100+ AI/ML recipes and builds LLVM for `llvmlite`/`numba`. OpenCentauri only needs a small subset (`scipy`, `numpy`, `openblas`). Cherry-picking the required recipes into `meta-opencentauri` avoids pulling in the entire heavy dependency chain.

## Adding a new package

If a package is needed for an OpenCentauri application:

1. Check if it already exists in poky or meta-openembedded
2. If not, place the recipe here under `recipes-python/<package>/`
3. Document it in the table above
