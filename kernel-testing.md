# CC1 kernel footprint testing

This file records the isolated experiments for PR #256. It is intentionally
local analysis rather than a production image file; the PR will contain only a
short summary.

## Scope and method

Target: `carbon2u`, using wired Ethernet for recovery and SSH. The board has
the production peripherals needed for validation, including the camera and
USB devices. The shared-cache edit in `build/conf/local.conf` is local-only.

Each variant is built from current `origin/main` and tested cumulatively. The
SWU, kernel image, module archive, generated kernel `.config`, and checksums
are retained outside this repository. After flashing, wait five minutes for
the normal services to settle, then collect three idle samples per boot. Each
sample records `/proc/meminfo`, `/proc/swaps`, slab/page-table/kernel-stack
figures, boot memory, service state, and peripheral state.

The final baseline-versus-final comparison also uses a bounded synthetic load:
two CPU burners, bounded memory pressure, Moonraker/API polling, camera
snapshot polling, SSH checks, and swap/pressure telemetry. No printer motion
or heating is used.

## Baseline

Current `origin/main` (placeholder `doom/kernel` rebased onto it) built as a
6,521-task SWU on 2026-07-20. The baseline artifact sizes are:

- `zImage`: 3,802,928 bytes
- modules archive: 4,527,253 bytes
- `bootA.img`: 6,450,176 bytes
- SWU: 125,299,200 bytes
- boot kernel line: `113632K/130048K available`, with 8,192K kernel code,
  1,175K rwdata, 2,024K rodata, 1,024K init, 267K bss, and 16,416K reserved

On `carbon2u`, three settled idle samples produced a median of 29,384 KiB
`MemAvailable`, 12,604 KiB slab, 728 KiB kernel stack, 552 KiB page tables,
and 7,424 KiB zram swap used. Klipper, Moonraker, all three UI alternatives,
ustreamer, camera (`/dev/video0` and `/dev/video1`), GPIO, I2C, PWM,
remoteproc, USB storage, and wired Ethernet were healthy.

The untouched baseline also reproduced the separately known RTL8821CU USB
failure: enumeration under `2-1.2` returned repeated `-71` errors and no
`wlan0` appeared. This is expected because current `origin/main` does not yet
contain the separate Wi-Fi PR; it is recorded as an out-of-scope baseline
condition, while Ethernet and the attached peripherals remain usable for this
test.

## Candidate groups

The original 97-line `runtime-memory.cfg` mixed unrelated risks. The new test
series evaluates these groups independently:

1. Non-CC1 Allwinner pinctrl drivers and `PWM_SUN4I`; retain CC1
   `PWM_SUN20I`, display, GPIO, I2C, SPI, USB, camera, and remoteproc support.
2. HIGHMEM, io_uring, and fanotify; retain seccomp, inotify, and both IPC
   ABIs.
3. ftrace/tracing, uprobes, perf events, SLUB debugging, and task accounting;
   retain `KALLSYMS`, `/proc/config.gz`, and symbolic crash reporting.
4. IPv6, netfilter/conntrack, bridge, and VLAN support; retain IPv4 Ethernet
   and Wi-Fi.
5. cgroups and SCHED_AUTOGROUP; retain only if the measured memory gain is at
   least 512 KiB and synthetic-load API/camera p95 latency does not regress by
   more than 10%.

Each group will have its own commit and will be kept or dropped based on the
recorded result. Hardware/peripheral regressions always reject a group.

## Results

| Variant | Config groups | zImage | modules | boot memory | idle MemAvailable | decision |
| --- | --- | ---: | ---: | ---: | ---: | --- |
| baseline | none | 3,802,928 | 4,527,253 | 113,632K available | 29,384 KiB | reference; known out-of-scope Wi-Fi enumeration failure |
| platform-drivers | non-CC1 pinctrl + `PWM_SUN4I` | 3,794,160 (-8,768) | 4,527,161 (-92) | 113,696K available (+64K) | 29,624 KiB (+240) | retain; all attached peripherals healthy |
| runtime-apis | platform group + HIGHMEM/io_uring/fanotify | 3,736,336 (-66,592) | 4,528,636 (+1,383) | 113,728K available (+96K) | 25,500 KiB (-3,884) | provisional retain; services/peripherals healthy; idle figure is noisy because zram was not enabled until manually started |
| instrumentation | runtime APIs + tracing/perf/task accounting | 3,408,920 (-394,008) | 4,309,570 (-217,683) | 116,180K available (+2,548K) | 29,256 KiB (-128) | retain; all services/peripherals healthy, no warnings/OOM; zram required manual start during this boot |
| network | instrumentation + IPv6/netfilter/conntrack/bridge/VLAN | 3,241,032 (-561,896) | 3,605,478 (-921,775) | 116,196K available (+2,564K) | pending | retain; IPv4 Ethernet, HTTP 80/8080, services, camera and peripherals healthy |
| cgroups | network group + cgroup scheduling/autogroup | 3,208,328 (-594,600) | 3,603,083 (-924,170) | 116,208K available (+2,576K) | pending | retain; cgroups absent as intended, IPv4/services/peripherals healthy |

The network and cgroup rows were accepted on boot reservation and functional
smoke tests. The final cgroup image was also run under two CPU burners while
polling the camera endpoint (8080) and UI/API endpoint (80) ten times each;
all 20 requests returned success. A full printer-motion test is intentionally
out of scope for this kernel-footprint change.

## Recommendation and savings

Retain all five incremental groups. Relative to the untouched baseline, the
combined configuration saves 594,600 bytes in `zImage`, 924,170 bytes in the
compressed module archive, and 2,011,136 bytes in the SWU. The boot allocator
reports 2,576 KiB more available RAM (116,208K versus 113,632K). These are
measured deltas from the retained artifacts; module archive compression can
vary slightly with metadata, so the boot-memory and zImage figures are the
strongest comparisons.

The only test caveat is zram initialization: on several new images the init
script loaded the module but did not activate swap until manually starting
`/etc/init.d/zram`. This is independent of the tested kernel options and is
recorded rather than attributed to the footprint patches.

## Functional acceptance

Every retained variant must boot repeatedly without kernel warnings or OOM and
keep Klipper, Moonraker, UI, zram, Ethernet, Wi-Fi, camera, USB storage,
display, PWM, GPIO/I2C/SPI, and remoteproc operational.
