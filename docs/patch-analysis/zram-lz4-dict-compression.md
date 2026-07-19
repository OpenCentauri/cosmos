# Patch Feasibility Analysis: zram LZ4 Dictionary Compression Optimization

## Patch Reference

- **Subject:** `zram: Optimize LZ4 dictionary compression performance`
- **Author:** gao xu \<gaoxu2@honor.com\>
- **Date:** 2026-03-10
- **Thread:** https://lore.kernel.org/lkml/ae51966c3cb445e9983230243bb6a5b2@honor.com/
- **Files touched:** `drivers/block/zram/backend_lz4.c`, `drivers/block/zram/zcomp.h`, `drivers/block/zram/zram_drv.c`

## Our Kernel

- **Version:** Linux **6.6.85** (LTS), built by Yocto for `elegoo-centauri-carbon1`
- **Recipe:** `meta-sunxi/recipes-kernel/linux/linux-mainline_6.6.85.bb`

---

## Part 1: Patch Applicability to 6.6.85

### Verdict: ❌ Does NOT Apply

The patch cannot be applied to kernel 6.6.85 — not even with manual conflict resolution. The zram
compression subsystem in 6.6 uses a completely different architecture to the one the patch targets.

### Finding 1 — `backend_lz4.c` Does Not Exist

The patch's primary target file `drivers/block/zram/backend_lz4.c` **does not exist in 6.6.85**.

In 6.6, zram compression is handled via the generic kernel `crypto_comp` API inside `zcomp.c`.
The refactoring that introduced per-algorithm backend files (`backend_lz4.c`, `backend_zstd.c`, etc.)
landed in a later kernel release (estimated 6.8–6.9). There is no file to patch.

```
# 6.6.85 zram directory:
drivers/block/zram/
├── Kconfig
├── Makefile
├── zcomp.c      ← LZ4 handled here via crypto_comp, no dedicated backend file
├── zcomp.h
├── zram_drv.c
└── zram_drv.h
```

### Finding 2 — `zcomp.h` Structure Is Completely Different

The patch adds a `dict_gen` field to `struct zcomp_params`:

```c
// Patch expects this struct to exist in zcomp.h:
struct zcomp_params {
    void *dict;
    size_t dict_sz;
    s32 level;
+   u32 dict_gen;   // ← field added by patch
    ...
    void *drv_data;
};
```

In 6.6.85, **`struct zcomp_params` does not exist**. The compression context is `struct zcomp_strm`
backed by the generic `struct crypto_comp`:

```c
// Actual 6.6.85 zcomp.h:
struct zcomp_strm {
    local_lock_t lock;
    void *buffer;
    struct crypto_comp *tfm;  // no dict, no level, no drv_data
};
```

### Finding 3 — `comp_params_store()` Does Not Exist in `zram_drv.c`

The patch adds `zram->params[prio].dict_gen++` at line 1709 inside `comp_params_store()`. This
function, and the entire concept of per-priority compression params (`zram->params[prio]`), does not
exist in 6.6.85 `zram_drv.c`. There is no target hunk to apply.

### Summary

| Target | Status in 6.6.85 |
|--------|-----------------|
| `drivers/block/zram/backend_lz4.c` | ❌ File does not exist |
| `struct zcomp_params` in `zcomp.h` | ❌ Struct does not exist |
| `comp_params_store()` in `zram_drv.c` | ❌ Function does not exist |

Applying this patch would require **first backporting the entire post-6.6 zram subsystem refactor**,
which spans many commits across multiple files — a disproportionate effort.

### Relevance to This Project

Even if the patch could be applied, its practical impact would be **zero** on this device:

- zram is configured as a compressed swap device (`CONFIG_ZRAM=m`, `CONFIG_ZRAM_DEF_COMP_LZ4=y`)
- **Dictionary compression is not configured** — the >50% speedup only applies when a zram
  dictionary is set via sysfs, which is not done in this build
- The optimization path is gated behind `if (!zctx->cstrm)` which is only entered when a
  dictionary is actively in use

---

## Part 2: Kernel Upgrade Feasibility — 6.6.85 → 6.12 LTS

The zram architecture the patch targets was introduced between 6.6 and 6.12. If applying this
patch (or similar future zram improvements) is desired, a kernel upgrade would be needed first.

### Target Version: 6.12 LTS

Currently active non-EOL kernel releases:

| Version | Type | Recommendation |
|---------|------|----------------|
| 6.6.132 | LTS | Current |
| **6.12.80** | **LTS** | **Recommended upgrade target** |
| 6.18.21 | Longterm | Too new, higher risk |

6.12 LTS is the appropriate target: it has the new zram backend architecture AND is a stable
long-term release with a multi-year support window.

### Risk Assessment

#### 1. Out-of-Tree Patches — Medium Risk

Three patches in `meta-opencentauri` must be re-validated and potentially rebased against 6.12:

| Patch | Area | Risk | Notes |
|-------|------|------|-------|
| `0001-Add-elegoo-centauri-carbon1.dts.patch` | Board DTS | 🟡 Low | DTS files rarely conflict; binding APIs stable |
| `0001-Add-support-for-r528-msgbox-and-remoteproc.patch` | R528 mailbox + remoteproc driver | 🟠 Medium | Remoteproc subsystem has evolved 6.6→6.12; may need rebase |
| `0002-drm-add-RB-channel-swap-support-for-panels-with-swap.patch` | DRM sun4i channel swap | 🟡 Low-Medium | DRM driver context can shift; likely minor conflicts |
| `0003-thermal-sun8i-add-sun20i-d1-ths-support.patch` | Thermal sensor support | 🟡 Low-Medium | sun8i thermal driver; may be upstreamed in 6.12 |

#### 2. meta-sunxi Layer — High Risk

The `meta-sunxi` layer only provides `linux-mainline_6.6.85.bb`. An upgrade requires:
- A new `linux-mainline_6.12.x.bb` recipe (straightforward to write, but needs validation)
- Re-validating all `sunxi-kmeta` BSP config fragments against 6.12 Kconfig

#### 3. Kernel Config Fragments — Low Risk

The `.cfg` fragments in `meta-opencentauri/recipes-kernel/linux/linux-mainline/elegoo-centauri-carbon1/`
set explicit `CONFIG_*` values. Some symbols may have been renamed or removed in 6.12, but
these are detectable via `do_kernel_configcheck` warnings at build time.

#### 4. R528/T113-S3 SoC Mainline Support in 6.12 — Unknown

Allwinner T113-S3/R528 upstream DTS and driver support has been incrementally landing in
mainline. The gap between what 6.12 provides out-of-the-box vs. what our patches add needs
to be assessed — some of our patches may already be upstreamed in 6.12, reducing the porting
burden, or there may be new gaps.

### Upgrade Effort Summary

| Area | Effort |
|------|--------|
| New kernel recipe `.bb` file | Low (1–2 hours) |
| Config fragment validation | Low (automated, fix warnings) |
| Rebase 4 out-of-tree patches | Medium (0.5–2 days depending on conflicts) |
| R528/T113 SoC gap analysis | Unknown (needs exploratory build) |
| Full test/validation cycle | Medium (build + flash + smoke test) |

### Upgrade Recommendation

A 6.12 LTS upgrade is **feasible** but requires a dedicated effort. The recommended approach:

1. Create a test branch with a 6.12.x kernel recipe
2. Run a build to surface `do_kernel_configcheck` warnings and compilation errors
3. Assess which out-of-tree patches apply cleanly vs. need rebase
4. Check if R528/T113 DTS and drivers are already upstreamed in 6.12
5. Decide whether the upgrade is worth pursuing based on actual gap size

This would be a separate work item from the zram patch itself.
