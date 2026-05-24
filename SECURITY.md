# Security Policy

This document describes how to report security issues in
`Brofalo/pono-print-os`, the signed Yocto OS layer fork for Pono Print
(Elegoo Centauri Carbon target).

## Reporting a Vulnerability

**TBD JACK-INPUT:** disclosure email address. Pending options:
- `security@ponodata.com` (would require MX record + mailbox provisioning)
- Jack personal address with auto-forward + filter
- Foundry-routed triage (Captain Spot) with Jack-confirmation gate

Until the disclosure email is locked, file a private security advisory
via GitHub:

<https://github.com/Brofalo/pono-print-os/security/advisories/new>

Do not file public issues for security bugs. Do not post details to
community Discord, Reddit, or social media before disclosure.

## Supported Versions

Pre-1.0. No semver-stable release exists yet. Fix-forward only on the
`main` branch. Users running pre-1.0 firmware should track HEAD or stop
flashing.

The 1.0 tag waits on:

- M112 emergency-stop bench-verify executed
- DSP MCU clean rebuild + flash bench-verified
- Cosign signing pipeline shipped blocking from week 0 (foundation Step 6)
- One week of continuous prints without regression

Once 1.0 ships, the supported-versions matrix will list patched/EOL lines.

## Signing and Verification

### Current state (2026-05-23)

| Surface | Signing | Verification |
|---|---|---|
| Git commits | SSH (ED25519, `maui@brofalo` key) | GitHub commit verification badge |
| SWU artifacts | NOT YET (foundation Step 6) | DEFERRED |
| IPK packages | NOT YET | DEFERRED |
| Build provenance | NOT YET | DEFERRED |

Branch protection on `main`: `required_linear_history=true`,
`allow_force_pushes=false`, `allow_deletions=false`,
`required_conversation_resolution=true`. `required_signatures` flag
deferred behind SSH-signing-key registration (H_FOLLOWUP_1).

### Cosign signing (planned, foundation Step 6)

Per the signed-first HARD requirement, the cosign signing pipeline
ships blocking from week 0 with no advisory-then-required transition.
All SWU artifacts and IPK packages will be signed via Fulcio + Rekor
keyless cosign attestations.

Once shipped, verify with:

```bash
cosign verify-blob \
  --certificate-identity-regexp 'https://github.com/Brofalo/pono-print-os/.*' \
  --certificate-oidc-issuer 'https://token.actions.githubusercontent.com' \
  --signature <swu>.sig \
  --bundle <swu>.bundle \
  <swu>
```

Until cosign ships: do not flash SWU artifacts unless built locally
on Node-One from a verified commit you trust.

## Key Custody

**TBD JACK-INPUT:** key custody plan. Current state:

- Jack-only signing authority for SSH commits (per Jack-checkpoint #7)
- CI bot signing comes online when V2 cosign ships
- No backup or recovery plan banked yet
- No rotation cadence banked yet

Pending policy decisions:

- Hardware-token backup (e.g., offline yubikey at known location)
- Bridge Crew named-recovery (Jack-unavailable fallback)
- Rotation cadence (annual? on suspected compromise?)

## Threat Surface

- SWU image (Yocto build output): bedrock for printer OS. A tampered
  SWU is firmware compromise at full privilege.
- IPK packages (`klipper`, `klipper-firmware-{toolhead,bed,dsp}`,
  `mainsail`, `moonraker`, related): tampered IPK is in-image
  compromise.
- Recipe content (`meta-opencentauri/recipes-*`): tampered recipe
  changes what the build produces.
- Patches in `meta-opencentauri/recipes-apps/klipper/files/`: tampered
  patch alters Klipper behavior at build time.
- Build runner (`pono-prime`, self-hosted Linux ARM64): the cosmos SWU
  is built on the pono-prime runner per
  `.github/workflows/build-s-plus-batch.yml`. Compromise of runner
  enables arbitrary recipe tampering. Hardening tracked separately
  under fleet ops.

## Acknowledgments

- Upstream parent: [OpenCentauri/cosmos](https://github.com/OpenCentauri/cosmos)
  by OpenCentauri (the COSMOS firmware project for the Centauri Carbon).
- Companion Klipper firmware:
  [Brofalo/pono-kalico](https://github.com/Brofalo/pono-kalico) (forks
  [KalicoCrew/kalico](https://github.com/KalicoCrew/kalico)).
- See [`DIVERGENCE.md`](DIVERGENCE.md) for the live Brofalo-only commit log.
- See [`CHANGELOG.md`](CHANGELOG.md) for release history.
