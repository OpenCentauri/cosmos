# Brofalo/pono-print-os changelog

All notable changes to the Pono Print OS land here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Foundation Step 4 cutover (2026-05-23): `meta-opencentauri/recipes-apps/klipper/kalico_2026.02.00.inc` SRC_URI git URL changed from `OpenCentauri/kalico.git` to `Brofalo/pono-kalico.git`; SRCREV now points at `Brofalo/pono-kalico` HEAD which carries upstream-author-preserved signed commits.
- DSP firmware (`kalico-firmware-dsp_2026.02.00.bb`) now generates a `.sha256` sidecar parallel to bed/toolhead. Tamper-detect substrate for the DSP blob between SWU apply and remoteproc load.

### Changed
- Migrated `0001-Reduce-log-rotate-threshold.patch` from a SRC_URI patch to a native commit in `Brofalo/pono-kalico` (`4218e722`, James Turton attribution preserved).
- PR bumped `r4 -> r5` on `kalico_2026.02.00.inc` (both SRC_URI URL and SRCREV changed).

### Deferred
- `0001-remove-save-config-subfile-check.patch` still applied via SRC_URI (gated on cosmos config audit per foundation plan).
- `0002-reduce-calibration-difference-tolerance.patch` still applied via SRC_URI (gated on HX711 noise-floor bench measurement per foundation plan).

### Security
- SSH commit signing configured on `Brofalo/pono-print-os` repository. Commits signed by `maui@brofalo` ED25519 key (`SHA256:4+2su7s4+66xaPk/3WZoCRXfQHDfdwqZZGj/iOxEv8M`); GitHub-verified status pending SSH signing key registration (H_FOLLOWUP_1).
- Branch protection applied to `main`: `required_linear_history=true`, `allow_force_pushes=false`, `allow_deletions=false`, `required_conversation_resolution=true`. `required_signatures` deferred behind H_FOLLOWUP_1.
- `SECURITY.md` added at repository root. Documents reporting path (GitHub Security advisory until disclosure email locked), pre-1.0 support stance, signing state (SSH commits live; cosign blocking from week 0 per foundation Step 6), key custody, and threat surface. Contains two `TBD JACK-INPUT` markers (disclosure email and key custody plan).

### Documentation
- README.md pivoted with a `Brofalo/pono-print-os` fork banner prepended above the upstream OC/cosmos README. Banner cross-links DIVERGENCE.md, CHANGELOG.md, SECURITY.md, pono-kalico, the slicer pipeline, and the grumpyscreen UI rewrite. Upstream README content preserved verbatim below the banner separator.

## Provenance

Pono Print OS is a downstream fork of the OpenCentauri/cosmos Yocto layer
plus Brofalo-specific customizations. The kalico firmware source it
builds against lives at `Brofalo/pono-kalico` (which is itself a fork of
`OpenCentauri/kalico rpmsg-with-new-hx71x`).

Fork-time anchor: `Brofalo/pono-print-os main` was created 2026-05-23
from `OpenCentauri/cosmos main` at SHA `44fd4116` (the OC cosmos head at
fork-time). The current Brofalo HEAD is on top of that anchor.

Upstream tracking:
- `OpenCentauri/cosmos` (parent): added as `upstream` remote on local
  clones for back-merge of OC's recipe-layer changes.
- `KalicoCrew/kalico main` (grand-upstream of the firmware source):
  tracked indirectly via `Brofalo/pono-kalico`.

## Pono Print V1.0 release criteria

Pono Print 1.0 ships when these gates all pass:

- [ ] M112 emergency-stop bench-verify per `pono-print/docs/M112_VERIFY.md`
- [ ] Z-touch repeatability test (20 probes, std dev under 0.01mm)
- [ ] Thermal runaway bench-verify (introduce sensor-failure condition + confirm shutdown within timeout)
- [ ] HX711 noise-floor bench measurement + Patch 2 (calibration tolerance) disposition
- [ ] Cosmos config audit + Patch 1 (save-config-subfile-check) disposition
- [ ] Sign-first build pipeline (Step 6: cosign + Fulcio + Rekor)
- [ ] Reproducible build verification (Step 8)
- [ ] 1-week continuous prints (Step 9, criterion locked: any-filament, ~7 prints minimum)
- [ ] Docs polished for OC community (Step 10)
- [ ] DSP firmware clean rebuild + flash + signed

Each gate's PASS evidence banks to `~/.claude/projects/.../memory/` and
cross-references this CHANGELOG.
