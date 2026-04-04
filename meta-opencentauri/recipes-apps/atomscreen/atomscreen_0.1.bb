inherit cargo update-rc.d

SUMMARY = "Atomscreen - Interface for Klipper"
DESCRIPTION = "A native klipper touchscreen graphical user interface that works \
    both with and without a display server such as X11/Wayland"
HOMEPAGE = "https://github.com/OpenCentauri/atomscreen"
LICENSE = "GPL-3.0-only"
LIC_FILES_CHKSUM = "file://LICENSE;md5=1ebbd3e34237af26da5dc08a4e440464"

S = "${WORKDIR}/git"

DEPENDS += "fontconfig openssl pkgconfig-native"
RDEPENDS:${PN} += "fontconfig gui-switcher"

INSANE_SKIP:${PN} = "already-stripped"

INITSCRIPT_NAME = "atomscreen"
INITSCRIPT_PARAMS = "disable"

export BINDGEN_EXTRA_CLANG_ARGS = "${TOOLCHAIN_OPTIONS} --sysroot=${STAGING_DIR_TARGET} -I${STAGING_INCDIR}"


do_install:append() {
    install -d ${D}${sysconfdir}/klipper
    install -d ${D}${sysconfdir}/klipper/config
    install -m 0644 ${WORKDIR}/atomscreen.toml ${D}${sysconfdir}/klipper/config/

    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/atomscreen.init ${D}${sysconfdir}/init.d/atomscreen
}

FILES:${PN} += " \
    ${sysconfdir}/init.d/atomscreen \
    ${sysconfdir}/atomscreen \
"

CONFFILES:${PN} = " \
    ${sysconfdir}/klipper/config/atomscreen.toml \
"

PR = "r2"

SRCREV = "a8aec6f69fa84c66ae1bfd6a1316a65d24c79dd5"
SRC_URI = " \
    git://github.com/jamesturton/atomscreen.git;protocol=https;branch=crates-slint \
    file://atomscreen.toml \
    file://atomscreen.init \
    crate://crates.io/ab_glyph/0.2.32 \
    crate://crates.io/ab_glyph_rasterizer/0.1.10 \
    crate://crates.io/accesskit/0.22.0 \
    crate://crates.io/accesskit_atspi_common/0.15.0 \
    crate://crates.io/accesskit_consumer/0.32.0 \
    crate://crates.io/accesskit_macos/0.23.0 \
    crate://crates.io/accesskit_unix/0.18.0 \
    crate://crates.io/accesskit_windows/0.30.0 \
    crate://crates.io/accesskit_winit/0.30.0 \
    crate://crates.io/adler2/2.0.1 \
    crate://crates.io/ahash/0.8.12 \
    crate://crates.io/aho-corasick/1.1.4 \
    crate://crates.io/aligned/0.4.3 \
    crate://crates.io/aligned-vec/0.6.4 \
    crate://crates.io/allocator-api2/0.2.21 \
    crate://crates.io/android-activity/0.6.0 \
    crate://crates.io/android-properties/0.2.2 \
    crate://crates.io/android_system_properties/0.1.5 \
    crate://crates.io/annotate-snippets/0.12.13 \
    crate://crates.io/anstream/1.0.0 \
    crate://crates.io/anstyle/1.0.14 \
    crate://crates.io/anstyle-parse/1.0.0 \
    crate://crates.io/anstyle-query/1.1.5 \
    crate://crates.io/anstyle-wincon/3.0.11 \
    crate://crates.io/anyhow/1.0.102 \
    crate://crates.io/arbitrary/1.4.2 \
    crate://crates.io/arg_enum_proc_macro/0.3.4 \
    crate://crates.io/arrayref/0.3.9 \
    crate://crates.io/arrayvec/0.7.6 \
    crate://crates.io/as-raw-xcb-connection/1.0.1 \
    crate://crates.io/as-slice/0.2.1 \
    crate://crates.io/async-broadcast/0.7.2 \
    crate://crates.io/async-channel/2.5.0 \
    crate://crates.io/async-executor/1.14.0 \
    crate://crates.io/async-io/2.6.0 \
    crate://crates.io/async-lock/3.4.2 \
    crate://crates.io/async-process/2.5.0 \
    crate://crates.io/async-recursion/1.1.1 \
    crate://crates.io/async-signal/0.2.13 \
    crate://crates.io/async-task/4.7.1 \
    crate://crates.io/async-trait/0.1.89 \
    crate://crates.io/atomic-waker/1.1.2 \
    crate://crates.io/atspi/0.29.0 \
    crate://crates.io/atspi-common/0.13.0 \
    crate://crates.io/atspi-proxies/0.13.0 \
    crate://crates.io/auto_enums/0.8.8 \
    crate://crates.io/autocfg/1.5.0 \
    crate://crates.io/av-scenechange/0.14.1 \
    crate://crates.io/av1-grain/0.2.5 \
    crate://crates.io/avif-serialize/0.8.8 \
    crate://crates.io/base64/0.21.7 \
    crate://crates.io/base64/0.22.1 \
    crate://crates.io/bincode/2.0.1 \
    crate://crates.io/bindgen/0.68.1 \
    crate://crates.io/bindgen/0.72.1 \
    crate://crates.io/bit_field/0.10.3 \
    crate://crates.io/bitflags/1.3.2 \
    crate://crates.io/bitflags/2.11.0 \
    crate://crates.io/bitstream-io/4.9.0 \
    crate://crates.io/bitvec/1.0.1 \
    crate://crates.io/block-buffer/0.10.4 \
    crate://crates.io/block2/0.5.1 \
    crate://crates.io/block2/0.6.2 \
    crate://crates.io/blocking/1.6.2 \
    crate://crates.io/borsh/1.6.0 \
    crate://crates.io/built/0.8.0 \
    crate://crates.io/bumpalo/3.20.2 \
    crate://crates.io/by_address/1.2.1 \
    crate://crates.io/bytemuck/1.25.0 \
    crate://crates.io/bytemuck_derive/1.10.2 \
    crate://crates.io/byteorder/1.5.0 \
    crate://crates.io/byteorder-lite/0.1.0 \
    crate://crates.io/bytes/1.11.1 \
    crate://crates.io/calloop/0.13.0 \
    crate://crates.io/calloop/0.14.4 \
    crate://crates.io/calloop-wayland-source/0.3.0 \
    crate://crates.io/calloop-wayland-source/0.4.1 \
    crate://crates.io/cc/1.2.57 \
    crate://crates.io/cesu8/1.1.0 \
    crate://crates.io/cexpr/0.6.0 \
    crate://crates.io/cfg-if/1.0.4 \
    crate://crates.io/cfg_aliases/0.2.1 \
    crate://crates.io/cgl/0.3.2 \
    crate://crates.io/chrono/0.4.44 \
    crate://crates.io/clang-sys/1.8.1 \
    crate://crates.io/clap/4.6.0 \
    crate://crates.io/clap_builder/4.6.0 \
    crate://crates.io/clap_derive/4.6.0 \
    crate://crates.io/clap_lex/1.1.0 \
    crate://crates.io/clipboard-win/5.4.1 \
    crate://crates.io/clru/0.6.3 \
    crate://crates.io/color_quant/1.1.0 \
    crate://crates.io/colorchoice/1.0.5 \
    crate://crates.io/combine/4.6.7 \
    crate://crates.io/concurrent-queue/2.5.0 \
    crate://crates.io/const-field-offset/0.1.5 \
    crate://crates.io/const-field-offset-macro/0.1.5 \
    crate://crates.io/convert_case/0.10.0 \
    crate://crates.io/copypasta/0.10.2 \
    crate://crates.io/core-foundation/0.10.1 \
    crate://crates.io/core-foundation/0.9.4 \
    crate://crates.io/core-foundation-sys/0.8.7 \
    crate://crates.io/core-graphics/0.23.2 \
    crate://crates.io/core-graphics-types/0.1.3 \
    crate://crates.io/core2/0.4.0 \
    crate://crates.io/core_maths/0.1.1 \
    crate://crates.io/countme/3.0.1 \
    crate://crates.io/cpp/0.5.10 \
    crate://crates.io/cpp_build/0.5.10 \
    crate://crates.io/cpp_common/0.5.10 \
    crate://crates.io/cpp_macros/0.5.10 \
    crate://crates.io/cpufeatures/0.2.17 \
    crate://crates.io/crc32fast/1.5.0 \
    crate://crates.io/critical-section/1.2.0 \
    crate://crates.io/crossbeam-channel/0.5.15 \
    crate://crates.io/crossbeam-deque/0.8.6 \
    crate://crates.io/crossbeam-epoch/0.9.18 \
    crate://crates.io/crossbeam-utils/0.8.21 \
    crate://crates.io/crunchy/0.2.4 \
    crate://crates.io/crypto-common/0.1.7 \
    crate://crates.io/ctor-lite/0.1.2 \
    crate://crates.io/cursor-icon/1.2.0 \
    crate://crates.io/data-url/0.3.2 \
    crate://crates.io/derive_more/2.1.1 \
    crate://crates.io/derive_more-impl/2.1.1 \
    crate://crates.io/derive_utils/0.15.1 \
    crate://crates.io/digest/0.10.7 \
    crate://crates.io/dispatch/0.2.0 \
    crate://crates.io/dispatch2/0.3.1 \
    crate://crates.io/displaydoc/0.2.5 \
    crate://crates.io/dlib/0.5.3 \
    crate://crates.io/downcast-rs/1.2.1 \
    crate://crates.io/dpi/0.1.2 \
    crate://crates.io/drm/0.14.2 \
    crate://crates.io/drm-ffi/0.9.1 \
    crate://crates.io/drm-fourcc/2.2.0 \
    crate://crates.io/drm-sys/0.8.1 \
    crate://crates.io/either/1.15.0 \
    crate://crates.io/endi/1.1.1 \
    crate://crates.io/enumflags2/0.7.12 \
    crate://crates.io/enumflags2_derive/0.7.12 \
    crate://crates.io/equator/0.4.2 \
    crate://crates.io/equator-macro/0.4.2 \
    crate://crates.io/equivalent/1.0.2 \
    crate://crates.io/errno/0.3.14 \
    crate://crates.io/error-code/3.3.2 \
    crate://crates.io/euclid/0.22.13 \
    crate://crates.io/evdev/0.13.2 \
    crate://crates.io/event-listener/5.4.1 \
    crate://crates.io/event-listener-strategy/0.5.4 \
    crate://crates.io/exr/1.74.0 \
    crate://crates.io/fastrand/2.3.0 \
    crate://crates.io/fastwebsockets/0.10.0 \
    crate://crates.io/fax/0.2.6 \
    crate://crates.io/fax_derive/0.2.0 \
    crate://crates.io/fdeflate/0.3.7 \
    crate://crates.io/femtovg/0.20.4 \
    crate://crates.io/field-offset/0.3.6 \
    crate://crates.io/filetime/0.2.27 \
    crate://crates.io/find-msvc-tools/0.1.9 \
    crate://crates.io/flate2/1.1.9 \
    crate://crates.io/float-cmp/0.9.0 \
    crate://crates.io/fnv/1.0.7 \
    crate://crates.io/foldhash/0.1.5 \
    crate://crates.io/foldhash/0.2.0 \
    crate://crates.io/font-types/0.10.1 \
    crate://crates.io/fontdb/0.23.0 \
    crate://crates.io/fontdue/0.9.3 \
    crate://crates.io/fontique/0.7.0 \
    crate://crates.io/foreign-types/0.3.2 \
    crate://crates.io/foreign-types/0.5.0 \
    crate://crates.io/foreign-types-macros/0.2.3 \
    crate://crates.io/foreign-types-shared/0.1.1 \
    crate://crates.io/foreign-types-shared/0.3.1 \
    crate://crates.io/form_urlencoded/1.2.2 \
    crate://crates.io/funty/2.0.0 \
    crate://crates.io/futures/0.3.32 \
    crate://crates.io/futures-channel/0.3.32 \
    crate://crates.io/futures-core/0.3.32 \
    crate://crates.io/futures-executor/0.3.32 \
    crate://crates.io/futures-io/0.3.32 \
    crate://crates.io/futures-lite/2.6.1 \
    crate://crates.io/futures-macro/0.3.32 \
    crate://crates.io/futures-sink/0.3.32 \
    crate://crates.io/futures-task/0.3.32 \
    crate://crates.io/futures-util/0.3.32 \
    crate://crates.io/gbm/0.18.0 \
    crate://crates.io/gbm-sys/0.4.0 \
    crate://crates.io/generic-array/0.14.7 \
    crate://crates.io/gethostname/1.1.0 \
    crate://crates.io/getopts/0.2.24 \
    crate://crates.io/getrandom/0.2.17 \
    crate://crates.io/getrandom/0.3.4 \
    crate://crates.io/getrandom/0.4.2 \
    crate://crates.io/gif/0.14.1 \
    crate://crates.io/gl_generator/0.14.0 \
    crate://crates.io/glob/0.3.3 \
    crate://crates.io/glow/0.16.0 \
    crate://crates.io/glutin/0.32.3 \
    crate://crates.io/glutin-winit/0.5.0 \
    crate://crates.io/glutin_egl_sys/0.7.1 \
    crate://crates.io/glutin_glx_sys/0.6.1 \
    crate://crates.io/glutin_wgl_sys/0.6.1 \
    crate://crates.io/half/2.7.1 \
    crate://crates.io/harfrust/0.3.2 \
    crate://crates.io/hashbrown/0.14.5 \
    crate://crates.io/hashbrown/0.15.5 \
    crate://crates.io/hashbrown/0.16.1 \
    crate://crates.io/heck/0.5.0 \
    crate://crates.io/hermit-abi/0.3.9 \
    crate://crates.io/hermit-abi/0.5.2 \
    crate://crates.io/hex/0.4.3 \
    crate://crates.io/home/0.5.12 \
    crate://crates.io/htmlparser/0.2.1 \
    crate://crates.io/http/1.4.0 \
    crate://crates.io/http-body/1.0.1 \
    crate://crates.io/http-body-util/0.1.3 \
    crate://crates.io/httparse/1.10.1 \
    crate://crates.io/httpdate/1.0.3 \
    crate://crates.io/hyper/1.8.1 \
    crate://crates.io/hyper-tls/0.6.0 \
    crate://crates.io/hyper-util/0.1.20 \
    crate://crates.io/i-slint-backend-linuxkms/1.15.1 \
    crate://crates.io/i-slint-backend-qt/1.15.1 \
    crate://crates.io/i-slint-backend-selector/1.15.1 \
    crate://crates.io/i-slint-backend-winit/1.15.1 \
    crate://crates.io/i-slint-common/1.15.1 \
    crate://crates.io/i-slint-compiler/1.15.1 \
    crate://crates.io/i-slint-core/1.15.1 \
    crate://crates.io/i-slint-core-macros/1.15.1 \
    crate://crates.io/i-slint-renderer-femtovg/1.15.1 \
    crate://crates.io/i-slint-renderer-skia/1.15.1 \
    crate://crates.io/i-slint-renderer-software/1.15.1 \
    crate://crates.io/iana-time-zone/0.1.65 \
    crate://crates.io/iana-time-zone-haiku/0.1.2 \
    crate://crates.io/icu_collections/2.1.1 \
    crate://crates.io/icu_locale_core/2.1.1 \
    crate://crates.io/icu_normalizer/2.1.1 \
    crate://crates.io/icu_normalizer_data/2.1.1 \
    crate://crates.io/icu_properties/2.1.2 \
    crate://crates.io/icu_properties_data/2.1.2 \
    crate://crates.io/icu_provider/2.1.1 \
    crate://crates.io/id-arena/2.3.0 \
    crate://crates.io/idna/1.1.0 \
    crate://crates.io/idna_adapter/1.2.1 \
    crate://crates.io/image/0.25.10 \
    crate://crates.io/image-webp/0.2.4 \
    crate://crates.io/imagesize/0.14.0 \
    crate://crates.io/imgref/1.12.0 \
    crate://crates.io/indexmap/2.13.0 \
    crate://crates.io/input/0.9.1 \
    crate://crates.io/input-sys/1.18.0 \
    crate://crates.io/integer-sqrt/0.1.5 \
    crate://crates.io/interpolate_name/0.2.4 \
    crate://crates.io/io-lifetimes/1.0.11 \
    crate://crates.io/ipnet/2.12.0 \
    crate://crates.io/iri-string/0.7.10 \
    crate://crates.io/is_terminal_polyfill/1.70.2 \
    crate://crates.io/itertools/0.13.0 \
    crate://crates.io/itertools/0.14.0 \
    crate://crates.io/itoa/1.0.17 \
    crate://crates.io/jni/0.21.1 \
    crate://crates.io/jni-sys/0.3.0 \
    crate://crates.io/jobserver/0.1.34 \
    crate://crates.io/js-sys/0.3.91 \
    crate://crates.io/keyboard-types/0.7.0 \
    crate://crates.io/khronos_api/3.1.0 \
    crate://crates.io/kurbo/0.12.0 \
    crate://crates.io/kurbo/0.13.0 \
    crate://crates.io/lazy_static/1.5.0 \
    crate://crates.io/lazycell/1.3.0 \
    crate://crates.io/leb128fmt/0.1.0 \
    crate://crates.io/lebe/0.5.3 \
    crate://crates.io/libc/0.2.183 \
    crate://crates.io/libfuzzer-sys/0.4.12 \
    crate://crates.io/libloading/0.8.9 \
    crate://crates.io/libm/0.2.16 \
    crate://crates.io/libredox/0.1.14 \
    crate://crates.io/libudev-sys/0.1.4 \
    crate://crates.io/linebender_resource_handle/0.1.1 \
    crate://crates.io/linked-hash-map/0.5.6 \
    crate://crates.io/linked_hash_set/0.1.6 \
    crate://crates.io/linux-raw-sys/0.12.1 \
    crate://crates.io/linux-raw-sys/0.4.15 \
    crate://crates.io/linux-raw-sys/0.9.4 \
    crate://crates.io/linuxfb/0.3.1 \
    crate://crates.io/litemap/0.8.1 \
    crate://crates.io/lock_api/0.4.14 \
    crate://crates.io/log/0.4.29 \
    crate://crates.io/loop9/0.1.5 \
    crate://crates.io/lyon_algorithms/1.0.19 \
    crate://crates.io/lyon_extra/1.1.0 \
    crate://crates.io/lyon_geom/1.0.19 \
    crate://crates.io/lyon_path/1.0.19 \
    crate://crates.io/maybe-rayon/0.1.1 \
    crate://crates.io/memchr/2.8.0 \
    crate://crates.io/memmap/0.7.0 \
    crate://crates.io/memmap2/0.9.10 \
    crate://crates.io/memoffset/0.9.1 \
    crate://crates.io/minimal-lexical/0.2.1 \
    crate://crates.io/miniz_oxide/0.8.9 \
    crate://crates.io/mio/1.1.1 \
    crate://crates.io/moxcms/0.8.1 \
    crate://crates.io/muda/0.17.1 \
    crate://crates.io/native-tls/0.2.18 \
    crate://crates.io/natord/1.0.9 \
    crate://crates.io/ndk/0.9.0 \
    crate://crates.io/ndk-context/0.1.1 \
    crate://crates.io/ndk-sys/0.6.0+11769913 \
    crate://crates.io/new_debug_unreachable/1.0.6 \
    crate://crates.io/nix/0.29.0 \
    crate://crates.io/nix/0.30.1 \
    crate://crates.io/nom/7.1.3 \
    crate://crates.io/nom/8.0.0 \
    crate://crates.io/noop_proc_macro/0.3.0 \
    crate://crates.io/num-bigint/0.4.6 \
    crate://crates.io/num-derive/0.4.2 \
    crate://crates.io/num-integer/0.1.46 \
    crate://crates.io/num-rational/0.4.2 \
    crate://crates.io/num-traits/0.2.19 \
    crate://crates.io/num_enum/0.7.6 \
    crate://crates.io/num_enum_derive/0.7.6 \
    crate://crates.io/objc-sys/0.3.5 \
    crate://crates.io/objc2/0.5.2 \
    crate://crates.io/objc2/0.6.4 \
    crate://crates.io/objc2-app-kit/0.2.2 \
    crate://crates.io/objc2-app-kit/0.3.2 \
    crate://crates.io/objc2-cloud-kit/0.2.2 \
    crate://crates.io/objc2-cloud-kit/0.3.2 \
    crate://crates.io/objc2-contacts/0.2.2 \
    crate://crates.io/objc2-core-data/0.2.2 \
    crate://crates.io/objc2-core-data/0.3.2 \
    crate://crates.io/objc2-core-foundation/0.3.2 \
    crate://crates.io/objc2-core-graphics/0.3.2 \
    crate://crates.io/objc2-core-image/0.2.2 \
    crate://crates.io/objc2-core-image/0.3.2 \
    crate://crates.io/objc2-core-location/0.2.2 \
    crate://crates.io/objc2-core-text/0.3.2 \
    crate://crates.io/objc2-core-video/0.3.2 \
    crate://crates.io/objc2-encode/4.1.0 \
    crate://crates.io/objc2-foundation/0.2.2 \
    crate://crates.io/objc2-foundation/0.3.2 \
    crate://crates.io/objc2-io-surface/0.3.2 \
    crate://crates.io/objc2-link-presentation/0.2.2 \
    crate://crates.io/objc2-metal/0.2.2 \
    crate://crates.io/objc2-metal/0.3.2 \
    crate://crates.io/objc2-quartz-core/0.2.2 \
    crate://crates.io/objc2-quartz-core/0.3.2 \
    crate://crates.io/objc2-symbols/0.2.2 \
    crate://crates.io/objc2-ui-kit/0.2.2 \
    crate://crates.io/objc2-ui-kit/0.3.2 \
    crate://crates.io/objc2-uniform-type-identifiers/0.2.2 \
    crate://crates.io/objc2-user-notifications/0.2.2 \
    crate://crates.io/once_cell/1.21.4 \
    crate://crates.io/once_cell_polyfill/1.70.2 \
    crate://crates.io/openssl/0.10.76 \
    crate://crates.io/openssl-macros/0.1.1 \
    crate://crates.io/openssl-probe/0.2.1 \
    crate://crates.io/openssl-src/300.5.5+3.5.5 \
    crate://crates.io/openssl-sys/0.9.112 \
    crate://crates.io/optional_struct/0.5.2 \
    crate://crates.io/optional_struct_macro/0.5.2 \
    crate://crates.io/orbclient/0.3.51 \
    crate://crates.io/ordered-stream/0.2.0 \
    crate://crates.io/owned_ttf_parser/0.25.1 \
    crate://crates.io/parking/2.2.1 \
    crate://crates.io/parking_lot/0.12.5 \
    crate://crates.io/parking_lot_core/0.9.12 \
    crate://crates.io/parley/0.7.0 \
    crate://crates.io/paste/1.0.15 \
    crate://crates.io/pastey/0.1.1 \
    crate://crates.io/peeking_take_while/0.1.2 \
    crate://crates.io/percent-encoding/2.3.2 \
    crate://crates.io/pico-args/0.5.0 \
    crate://crates.io/pin-project/1.1.11 \
    crate://crates.io/pin-project-internal/1.1.11 \
    crate://crates.io/pin-project-lite/0.2.17 \
    crate://crates.io/pin-utils/0.1.0 \
    crate://crates.io/pin-weak/1.1.0 \
    crate://crates.io/piper/0.2.5 \
    crate://crates.io/pkg-config/0.3.32 \
    crate://crates.io/plain/0.2.3 \
    crate://crates.io/png/0.17.16 \
    crate://crates.io/png/0.18.1 \
    crate://crates.io/polling/3.11.0 \
    crate://crates.io/portable-atomic/1.13.1 \
    crate://crates.io/potential_utf/0.1.4 \
    crate://crates.io/ppv-lite86/0.2.21 \
    crate://crates.io/prettyplease/0.2.37 \
    crate://crates.io/proc-macro-crate/3.5.0 \
    crate://crates.io/proc-macro2/1.0.106 \
    crate://crates.io/profiling/1.0.17 \
    crate://crates.io/profiling-procmacros/1.0.17 \
    crate://crates.io/pulldown-cmark/0.13.1 \
    crate://crates.io/pulldown-cmark-escape/0.11.0 \
    crate://crates.io/pxfm/0.1.28 \
    crate://crates.io/qoi/0.4.1 \
    crate://crates.io/qttypes/0.2.12 \
    crate://crates.io/quick-error/2.0.1 \
    crate://crates.io/quick-xml/0.38.4 \
    crate://crates.io/quick-xml/0.39.2 \
    crate://crates.io/quote/1.0.45 \
    crate://crates.io/r-efi/5.3.0 \
    crate://crates.io/r-efi/6.0.0 \
    crate://crates.io/radium/0.7.0 \
    crate://crates.io/rand/0.8.5 \
    crate://crates.io/rand/0.9.2 \
    crate://crates.io/rand_chacha/0.3.1 \
    crate://crates.io/rand_chacha/0.9.0 \
    crate://crates.io/rand_core/0.6.4 \
    crate://crates.io/rand_core/0.9.5 \
    crate://crates.io/rav1e/0.8.1 \
    crate://crates.io/ravif/0.13.0 \
    crate://crates.io/raw-window-handle/0.6.2 \
    crate://crates.io/raw-window-metal/1.1.0 \
    crate://crates.io/rayon/1.11.0 \
    crate://crates.io/rayon-core/1.13.0 \
    crate://crates.io/read-fonts/0.35.0 \
    crate://crates.io/redox_syscall/0.4.1 \
    crate://crates.io/redox_syscall/0.5.18 \
    crate://crates.io/redox_syscall/0.7.3 \
    crate://crates.io/regex/1.12.3 \
    crate://crates.io/regex-automata/0.4.14 \
    crate://crates.io/regex-syntax/0.8.10 \
    crate://crates.io/reqwest/0.13.2 \
    crate://crates.io/resvg/0.46.0 \
    crate://crates.io/rgb/0.8.53 \
    crate://crates.io/rowan/0.16.1 \
    crate://crates.io/roxmltree/0.21.1 \
    crate://crates.io/rspolib/0.1.2 \
    crate://crates.io/rustc-hash/1.1.0 \
    crate://crates.io/rustc-hash/2.1.1 \
    crate://crates.io/rustc_version/0.4.1 \
    crate://crates.io/rustix/0.38.44 \
    crate://crates.io/rustix/1.1.4 \
    crate://crates.io/rustls-pki-types/1.14.0 \
    crate://crates.io/rustversion/1.0.22 \
    crate://crates.io/rustybuzz/0.20.1 \
    crate://crates.io/same-file/1.0.6 \
    crate://crates.io/schannel/0.1.29 \
    crate://crates.io/scoped-tls/1.0.1 \
    crate://crates.io/scoped-tls-hkt/0.1.5 \
    crate://crates.io/scopeguard/1.2.0 \
    crate://crates.io/sctk-adwaita/0.10.1 \
    crate://crates.io/security-framework/3.7.0 \
    crate://crates.io/security-framework-sys/2.17.0 \
    crate://crates.io/semver/1.0.27 \
    crate://crates.io/serde/1.0.228 \
    crate://crates.io/serde_core/1.0.228 \
    crate://crates.io/serde_derive/1.0.228 \
    crate://crates.io/serde_json/1.0.149 \
    crate://crates.io/serde_repr/0.1.20 \
    crate://crates.io/serde_spanned/1.0.4 \
    crate://crates.io/sha1/0.10.6 \
    crate://crates.io/shlex/1.3.0 \
    crate://crates.io/signal-hook-registry/1.4.8 \
    crate://crates.io/simd-adler32/0.3.8 \
    crate://crates.io/simd_helpers/0.1.0 \
    crate://crates.io/simdutf8/0.1.5 \
    crate://crates.io/simplecss/0.2.2 \
    crate://crates.io/siphasher/1.0.2 \
    crate://crates.io/skia-bindings/0.90.0 \
    crate://crates.io/skia-safe/0.90.0 \
    crate://crates.io/skrifa/0.37.0 \
    crate://crates.io/slab/0.4.12 \
    crate://crates.io/slint/1.15.1 \
    crate://crates.io/slint-build/1.15.1 \
    crate://crates.io/slint-macros/1.15.1 \
    crate://crates.io/slotmap/1.1.1 \
    crate://crates.io/smallvec/1.15.1 \
    crate://crates.io/smithay-client-toolkit/0.19.2 \
    crate://crates.io/smithay-client-toolkit/0.20.0 \
    crate://crates.io/smithay-clipboard/0.7.3 \
    crate://crates.io/smol_str/0.2.2 \
    crate://crates.io/smol_str/0.3.6 \
    crate://crates.io/snafu/0.8.9 \
    crate://crates.io/snafu-derive/0.8.9 \
    crate://crates.io/socket2/0.6.3 \
    crate://crates.io/softbuffer/0.4.8 \
    crate://crates.io/spin_on/0.1.1 \
    crate://crates.io/stable_deref_trait/1.2.1 \
    crate://crates.io/static_assertions/1.1.0 \
    crate://crates.io/strict-num/0.1.1 \
    crate://crates.io/strsim/0.11.1 \
    crate://crates.io/strum/0.27.2 \
    crate://crates.io/strum_macros/0.27.2 \
    crate://crates.io/svgtypes/0.16.1 \
    crate://crates.io/swash/0.2.6 \
    crate://crates.io/syn/2.0.117 \
    crate://crates.io/sync_wrapper/1.0.2 \
    crate://crates.io/synstructure/0.13.2 \
    crate://crates.io/sys-locale/0.3.2 \
    crate://crates.io/tap/1.0.1 \
    crate://crates.io/tar/0.4.44 \
    crate://crates.io/tempfile/3.27.0 \
    crate://crates.io/text-size/1.1.1 \
    crate://crates.io/thiserror/1.0.69 \
    crate://crates.io/thiserror/2.0.18 \
    crate://crates.io/thiserror-impl/1.0.69 \
    crate://crates.io/thiserror-impl/2.0.18 \
    crate://crates.io/tiff/0.11.3 \
    crate://crates.io/tiny-skia/0.11.4 \
    crate://crates.io/tiny-skia-path/0.11.4 \
    crate://crates.io/tiny-xlib/0.2.4 \
    crate://crates.io/tinystr/0.8.2 \
    crate://crates.io/tinyvec/1.11.0 \
    crate://crates.io/tinyvec_macros/0.1.1 \
    crate://crates.io/tokio/1.50.0 \
    crate://crates.io/tokio-macros/2.6.1 \
    crate://crates.io/tokio-native-tls/0.3.1 \
    crate://crates.io/toml/0.9.12+spec-1.1.0 \
    crate://crates.io/toml_datetime/0.7.5+spec-1.1.0 \
    crate://crates.io/toml_datetime/1.0.0+spec-1.1.0 \
    crate://crates.io/toml_edit/0.24.1+spec-1.1.0 \
    crate://crates.io/toml_edit/0.25.4+spec-1.1.0 \
    crate://crates.io/toml_parser/1.0.9+spec-1.1.0 \
    crate://crates.io/toml_writer/1.0.6+spec-1.1.0 \
    crate://crates.io/tower/0.5.3 \
    crate://crates.io/tower-http/0.6.8 \
    crate://crates.io/tower-layer/0.3.3 \
    crate://crates.io/tower-service/0.3.3 \
    crate://crates.io/tracing/0.1.44 \
    crate://crates.io/tracing-attributes/0.1.31 \
    crate://crates.io/tracing-core/0.1.36 \
    crate://crates.io/try-lock/0.2.5 \
    crate://crates.io/ttf-parser/0.21.1 \
    crate://crates.io/ttf-parser/0.25.1 \
    crate://crates.io/typed-index-collections/3.5.0 \
    crate://crates.io/typenum/1.19.0 \
    crate://crates.io/udev/0.9.3 \
    crate://crates.io/uds_windows/1.2.1 \
    crate://crates.io/unicase/2.9.0 \
    crate://crates.io/unicode-bidi/0.3.18 \
    crate://crates.io/unicode-bidi-mirroring/0.4.0 \
    crate://crates.io/unicode-ccc/0.4.0 \
    crate://crates.io/unicode-ident/1.0.24 \
    crate://crates.io/unicode-linebreak/0.1.5 \
    crate://crates.io/unicode-properties/0.1.4 \
    crate://crates.io/unicode-script/0.5.8 \
    crate://crates.io/unicode-segmentation/1.12.0 \
    crate://crates.io/unicode-vo/0.1.0 \
    crate://crates.io/unicode-width/0.2.2 \
    crate://crates.io/unicode-xid/0.2.6 \
    crate://crates.io/unty/0.0.4 \
    crate://crates.io/url/2.5.8 \
    crate://crates.io/usvg/0.46.0 \
    crate://crates.io/utf-8/0.7.6 \
    crate://crates.io/utf8_iter/1.0.4 \
    crate://crates.io/utf8parse/0.2.2 \
    crate://crates.io/uuid/1.22.0 \
    crate://crates.io/v_frame/0.3.9 \
    crate://crates.io/vcpkg/0.2.15 \
    crate://crates.io/version_check/0.9.5 \
    crate://crates.io/vtable/0.3.0 \
    crate://crates.io/vtable-macro/0.3.0 \
    crate://crates.io/walkdir/2.5.0 \
    crate://crates.io/want/0.3.1 \
    crate://crates.io/wasi/0.11.1+wasi-snapshot-preview1 \
    crate://crates.io/wasip2/1.0.2+wasi-0.2.9 \
    crate://crates.io/wasip3/0.4.0+wasi-0.3.0-rc-2026-01-06 \
    crate://crates.io/wasm-bindgen/0.2.114 \
    crate://crates.io/wasm-bindgen-futures/0.4.64 \
    crate://crates.io/wasm-bindgen-macro/0.2.114 \
    crate://crates.io/wasm-bindgen-macro-support/0.2.114 \
    crate://crates.io/wasm-bindgen-shared/0.2.114 \
    crate://crates.io/wasm-encoder/0.244.0 \
    crate://crates.io/wasm-metadata/0.244.0 \
    crate://crates.io/wasmparser/0.244.0 \
    crate://crates.io/wayland-backend/0.3.14 \
    crate://crates.io/wayland-client/0.31.13 \
    crate://crates.io/wayland-csd-frame/0.3.0 \
    crate://crates.io/wayland-cursor/0.31.13 \
    crate://crates.io/wayland-protocols/0.32.11 \
    crate://crates.io/wayland-protocols-experimental/20250721.0.1 \
    crate://crates.io/wayland-protocols-misc/0.3.11 \
    crate://crates.io/wayland-protocols-plasma/0.3.11 \
    crate://crates.io/wayland-protocols-wlr/0.3.11 \
    crate://crates.io/wayland-scanner/0.31.9 \
    crate://crates.io/wayland-sys/0.31.10 \
    crate://crates.io/web-sys/0.3.91 \
    crate://crates.io/web-time/1.1.0 \
    crate://crates.io/weezl/0.1.12 \
    crate://crates.io/which/4.4.2 \
    crate://crates.io/winapi/0.3.9 \
    crate://crates.io/winapi-i686-pc-windows-gnu/0.4.0 \
    crate://crates.io/winapi-util/0.1.11 \
    crate://crates.io/winapi-x86_64-pc-windows-gnu/0.4.0 \
    crate://crates.io/windows/0.58.0 \
    crate://crates.io/windows/0.61.3 \
    crate://crates.io/windows/0.62.2 \
    crate://crates.io/windows-collections/0.2.0 \
    crate://crates.io/windows-collections/0.3.2 \
    crate://crates.io/windows-core/0.58.0 \
    crate://crates.io/windows-core/0.61.2 \
    crate://crates.io/windows-core/0.62.2 \
    crate://crates.io/windows-future/0.2.1 \
    crate://crates.io/windows-future/0.3.2 \
    crate://crates.io/windows-implement/0.58.0 \
    crate://crates.io/windows-implement/0.60.2 \
    crate://crates.io/windows-interface/0.58.0 \
    crate://crates.io/windows-interface/0.59.3 \
    crate://crates.io/windows-link/0.1.3 \
    crate://crates.io/windows-link/0.2.1 \
    crate://crates.io/windows-numerics/0.2.0 \
    crate://crates.io/windows-numerics/0.3.1 \
    crate://crates.io/windows-result/0.2.0 \
    crate://crates.io/windows-result/0.3.4 \
    crate://crates.io/windows-result/0.4.1 \
    crate://crates.io/windows-strings/0.1.0 \
    crate://crates.io/windows-strings/0.4.2 \
    crate://crates.io/windows-strings/0.5.1 \
    crate://crates.io/windows-sys/0.45.0 \
    crate://crates.io/windows-sys/0.48.0 \
    crate://crates.io/windows-sys/0.52.0 \
    crate://crates.io/windows-sys/0.59.0 \
    crate://crates.io/windows-sys/0.60.2 \
    crate://crates.io/windows-sys/0.61.2 \
    crate://crates.io/windows-targets/0.42.2 \
    crate://crates.io/windows-targets/0.48.5 \
    crate://crates.io/windows-targets/0.52.6 \
    crate://crates.io/windows-targets/0.53.5 \
    crate://crates.io/windows-threading/0.1.0 \
    crate://crates.io/windows-threading/0.2.1 \
    crate://crates.io/windows_aarch64_gnullvm/0.42.2 \
    crate://crates.io/windows_aarch64_gnullvm/0.48.5 \
    crate://crates.io/windows_aarch64_gnullvm/0.52.6 \
    crate://crates.io/windows_aarch64_gnullvm/0.53.1 \
    crate://crates.io/windows_aarch64_msvc/0.42.2 \
    crate://crates.io/windows_aarch64_msvc/0.48.5 \
    crate://crates.io/windows_aarch64_msvc/0.52.6 \
    crate://crates.io/windows_aarch64_msvc/0.53.1 \
    crate://crates.io/windows_i686_gnu/0.42.2 \
    crate://crates.io/windows_i686_gnu/0.48.5 \
    crate://crates.io/windows_i686_gnu/0.52.6 \
    crate://crates.io/windows_i686_gnu/0.53.1 \
    crate://crates.io/windows_i686_gnullvm/0.52.6 \
    crate://crates.io/windows_i686_gnullvm/0.53.1 \
    crate://crates.io/windows_i686_msvc/0.42.2 \
    crate://crates.io/windows_i686_msvc/0.48.5 \
    crate://crates.io/windows_i686_msvc/0.52.6 \
    crate://crates.io/windows_i686_msvc/0.53.1 \
    crate://crates.io/windows_x86_64_gnu/0.42.2 \
    crate://crates.io/windows_x86_64_gnu/0.48.5 \
    crate://crates.io/windows_x86_64_gnu/0.52.6 \
    crate://crates.io/windows_x86_64_gnu/0.53.1 \
    crate://crates.io/windows_x86_64_gnullvm/0.42.2 \
    crate://crates.io/windows_x86_64_gnullvm/0.48.5 \
    crate://crates.io/windows_x86_64_gnullvm/0.52.6 \
    crate://crates.io/windows_x86_64_gnullvm/0.53.1 \
    crate://crates.io/windows_x86_64_msvc/0.42.2 \
    crate://crates.io/windows_x86_64_msvc/0.48.5 \
    crate://crates.io/windows_x86_64_msvc/0.52.6 \
    crate://crates.io/windows_x86_64_msvc/0.53.1 \
    crate://crates.io/winit/0.30.13 \
    crate://crates.io/winnow/0.7.15 \
    crate://crates.io/wit-bindgen/0.51.0 \
    crate://crates.io/wit-bindgen-core/0.51.0 \
    crate://crates.io/wit-bindgen-rust/0.51.0 \
    crate://crates.io/wit-bindgen-rust-macro/0.51.0 \
    crate://crates.io/wit-component/0.244.0 \
    crate://crates.io/wit-parser/0.244.0 \
    crate://crates.io/write-fonts/0.43.0 \
    crate://crates.io/writeable/0.6.2 \
    crate://crates.io/wyz/0.5.1 \
    crate://crates.io/x11-clipboard/0.9.3 \
    crate://crates.io/x11-dl/2.21.0 \
    crate://crates.io/x11rb/0.13.2 \
    crate://crates.io/x11rb-protocol/0.13.2 \
    crate://crates.io/xattr/1.6.1 \
    crate://crates.io/xcursor/0.3.10 \
    crate://crates.io/xkbcommon/0.9.0 \
    crate://crates.io/xkbcommon-dl/0.4.2 \
    crate://crates.io/xkeysym/0.2.1 \
    crate://crates.io/xml-rs/0.8.28 \
    crate://crates.io/xmlwriter/0.1.0 \
    crate://crates.io/y4m/0.8.0 \
    crate://crates.io/yazi/0.2.1 \
    crate://crates.io/yeslogic-fontconfig-sys/6.0.0 \
    crate://crates.io/yoke/0.8.1 \
    crate://crates.io/yoke-derive/0.8.1 \
    crate://crates.io/zbus/5.14.0 \
    crate://crates.io/zbus-lockstep/0.5.2 \
    crate://crates.io/zbus-lockstep-macros/0.5.2 \
    crate://crates.io/zbus_macros/5.14.0 \
    crate://crates.io/zbus_names/4.3.1 \
    crate://crates.io/zbus_xml/5.1.0 \
    crate://crates.io/zeno/0.3.3 \
    crate://crates.io/zerocopy/0.8.42 \
    crate://crates.io/zerocopy-derive/0.8.42 \
    crate://crates.io/zerofrom/0.1.6 \
    crate://crates.io/zerofrom-derive/0.1.6 \
    crate://crates.io/zeroize/1.8.2 \
    crate://crates.io/zerotrie/0.2.3 \
    crate://crates.io/zerovec/0.11.5 \
    crate://crates.io/zerovec-derive/0.11.2 \
    crate://crates.io/zmij/1.0.21 \
    crate://crates.io/zune-core/0.5.1 \
    crate://crates.io/zune-inflate/0.2.54 \
    crate://crates.io/zune-jpeg/0.5.13 \
    crate://crates.io/zvariant/5.10.0 \
    crate://crates.io/zvariant_derive/5.10.0 \
    crate://crates.io/zvariant_utils/3.3.0 \
"

SRC_URI[ab_glyph-0.2.32.sha256sum] = "01c0457472c38ea5bd1c3b5ada5e368271cb550be7a4ca4a0b4634e9913f6cc2"
SRC_URI[ab_glyph_rasterizer-0.1.10.sha256sum] = "366ffbaa4442f4684d91e2cd7c5ea7c4ed8add41959a31447066e279e432b618"
SRC_URI[accesskit-0.22.0.sha256sum] = "3eca13c82f9a5cd813120b2e9b6a5d10532c6e4cd140c295cebd1f770095c8a5"
SRC_URI[accesskit_atspi_common-0.15.0.sha256sum] = "3eb9cc46b7fb6987c4f891f0301b230b29d9e69b4854f060a0cf41fbc407ab77"
SRC_URI[accesskit_consumer-0.32.0.sha256sum] = "69d880a613f29621c90e801feec40f5dd61d837d7e20bf9b67676d45e7364a36"
SRC_URI[accesskit_macos-0.23.0.sha256sum] = "5b0ddfc3fe3d457d11cc1c4989105986a03583a1d54d0c25053118944b62e100"
SRC_URI[accesskit_unix-0.18.0.sha256sum] = "d5d552169ef018149966ed139bb0311c6947b3343e9140d1b9f88d69da9528fd"
SRC_URI[accesskit_windows-0.30.0.sha256sum] = "d277279d0a3b0c0021dd110b55aa1fe326b09ee2cbc338df28f847c7daf94e25"
SRC_URI[accesskit_winit-0.30.0.sha256sum] = "db08dff285306264a1de127ea07bb9e7a1ed71bd8593c168d0731caa782516c9"
SRC_URI[adler2-2.0.1.sha256sum] = "320119579fcad9c21884f5c4861d16174d0e06250625266f50fe6898340abefa"
SRC_URI[ahash-0.8.12.sha256sum] = "5a15f179cd60c4584b8a8c596927aadc462e27f2ca70c04e0071964a73ba7a75"
SRC_URI[aho-corasick-1.1.4.sha256sum] = "ddd31a130427c27518df266943a5308ed92d4b226cc639f5a8f1002816174301"
SRC_URI[aligned-0.4.3.sha256sum] = "ee4508988c62edf04abd8d92897fca0c2995d907ce1dfeaf369dac3716a40685"
SRC_URI[aligned-vec-0.6.4.sha256sum] = "dc890384c8602f339876ded803c97ad529f3842aba97f6392b3dba0dd171769b"
SRC_URI[allocator-api2-0.2.21.sha256sum] = "683d7910e743518b0e34f1186f92494becacb047c7b6bf616c96772180fef923"
SRC_URI[android-activity-0.6.0.sha256sum] = "ef6978589202a00cd7e118380c448a08b6ed394c3a8df3a430d0898e3a42d046"
SRC_URI[android-properties-0.2.2.sha256sum] = "fc7eb209b1518d6bb87b283c20095f5228ecda460da70b44f0802523dea6da04"
SRC_URI[android_system_properties-0.1.5.sha256sum] = "819e7219dbd41043ac279b19830f2efc897156490d7fd6ea916720117ee66311"
SRC_URI[annotate-snippets-0.12.13.sha256sum] = "74fc7650eedcb2fee505aad48491529e408f0e854c2d9f63eb86c1361b9b3f93"
SRC_URI[anstream-1.0.0.sha256sum] = "824a212faf96e9acacdbd09febd34438f8f711fb84e09a8916013cd7815ca28d"
SRC_URI[anstyle-1.0.14.sha256sum] = "940b3a0ca603d1eade50a4846a2afffd5ef57a9feac2c0e2ec2e14f9ead76000"
SRC_URI[anstyle-parse-1.0.0.sha256sum] = "52ce7f38b242319f7cabaa6813055467063ecdc9d355bbb4ce0c68908cd8130e"
SRC_URI[anstyle-query-1.1.5.sha256sum] = "40c48f72fd53cd289104fc64099abca73db4166ad86ea0b4341abe65af83dadc"
SRC_URI[anstyle-wincon-3.0.11.sha256sum] = "291e6a250ff86cd4a820112fb8898808a366d8f9f58ce16d1f538353ad55747d"
SRC_URI[anyhow-1.0.102.sha256sum] = "7f202df86484c868dbad7eaa557ef785d5c66295e41b460ef922eca0723b842c"
SRC_URI[arbitrary-1.4.2.sha256sum] = "c3d036a3c4ab069c7b410a2ce876bd74808d2d0888a82667669f8e783a898bf1"
SRC_URI[arg_enum_proc_macro-0.3.4.sha256sum] = "0ae92a5119aa49cdbcf6b9f893fe4e1d98b04ccbf82ee0584ad948a44a734dea"
SRC_URI[arrayref-0.3.9.sha256sum] = "76a2e8124351fda1ef8aaaa3bbd7ebbcb486bbcd4225aca0aa0d84bb2db8fecb"
SRC_URI[arrayvec-0.7.6.sha256sum] = "7c02d123df017efcdfbd739ef81735b36c5ba83ec3c59c80a9d7ecc718f92e50"
SRC_URI[as-raw-xcb-connection-1.0.1.sha256sum] = "175571dd1d178ced59193a6fc02dde1b972eb0bc56c892cde9beeceac5bf0f6b"
SRC_URI[as-slice-0.2.1.sha256sum] = "516b6b4f0e40d50dcda9365d53964ec74560ad4284da2e7fc97122cd83174516"
SRC_URI[async-broadcast-0.7.2.sha256sum] = "435a87a52755b8f27fcf321ac4f04b2802e337c8c4872923137471ec39c37532"
SRC_URI[async-channel-2.5.0.sha256sum] = "924ed96dd52d1b75e9c1a3e6275715fd320f5f9439fb5a4a11fa51f4221158d2"
SRC_URI[async-executor-1.14.0.sha256sum] = "c96bf972d85afc50bf5ab8fe2d54d1586b4e0b46c97c50a0c9e71e2f7bcd812a"
SRC_URI[async-io-2.6.0.sha256sum] = "456b8a8feb6f42d237746d4b3e9a178494627745c3c56c6ea55d92ba50d026fc"
SRC_URI[async-lock-3.4.2.sha256sum] = "290f7f2596bd5b78a9fec8088ccd89180d7f9f55b94b0576823bbbdc72ee8311"
SRC_URI[async-process-2.5.0.sha256sum] = "fc50921ec0055cdd8a16de48773bfeec5c972598674347252c0399676be7da75"
SRC_URI[async-recursion-1.1.1.sha256sum] = "3b43422f69d8ff38f95f1b2bb76517c91589a924d1559a0e935d7c8ce0274c11"
SRC_URI[async-signal-0.2.13.sha256sum] = "43c070bbf59cd3570b6b2dd54cd772527c7c3620fce8be898406dd3ed6adc64c"
SRC_URI[async-task-4.7.1.sha256sum] = "8b75356056920673b02621b35afd0f7dda9306d03c79a30f5c56c44cf256e3de"
SRC_URI[async-trait-0.1.89.sha256sum] = "9035ad2d096bed7955a320ee7e2230574d28fd3c3a0f186cbea1ff3c7eed5dbb"
SRC_URI[atomic-waker-1.1.2.sha256sum] = "1505bd5d3d116872e7271a6d4e16d81d0c8570876c8de68093a09ac269d8aac0"
SRC_URI[atspi-0.29.0.sha256sum] = "c77886257be21c9cd89a4ae7e64860c6f0eefca799bb79127913052bd0eefb3d"
SRC_URI[atspi-common-0.13.0.sha256sum] = "20c5617155740c98003016429ad13fe43ce7a77b007479350a9f8bf95a29f63d"
SRC_URI[atspi-proxies-0.13.0.sha256sum] = "2230e48787ed3eb4088996eab66a32ca20c0b67bbd4fd6cdfe79f04f1f04c9fc"
SRC_URI[auto_enums-0.8.8.sha256sum] = "65398a2893f41bce5c9259f6e1a4f03fbae40637c1bdc755b4f387f48c613b03"
SRC_URI[autocfg-1.5.0.sha256sum] = "c08606f8c3cbf4ce6ec8e28fb0014a2c086708fe954eaa885384a6165172e7e8"
SRC_URI[av-scenechange-0.14.1.sha256sum] = "0f321d77c20e19b92c39e7471cf986812cbb46659d2af674adc4331ef3f18394"
SRC_URI[av1-grain-0.2.5.sha256sum] = "8cfddb07216410377231960af4fcab838eaa12e013417781b78bd95ee22077f8"
SRC_URI[avif-serialize-0.8.8.sha256sum] = "375082f007bd67184fb9c0374614b29f9aaa604ec301635f72338bb65386a53d"
SRC_URI[base64-0.21.7.sha256sum] = "9d297deb1925b89f2ccc13d7635fa0714f12c87adce1c75356b39ca9b7178567"
SRC_URI[base64-0.22.1.sha256sum] = "72b3254f16251a8381aa12e40e3c4d2f0199f8c6508fbecb9d91f575e0fbb8c6"
SRC_URI[bincode-2.0.1.sha256sum] = "36eaf5d7b090263e8150820482d5d93cd964a81e4019913c972f4edcc6edb740"
SRC_URI[bindgen-0.68.1.sha256sum] = "726e4313eb6ec35d2730258ad4e15b547ee75d6afaa1361a922e78e59b7d8078"
SRC_URI[bindgen-0.72.1.sha256sum] = "993776b509cfb49c750f11b8f07a46fa23e0a1386ffc01fb1e7d343efc387895"
SRC_URI[bit_field-0.10.3.sha256sum] = "1e4b40c7323adcfc0a41c4b88143ed58346ff65a288fc144329c5c45e05d70c6"
SRC_URI[bitflags-1.3.2.sha256sum] = "bef38d45163c2f1dde094a7dfd33ccf595c92905c8f8f4fdc18d06fb1037718a"
SRC_URI[bitflags-2.11.0.sha256sum] = "843867be96c8daad0d758b57df9392b6d8d271134fce549de6ce169ff98a92af"
SRC_URI[bitstream-io-4.9.0.sha256sum] = "60d4bd9d1db2c6bdf285e223a7fa369d5ce98ec767dec949c6ca62863ce61757"
SRC_URI[bitvec-1.0.1.sha256sum] = "1bc2832c24239b0141d5674bb9174f9d68a8b5b3f2753311927c172ca46f7e9c"
SRC_URI[block-buffer-0.10.4.sha256sum] = "3078c7629b62d3f0439517fa394996acacc5cbc91c5a20d8c658e77abd503a71"
SRC_URI[block2-0.5.1.sha256sum] = "2c132eebf10f5cad5289222520a4a058514204aed6d791f1cf4fe8088b82d15f"
SRC_URI[block2-0.6.2.sha256sum] = "cdeb9d870516001442e364c5220d3574d2da8dc765554b4a617230d33fa58ef5"
SRC_URI[blocking-1.6.2.sha256sum] = "e83f8d02be6967315521be875afa792a316e28d57b5a2d401897e2a7921b7f21"
SRC_URI[borsh-1.6.0.sha256sum] = "d1da5ab77c1437701eeff7c88d968729e7766172279eab0676857b3d63af7a6f"
SRC_URI[built-0.8.0.sha256sum] = "f4ad8f11f288f48ca24471bbd51ac257aaeaaa07adae295591266b792902ae64"
SRC_URI[bumpalo-3.20.2.sha256sum] = "5d20789868f4b01b2f2caec9f5c4e0213b41e3e5702a50157d699ae31ced2fcb"
SRC_URI[by_address-1.2.1.sha256sum] = "64fa3c856b712db6612c019f14756e64e4bcea13337a6b33b696333a9eaa2d06"
SRC_URI[bytemuck-1.25.0.sha256sum] = "c8efb64bd706a16a1bdde310ae86b351e4d21550d98d056f22f8a7f7a2183fec"
SRC_URI[bytemuck_derive-1.10.2.sha256sum] = "f9abbd1bc6865053c427f7198e6af43bfdedc55ab791faed4fbd361d789575ff"
SRC_URI[byteorder-1.5.0.sha256sum] = "1fd0f2584146f6f2ef48085050886acf353beff7305ebd1ae69500e27c67f64b"
SRC_URI[byteorder-lite-0.1.0.sha256sum] = "8f1fe948ff07f4bd06c30984e69f5b4899c516a3ef74f34df92a2df2ab535495"
SRC_URI[bytes-1.11.1.sha256sum] = "1e748733b7cbc798e1434b6ac524f0c1ff2ab456fe201501e6497c8417a4fc33"
SRC_URI[calloop-0.13.0.sha256sum] = "b99da2f8558ca23c71f4fd15dc57c906239752dd27ff3c00a1d56b685b7cbfec"
SRC_URI[calloop-0.14.4.sha256sum] = "4dbf9978365bac10f54d1d4b04f7ce4427e51f71d61f2fe15e3fed5166474df7"
SRC_URI[calloop-wayland-source-0.3.0.sha256sum] = "95a66a987056935f7efce4ab5668920b5d0dac4a7c99991a67395f13702ddd20"
SRC_URI[calloop-wayland-source-0.4.1.sha256sum] = "138efcf0940a02ebf0cc8d1eff41a1682a46b431630f4c52450d6265876021fa"
SRC_URI[cc-1.2.57.sha256sum] = "7a0dd1ca384932ff3641c8718a02769f1698e7563dc6974ffd03346116310423"
SRC_URI[cesu8-1.1.0.sha256sum] = "6d43a04d8753f35258c91f8ec639f792891f748a1edbd759cf1dcea3382ad83c"
SRC_URI[cexpr-0.6.0.sha256sum] = "6fac387a98bb7c37292057cffc56d62ecb629900026402633ae9160df93a8766"
SRC_URI[cfg-if-1.0.4.sha256sum] = "9330f8b2ff13f34540b44e946ef35111825727b38d33286ef986142615121801"
SRC_URI[cfg_aliases-0.2.1.sha256sum] = "613afe47fcd5fac7ccf1db93babcb082c5994d996f20b8b159f2ad1658eb5724"
SRC_URI[cgl-0.3.2.sha256sum] = "0ced0551234e87afee12411d535648dd89d2e7f34c78b753395567aff3d447ff"
SRC_URI[chrono-0.4.44.sha256sum] = "c673075a2e0e5f4a1dde27ce9dee1ea4558c7ffe648f576438a20ca1d2acc4b0"
SRC_URI[clang-sys-1.8.1.sha256sum] = "0b023947811758c97c59bf9d1c188fd619ad4718dcaa767947df1cadb14f39f4"
SRC_URI[clap-4.6.0.sha256sum] = "b193af5b67834b676abd72466a96c1024e6a6ad978a1f484bd90b85c94041351"
SRC_URI[clap_builder-4.6.0.sha256sum] = "714a53001bf66416adb0e2ef5ac857140e7dc3a0c48fb28b2f10762fc4b5069f"
SRC_URI[clap_derive-4.6.0.sha256sum] = "1110bd8a634a1ab8cb04345d8d878267d57c3cf1b38d91b71af6686408bbca6a"
SRC_URI[clap_lex-1.1.0.sha256sum] = "c8d4a3bb8b1e0c1050499d1815f5ab16d04f0959b233085fb31653fbfc9d98f9"
SRC_URI[clipboard-win-5.4.1.sha256sum] = "bde03770d3df201d4fb868f2c9c59e66a3e4e2bd06692a0fe701e7103c7e84d4"
SRC_URI[clru-0.6.3.sha256sum] = "197fd99cb113a8d5d9b6376f3aa817f32c1078f2343b714fff7d2ca44fdf67d5"
SRC_URI[color_quant-1.1.0.sha256sum] = "3d7b894f5411737b7867f4827955924d7c254fc9f4d91a6aad6b097804b1018b"
SRC_URI[colorchoice-1.0.5.sha256sum] = "1d07550c9036bf2ae0c684c4297d503f838287c83c53686d05370d0e139ae570"
SRC_URI[combine-4.6.7.sha256sum] = "ba5a308b75df32fe02788e748662718f03fde005016435c444eea572398219fd"
SRC_URI[concurrent-queue-2.5.0.sha256sum] = "4ca0197aee26d1ae37445ee532fefce43251d24cc7c166799f4d46817f1d3973"
SRC_URI[const-field-offset-0.1.5.sha256sum] = "91fcde4ca1211b5a94b573083c472ee19e86b19a441913f66e1cc5c41daf0255"
SRC_URI[const-field-offset-macro-0.1.5.sha256sum] = "5387f5bbc9e9e6c96436ea125afa12614cebf8ac67f49abc08c1e7a891466c90"
SRC_URI[convert_case-0.10.0.sha256sum] = "633458d4ef8c78b72454de2d54fd6ab2e60f9e02be22f3c6104cdc8a4e0fceb9"
SRC_URI[copypasta-0.10.2.sha256sum] = "3e6811e17f81fe246ef2bc553f76b6ee6ab41a694845df1d37e52a92b7bbd38a"
SRC_URI[core-foundation-0.10.1.sha256sum] = "b2a6cd9ae233e7f62ba4e9353e81a88df7fc8a5987b8d445b4d90c879bd156f6"
SRC_URI[core-foundation-0.9.4.sha256sum] = "91e195e091a93c46f7102ec7818a2aa394e1e1771c3ab4825963fa03e45afb8f"
SRC_URI[core-foundation-sys-0.8.7.sha256sum] = "773648b94d0e5d620f64f280777445740e61fe701025087ec8b57f45c791888b"
SRC_URI[core-graphics-0.23.2.sha256sum] = "c07782be35f9e1140080c6b96f0d44b739e2278479f64e02fdab4e32dfd8b081"
SRC_URI[core-graphics-types-0.1.3.sha256sum] = "45390e6114f68f718cc7a830514a96f903cccd70d02a8f6d9f643ac4ba45afaf"
SRC_URI[core2-0.4.0.sha256sum] = "b49ba7ef1ad6107f8824dbe97de947cbaac53c44e7f9756a1fba0d37c1eec505"
SRC_URI[core_maths-0.1.1.sha256sum] = "77745e017f5edba1a9c1d854f6f3a52dac8a12dd5af5d2f54aecf61e43d80d30"
SRC_URI[countme-3.0.1.sha256sum] = "7704b5fdd17b18ae31c4c1da5a2e0305a2bf17b5249300a9ee9ed7b72114c636"
SRC_URI[cpp-0.5.10.sha256sum] = "f36bcac3d8234c1fb813358e83d1bb6b0290a3d2b3b5efc6b88bfeaf9d8eec17"
SRC_URI[cpp_build-0.5.10.sha256sum] = "27f8638c97fbd79cc6fc80b616e0e74b49bac21014faed590bbc89b7e2676c90"
SRC_URI[cpp_common-0.5.10.sha256sum] = "25fcfea2ee05889597d35e986c2ad0169694320ae5cc8f6d2640a4bb8a884560"
SRC_URI[cpp_macros-0.5.10.sha256sum] = "d156158fe86e274820f5a53bc9edb0885a6e7113909497aa8d883b69dd171871"
SRC_URI[cpufeatures-0.2.17.sha256sum] = "59ed5838eebb26a2bb2e58f6d5b5316989ae9d08bab10e0e6d103e656d1b0280"
SRC_URI[crc32fast-1.5.0.sha256sum] = "9481c1c90cbf2ac953f07c8d4a58aa3945c425b7185c9154d67a65e4230da511"
SRC_URI[critical-section-1.2.0.sha256sum] = "790eea4361631c5e7d22598ecd5723ff611904e3344ce8720784c93e3d83d40b"
SRC_URI[crossbeam-channel-0.5.15.sha256sum] = "82b8f8f868b36967f9606790d1903570de9ceaf870a7bf9fbbd3016d636a2cb2"
SRC_URI[crossbeam-deque-0.8.6.sha256sum] = "9dd111b7b7f7d55b72c0a6ae361660ee5853c9af73f70c3c2ef6858b950e2e51"
SRC_URI[crossbeam-epoch-0.9.18.sha256sum] = "5b82ac4a3c2ca9c3460964f020e1402edd5753411d7737aa39c3714ad1b5420e"
SRC_URI[crossbeam-utils-0.8.21.sha256sum] = "d0a5c400df2834b80a4c3327b3aad3a4c4cd4de0629063962b03235697506a28"
SRC_URI[crunchy-0.2.4.sha256sum] = "460fbee9c2c2f33933d720630a6a0bac33ba7053db5344fac858d4b8952d77d5"
SRC_URI[crypto-common-0.1.7.sha256sum] = "78c8292055d1c1df0cce5d180393dc8cce0abec0a7102adb6c7b1eef6016d60a"
SRC_URI[ctor-lite-0.1.2.sha256sum] = "e162d0c2e2068eb736b71e5597eff0b9944e6b973cd9f37b6a288ab9bf20e300"
SRC_URI[cursor-icon-1.2.0.sha256sum] = "f27ae1dd37df86211c42e150270f82743308803d90a6f6e6651cd730d5e1732f"
SRC_URI[data-url-0.3.2.sha256sum] = "be1e0bca6c3637f992fc1cc7cbc52a78c1ef6db076dbf1059c4323d6a2048376"
SRC_URI[derive_more-2.1.1.sha256sum] = "d751e9e49156b02b44f9c1815bcb94b984cdcc4396ecc32521c739452808b134"
SRC_URI[derive_more-impl-2.1.1.sha256sum] = "799a97264921d8623a957f6c3b9011f3b5492f557bbb7a5a19b7fa6d06ba8dcb"
SRC_URI[derive_utils-0.15.1.sha256sum] = "362f47930db19fe7735f527e6595e4900316b893ebf6d48ad3d31be928d57dd6"
SRC_URI[digest-0.10.7.sha256sum] = "9ed9a281f7bc9b7576e61468ba615a66a5c8cfdff42420a70aa82701a3b1e292"
SRC_URI[dispatch-0.2.0.sha256sum] = "bd0c93bb4b0c6d9b77f4435b0ae98c24d17f1c45b2ff844c6151a07256ca923b"
SRC_URI[dispatch2-0.3.1.sha256sum] = "1e0e367e4e7da84520dedcac1901e4da967309406d1e51017ae1abfb97adbd38"
SRC_URI[displaydoc-0.2.5.sha256sum] = "97369cbbc041bc366949bc74d34658d6cda5621039731c6310521892a3a20ae0"
SRC_URI[dlib-0.5.3.sha256sum] = "ab8ecd87370524b461f8557c119c405552c396ed91fc0a8eec68679eab26f94a"
SRC_URI[downcast-rs-1.2.1.sha256sum] = "75b325c5dbd37f80359721ad39aca5a29fb04c89279657cffdda8736d0c0b9d2"
SRC_URI[dpi-0.1.2.sha256sum] = "d8b14ccef22fc6f5a8f4d7d768562a182c04ce9a3b3157b91390b52ddfdf1a76"
SRC_URI[drm-0.14.2.sha256sum] = "c5b71449a23fe79542d6527ca572844b2016abf9573c49e43144d546b1735aec"
SRC_URI[drm-ffi-0.9.1.sha256sum] = "51a91c9b32ac4e8105dec255e849e0d66e27d7c34d184364fb93e469db08f690"
SRC_URI[drm-fourcc-2.2.0.sha256sum] = "0aafbcdb8afc29c1a7ee5fbe53b5d62f4565b35a042a662ca9fecd0b54dae6f4"
SRC_URI[drm-sys-0.8.1.sha256sum] = "ecc8e1361066d91f5ffccff060a3c3be9c3ecde15be2959c1937595f7a82a9f8"
SRC_URI[either-1.15.0.sha256sum] = "48c757948c5ede0e46177b7add2e67155f70e33c07fea8284df6576da70b3719"
SRC_URI[endi-1.1.1.sha256sum] = "66b7e2430c6dff6a955451e2cfc438f09cea1965a9d6f87f7e3b90decc014099"
SRC_URI[enumflags2-0.7.12.sha256sum] = "1027f7680c853e056ebcec683615fb6fbbc07dbaa13b4d5d9442b146ded4ecef"
SRC_URI[enumflags2_derive-0.7.12.sha256sum] = "67c78a4d8fdf9953a5c9d458f9efe940fd97a0cab0941c075a813ac594733827"
SRC_URI[equator-0.4.2.sha256sum] = "4711b213838dfee0117e3be6ac926007d7f433d7bbe33595975d4190cb07e6fc"
SRC_URI[equator-macro-0.4.2.sha256sum] = "44f23cf4b44bfce11a86ace86f8a73ffdec849c9fd00a386a53d278bd9e81fb3"
SRC_URI[equivalent-1.0.2.sha256sum] = "877a4ace8713b0bcf2a4e7eec82529c029f1d0619886d18145fea96c3ffe5c0f"
SRC_URI[errno-0.3.14.sha256sum] = "39cab71617ae0d63f51a36d69f866391735b51691dbda63cf6f96d042b63efeb"
SRC_URI[error-code-3.3.2.sha256sum] = "dea2df4cf52843e0452895c455a1a2cfbb842a1e7329671acf418fdc53ed4c59"
SRC_URI[euclid-0.22.13.sha256sum] = "df61bf483e837f88d5c2291dcf55c67be7e676b3a51acc48db3a7b163b91ed63"
SRC_URI[evdev-0.13.2.sha256sum] = "25b686663ba7f08d92880ff6ba22170f1df4e83629341cba34cf82cd65ebea99"
SRC_URI[event-listener-5.4.1.sha256sum] = "e13b66accf52311f30a0db42147dadea9850cb48cd070028831ae5f5d4b856ab"
SRC_URI[event-listener-strategy-0.5.4.sha256sum] = "8be9f3dfaaffdae2972880079a491a1a8bb7cbed0b8dd7a347f668b4150a3b93"
SRC_URI[exr-1.74.0.sha256sum] = "4300e043a56aa2cb633c01af81ca8f699a321879a7854d3896a0ba89056363be"
SRC_URI[fastrand-2.3.0.sha256sum] = "37909eebbb50d72f9059c3b6d82c0463f2ff062c9e95845c43a6c9c0355411be"
SRC_URI[fastwebsockets-0.10.0.sha256sum] = "305d3ba574508e27190906d11707dad683e0494e6b85eae9b044cb2734a5e422"
SRC_URI[fax-0.2.6.sha256sum] = "f05de7d48f37cd6730705cbca900770cab77a89f413d23e100ad7fad7795a0ab"
SRC_URI[fax_derive-0.2.0.sha256sum] = "a0aca10fb742cb43f9e7bb8467c91aa9bcb8e3ffbc6a6f7389bb93ffc920577d"
SRC_URI[fdeflate-0.3.7.sha256sum] = "1e6853b52649d4ac5c0bd02320cddc5ba956bdb407c4b75a2c6b75bf51500f8c"
SRC_URI[femtovg-0.20.4.sha256sum] = "35695993a8264f5dfa8facc135c003965cea40d83705b49e0f8c3a3b632a2171"
SRC_URI[field-offset-0.3.6.sha256sum] = "38e2275cc4e4fc009b0669731a1e5ab7ebf11f469eaede2bab9309a5b4d6057f"
SRC_URI[filetime-0.2.27.sha256sum] = "f98844151eee8917efc50bd9e8318cb963ae8b297431495d3f758616ea5c57db"
SRC_URI[find-msvc-tools-0.1.9.sha256sum] = "5baebc0774151f905a1a2cc41989300b1e6fbb29aff0ceffa1064fdd3088d582"
SRC_URI[flate2-1.1.9.sha256sum] = "843fba2746e448b37e26a819579957415c8cef339bf08564fe8b7ddbd959573c"
SRC_URI[float-cmp-0.9.0.sha256sum] = "98de4bbd547a563b716d8dfa9aad1cb19bfab00f4fa09a6a4ed21dbcf44ce9c4"
SRC_URI[fnv-1.0.7.sha256sum] = "3f9eec918d3f24069decb9af1554cad7c880e2da24a9afd88aca000531ab82c1"
SRC_URI[foldhash-0.1.5.sha256sum] = "d9c4f5dac5e15c24eb999c26181a6ca40b39fe946cbe4c263c7209467bc83af2"
SRC_URI[foldhash-0.2.0.sha256sum] = "77ce24cb58228fbb8aa041425bb1050850ac19177686ea6e0f41a70416f56fdb"
SRC_URI[font-types-0.10.1.sha256sum] = "39a654f404bbcbd48ea58c617c2993ee91d1cb63727a37bf2323a4edeed1b8c5"
SRC_URI[fontdb-0.23.0.sha256sum] = "457e789b3d1202543297a350643cf459f836cade38934e7a4cf6a39e7cde2905"
SRC_URI[fontdue-0.9.3.sha256sum] = "2e57e16b3fe8ff4364c0661fdaac543fb38b29ea9bc9c2f45612d90adf931d2b"
SRC_URI[fontique-0.7.0.sha256sum] = "30bbc252c93499b6d3635d692f892a637db0dbb130ce9b32bf20b28e0dcc470b"
SRC_URI[foreign-types-0.3.2.sha256sum] = "f6f339eb8adc052cd2ca78910fda869aefa38d22d5cb648e6485e4d3fc06f3b1"
SRC_URI[foreign-types-0.5.0.sha256sum] = "d737d9aa519fb7b749cbc3b962edcf310a8dd1f4b67c91c4f83975dbdd17d965"
SRC_URI[foreign-types-macros-0.2.3.sha256sum] = "1a5c6c585bc94aaf2c7b51dd4c2ba22680844aba4c687be581871a6f518c5742"
SRC_URI[foreign-types-shared-0.1.1.sha256sum] = "00b0228411908ca8685dba7fc2cdd70ec9990a6e753e89b6ac91a84c40fbaf4b"
SRC_URI[foreign-types-shared-0.3.1.sha256sum] = "aa9a19cbb55df58761df49b23516a86d432839add4af60fc256da840f66ed35b"
SRC_URI[form_urlencoded-1.2.2.sha256sum] = "cb4cb245038516f5f85277875cdaa4f7d2c9a0fa0468de06ed190163b1581fcf"
SRC_URI[funty-2.0.0.sha256sum] = "e6d5a32815ae3f33302d95fdcb2ce17862f8c65363dcfd29360480ba1001fc9c"
SRC_URI[futures-0.3.32.sha256sum] = "8b147ee9d1f6d097cef9ce628cd2ee62288d963e16fb287bd9286455b241382d"
SRC_URI[futures-channel-0.3.32.sha256sum] = "07bbe89c50d7a535e539b8c17bc0b49bdb77747034daa8087407d655f3f7cc1d"
SRC_URI[futures-core-0.3.32.sha256sum] = "7e3450815272ef58cec6d564423f6e755e25379b217b0bc688e295ba24df6b1d"
SRC_URI[futures-executor-0.3.32.sha256sum] = "baf29c38818342a3b26b5b923639e7b1f4a61fc5e76102d4b1981c6dc7a7579d"
SRC_URI[futures-io-0.3.32.sha256sum] = "cecba35d7ad927e23624b22ad55235f2239cfa44fd10428eecbeba6d6a717718"
SRC_URI[futures-lite-2.6.1.sha256sum] = "f78e10609fe0e0b3f4157ffab1876319b5b0db102a2c60dc4626306dc46b44ad"
SRC_URI[futures-macro-0.3.32.sha256sum] = "e835b70203e41293343137df5c0664546da5745f82ec9b84d40be8336958447b"
SRC_URI[futures-sink-0.3.32.sha256sum] = "c39754e157331b013978ec91992bde1ac089843443c49cbc7f46150b0fad0893"
SRC_URI[futures-task-0.3.32.sha256sum] = "037711b3d59c33004d3856fbdc83b99d4ff37a24768fa1be9ce3538a1cde4393"
SRC_URI[futures-util-0.3.32.sha256sum] = "389ca41296e6190b48053de0321d02a77f32f8a5d2461dd38762c0593805c6d6"
SRC_URI[gbm-0.18.0.sha256sum] = "ce852e998d3ca5e4a97014fb31c940dc5ef344ec7d364984525fd11e8a547e6a"
SRC_URI[gbm-sys-0.4.0.sha256sum] = "c13a5f2acc785d8fb6bf6b7ab6bfb0ef5dad4f4d97e8e70bb8e470722312f76f"
SRC_URI[generic-array-0.14.7.sha256sum] = "85649ca51fd72272d7821adaf274ad91c288277713d9c18820d8499a7ff69e9a"
SRC_URI[gethostname-1.1.0.sha256sum] = "1bd49230192a3797a9a4d6abe9b3eed6f7fa4c8a8a4947977c6f80025f92cbd8"
SRC_URI[getopts-0.2.24.sha256sum] = "cfe4fbac503b8d1f88e6676011885f34b7174f46e59956bba534ba83abded4df"
SRC_URI[getrandom-0.2.17.sha256sum] = "ff2abc00be7fca6ebc474524697ae276ad847ad0a6b3faa4bcb027e9a4614ad0"
SRC_URI[getrandom-0.3.4.sha256sum] = "899def5c37c4fd7b2664648c28120ecec138e4d395b459e5ca34f9cce2dd77fd"
SRC_URI[getrandom-0.4.2.sha256sum] = "0de51e6874e94e7bf76d726fc5d13ba782deca734ff60d5bb2fb2607c7406555"
SRC_URI[gif-0.14.1.sha256sum] = "f5df2ba84018d80c213569363bdcd0c64e6933c67fe4c1d60ecf822971a3c35e"
SRC_URI[gl_generator-0.14.0.sha256sum] = "1a95dfc23a2b4a9a2f5ab41d194f8bfda3cabec42af4e39f08c339eb2a0c124d"
SRC_URI[glob-0.3.3.sha256sum] = "0cc23270f6e1808e30a928bdc84dea0b9b4136a8bc82338574f23baf47bbd280"
SRC_URI[glow-0.16.0.sha256sum] = "c5e5ea60d70410161c8bf5da3fdfeaa1c72ed2c15f8bbb9d19fe3a4fad085f08"
SRC_URI[glutin-0.32.3.sha256sum] = "12124de845cacfebedff80e877bb37b5b75c34c5a4c89e47e1cdd67fb6041325"
SRC_URI[glutin-winit-0.5.0.sha256sum] = "85edca7075f8fc728f28cb8fbb111a96c3b89e930574369e3e9c27eb75d3788f"
SRC_URI[glutin_egl_sys-0.7.1.sha256sum] = "4c4680ba6195f424febdc3ba46e7a42a0e58743f2edb115297b86d7f8ecc02d2"
SRC_URI[glutin_glx_sys-0.6.1.sha256sum] = "8a7bb2938045a88b612499fbcba375a77198e01306f52272e692f8c1f3751185"
SRC_URI[glutin_wgl_sys-0.6.1.sha256sum] = "2c4ee00b289aba7a9e5306d57c2d05499b2e5dc427f84ac708bd2c090212cf3e"
SRC_URI[half-2.7.1.sha256sum] = "6ea2d84b969582b4b1864a92dc5d27cd2b77b622a8d79306834f1be5ba20d84b"
SRC_URI[harfrust-0.3.2.sha256sum] = "92c020db12c71d8a12a3fe7607873cade3a01a6287e29d540c8723276221b9d8"
SRC_URI[hashbrown-0.14.5.sha256sum] = "e5274423e17b7c9fc20b6e7e208532f9b19825d82dfd615708b70edd83df41f1"
SRC_URI[hashbrown-0.15.5.sha256sum] = "9229cfe53dfd69f0609a49f65461bd93001ea1ef889cd5529dd176593f5338a1"
SRC_URI[hashbrown-0.16.1.sha256sum] = "841d1cc9bed7f9236f321df977030373f4a4163ae1a7dbfe1a51a2c1a51d9100"
SRC_URI[heck-0.5.0.sha256sum] = "2304e00983f87ffb38b55b444b5e3b60a884b5d30c0fca7d82fe33449bbe55ea"
SRC_URI[hermit-abi-0.3.9.sha256sum] = "d231dfb89cfffdbc30e7fc41579ed6066ad03abda9e567ccafae602b97ec5024"
SRC_URI[hermit-abi-0.5.2.sha256sum] = "fc0fef456e4baa96da950455cd02c081ca953b141298e41db3fc7e36b1da849c"
SRC_URI[hex-0.4.3.sha256sum] = "7f24254aa9a54b5c858eaee2f5bccdb46aaf0e486a595ed5fd8f86ba55232a70"
SRC_URI[home-0.5.12.sha256sum] = "cc627f471c528ff0c4a49e1d5e60450c8f6461dd6d10ba9dcd3a61d3dff7728d"
SRC_URI[htmlparser-0.2.1.sha256sum] = "48ce8546b993eaf241d69ded33b1be6d205dd9857ec879d9d18bd05d3676e144"
SRC_URI[http-1.4.0.sha256sum] = "e3ba2a386d7f85a81f119ad7498ebe444d2e22c2af0b86b069416ace48b3311a"
SRC_URI[http-body-1.0.1.sha256sum] = "1efedce1fb8e6913f23e0c92de8e62cd5b772a67e7b3946df930a62566c93184"
SRC_URI[http-body-util-0.1.3.sha256sum] = "b021d93e26becf5dc7e1b75b1bed1fd93124b374ceb73f43d4d4eafec896a64a"
SRC_URI[httparse-1.10.1.sha256sum] = "6dbf3de79e51f3d586ab4cb9d5c3e2c14aa28ed23d180cf89b4df0454a69cc87"
SRC_URI[httpdate-1.0.3.sha256sum] = "df3b46402a9d5adb4c86a0cf463f42e19994e3ee891101b1841f30a545cb49a9"
SRC_URI[hyper-1.8.1.sha256sum] = "2ab2d4f250c3d7b1c9fcdff1cece94ea4e2dfbec68614f7b87cb205f24ca9d11"
SRC_URI[hyper-tls-0.6.0.sha256sum] = "70206fc6890eaca9fde8a0bf71caa2ddfc9fe045ac9e5c70df101a7dbde866e0"
SRC_URI[hyper-util-0.1.20.sha256sum] = "96547c2556ec9d12fb1578c4eaf448b04993e7fb79cbaad930a656880a6bdfa0"
SRC_URI[i-slint-backend-linuxkms-1.15.1.sha256sum] = "6b8827952ecfbbf76c8cb5bc3388ca9124c34f2b4fe5dffcfe57800d2a484885"
SRC_URI[i-slint-backend-qt-1.15.1.sha256sum] = "ac9d5db3221f453439ec5ad9b6ac3bb8d2b4825b2f8734f0cde4b67d7336c3da"
SRC_URI[i-slint-backend-selector-1.15.1.sha256sum] = "ce5a7e591a7257096e1f3da1bbb9ad6a140c307d0eee74f008a0b412fdb20dec"
SRC_URI[i-slint-backend-winit-1.15.1.sha256sum] = "bbbf4789191740f939c9563b8850379122d7b5c1ceb09f9297b50ad53e408787"
SRC_URI[i-slint-common-1.15.1.sha256sum] = "e7659797fd28d4df3ed275ff95bf730bdf4a88d253f07e1ee8d0032d70138c3a"
SRC_URI[i-slint-compiler-1.15.1.sha256sum] = "2a6f358d0d5389869d67cd6ab6f5acf98fe31827264a696593e9687213cff682"
SRC_URI[i-slint-core-1.15.1.sha256sum] = "a95591ff85f8e2ff11c8d26ea8429768c2b77866e0c7e7fd49348f23ad108b5c"
SRC_URI[i-slint-core-macros-1.15.1.sha256sum] = "7cc5f2f71682787dd5c6299555c0de635009eb269bbc54d6198e0d225b69fae4"
SRC_URI[i-slint-renderer-femtovg-1.15.1.sha256sum] = "eb6eccda447999bc6222988500b841b64c953988986af182334e7ba9a30f0edd"
SRC_URI[i-slint-renderer-skia-1.15.1.sha256sum] = "a64546232c0370f291e65fc92a4f4fc777ea78d5f48467873cb968b1de52e9ab"
SRC_URI[i-slint-renderer-software-1.15.1.sha256sum] = "a59be6c34935c4f8e41aa67a63518d5c59219c8eeb1d07af420bed8334fa31d7"
SRC_URI[iana-time-zone-0.1.65.sha256sum] = "e31bc9ad994ba00e440a8aa5c9ef0ec67d5cb5e5cb0cc7f8b744a35b389cc470"
SRC_URI[iana-time-zone-haiku-0.1.2.sha256sum] = "f31827a206f56af32e590ba56d5d2d085f558508192593743f16b2306495269f"
SRC_URI[icu_collections-2.1.1.sha256sum] = "4c6b649701667bbe825c3b7e6388cb521c23d88644678e83c0c4d0a621a34b43"
SRC_URI[icu_locale_core-2.1.1.sha256sum] = "edba7861004dd3714265b4db54a3c390e880ab658fec5f7db895fae2046b5bb6"
SRC_URI[icu_normalizer-2.1.1.sha256sum] = "5f6c8828b67bf8908d82127b2054ea1b4427ff0230ee9141c54251934ab1b599"
SRC_URI[icu_normalizer_data-2.1.1.sha256sum] = "7aedcccd01fc5fe81e6b489c15b247b8b0690feb23304303a9e560f37efc560a"
SRC_URI[icu_properties-2.1.2.sha256sum] = "020bfc02fe870ec3a66d93e677ccca0562506e5872c650f893269e08615d74ec"
SRC_URI[icu_properties_data-2.1.2.sha256sum] = "616c294cf8d725c6afcd8f55abc17c56464ef6211f9ed59cccffe534129c77af"
SRC_URI[icu_provider-2.1.1.sha256sum] = "85962cf0ce02e1e0a629cc34e7ca3e373ce20dda4c4d7294bbd0bf1fdb59e614"
SRC_URI[id-arena-2.3.0.sha256sum] = "3d3067d79b975e8844ca9eb072e16b31c3c1c36928edf9c6789548c524d0d954"
SRC_URI[idna-1.1.0.sha256sum] = "3b0875f23caa03898994f6ddc501886a45c7d3d62d04d2d90788d47be1b1e4de"
SRC_URI[idna_adapter-1.2.1.sha256sum] = "3acae9609540aa318d1bc588455225fb2085b9ed0c4f6bd0d9d5bcd86f1a0344"
SRC_URI[image-0.25.10.sha256sum] = "85ab80394333c02fe689eaf900ab500fbd0c2213da414687ebf995a65d5a6104"
SRC_URI[image-webp-0.2.4.sha256sum] = "525e9ff3e1a4be2fbea1fdf0e98686a6d98b4d8f937e1bf7402245af1909e8c3"
SRC_URI[imagesize-0.14.0.sha256sum] = "09e54e57b4c48b40f7aec75635392b12b3421fa26fe8b4332e63138ed278459c"
SRC_URI[imgref-1.12.0.sha256sum] = "e7c5cedc30da3a610cac6b4ba17597bdf7152cf974e8aab3afb3d54455e371c8"
SRC_URI[indexmap-2.13.0.sha256sum] = "7714e70437a7dc3ac8eb7e6f8df75fd8eb422675fc7678aff7364301092b1017"
SRC_URI[input-0.9.1.sha256sum] = "fbdc09524a91f9cacd26f16734ff63d7dc650daffadd2b6f84d17a285bd875a9"
SRC_URI[input-sys-1.18.0.sha256sum] = "bd4f5b4d1c00331c5245163aacfe5f20be75b564c7112d45893d4ae038119eb0"
SRC_URI[integer-sqrt-0.1.5.sha256sum] = "276ec31bcb4a9ee45f58bec6f9ec700ae4cf4f4f8f2fa7e06cb406bd5ffdd770"
SRC_URI[interpolate_name-0.2.4.sha256sum] = "c34819042dc3d3971c46c2190835914dfbe0c3c13f61449b2997f4e9722dfa60"
SRC_URI[io-lifetimes-1.0.11.sha256sum] = "eae7b9aee968036d54dce06cebaefd919e4472e753296daccd6d344e3e2df0c2"
SRC_URI[ipnet-2.12.0.sha256sum] = "d98f6fed1fde3f8c21bc40a1abb88dd75e67924f9cffc3ef95607bad8017f8e2"
SRC_URI[iri-string-0.7.10.sha256sum] = "c91338f0783edbd6195decb37bae672fd3b165faffb89bf7b9e6942f8b1a731a"
SRC_URI[is_terminal_polyfill-1.70.2.sha256sum] = "a6cb138bb79a146c1bd460005623e142ef0181e3d0219cb493e02f7d08a35695"
SRC_URI[itertools-0.13.0.sha256sum] = "413ee7dfc52ee1a4949ceeb7dbc8a33f2d6c088194d9f922fb8318faf1f01186"
SRC_URI[itertools-0.14.0.sha256sum] = "2b192c782037fadd9cfa75548310488aabdbf3d2da73885b31bd0abd03351285"
SRC_URI[itoa-1.0.17.sha256sum] = "92ecc6618181def0457392ccd0ee51198e065e016d1d527a7ac1b6dc7c1f09d2"
SRC_URI[jni-0.21.1.sha256sum] = "1a87aa2bb7d2af34197c04845522473242e1aa17c12f4935d5856491a7fb8c97"
SRC_URI[jni-sys-0.3.0.sha256sum] = "8eaf4bc02d17cbdd7ff4c7438cafcdf7fb9a4613313ad11b4f8fefe7d3fa0130"
SRC_URI[jobserver-0.1.34.sha256sum] = "9afb3de4395d6b3e67a780b6de64b51c978ecf11cb9a462c66be7d4ca9039d33"
SRC_URI[js-sys-0.3.91.sha256sum] = "b49715b7073f385ba4bc528e5747d02e66cb39c6146efb66b781f131f0fb399c"
SRC_URI[keyboard-types-0.7.0.sha256sum] = "b750dcadc39a09dbadd74e118f6dd6598df77fa01df0cfcdc52c28dece74528a"
SRC_URI[khronos_api-3.1.0.sha256sum] = "e2db585e1d738fc771bf08a151420d3ed193d9d895a36df7f6f8a9456b911ddc"
SRC_URI[kurbo-0.12.0.sha256sum] = "ce9729cc38c18d86123ab736fd2e7151763ba226ac2490ec092d1dd148825e32"
SRC_URI[kurbo-0.13.0.sha256sum] = "7564e90fe3c0d5771e1f0bc95322b21baaeaa0d9213fa6a0b61c99f8b17b3bfb"
SRC_URI[lazy_static-1.5.0.sha256sum] = "bbd2bcb4c963f2ddae06a2efc7e9f3591312473c50c6685e1f298068316e66fe"
SRC_URI[lazycell-1.3.0.sha256sum] = "830d08ce1d1d941e6b30645f1a0eb5643013d835ce3779a5fc208261dbe10f55"
SRC_URI[leb128fmt-0.1.0.sha256sum] = "09edd9e8b54e49e587e4f6295a7d29c3ea94d469cb40ab8ca70b288248a81db2"
SRC_URI[lebe-0.5.3.sha256sum] = "7a79a3332a6609480d7d0c9eab957bca6b455b91bb84e66d19f5ff66294b85b8"
SRC_URI[libc-0.2.183.sha256sum] = "b5b646652bf6661599e1da8901b3b9522896f01e736bad5f723fe7a3a27f899d"
SRC_URI[libfuzzer-sys-0.4.12.sha256sum] = "f12a681b7dd8ce12bff52488013ba614b869148d54dd79836ab85aafdd53f08d"
SRC_URI[libloading-0.8.9.sha256sum] = "d7c4b02199fee7c5d21a5ae7d8cfa79a6ef5bb2fc834d6e9058e89c825efdc55"
SRC_URI[libm-0.2.16.sha256sum] = "b6d2cec3eae94f9f509c767b45932f1ada8350c4bdb85af2fcab4a3c14807981"
SRC_URI[libredox-0.1.14.sha256sum] = "1744e39d1d6a9948f4f388969627434e31128196de472883b39f148769bfe30a"
SRC_URI[libudev-sys-0.1.4.sha256sum] = "3c8469b4a23b962c1396b9b451dda50ef5b283e8dd309d69033475fa9b334324"
SRC_URI[linebender_resource_handle-0.1.1.sha256sum] = "d4a5ff6bcca6c4867b1c4fd4ef63e4db7436ef363e0ad7531d1558856bae64f4"
SRC_URI[linked-hash-map-0.5.6.sha256sum] = "0717cef1bc8b636c6e1c1bbdefc09e6322da8a9321966e8928ef80d20f7f770f"
SRC_URI[linked_hash_set-0.1.6.sha256sum] = "984fb35d06508d1e69fc91050cceba9c0b748f983e6739fa2c7a9237154c52c8"
SRC_URI[linux-raw-sys-0.12.1.sha256sum] = "32a66949e030da00e8c7d4434b251670a91556f4144941d37452769c25d58a53"
SRC_URI[linux-raw-sys-0.4.15.sha256sum] = "d26c52dbd32dccf2d10cac7725f8eae5296885fb5703b261f7d0a0739ec807ab"
SRC_URI[linux-raw-sys-0.9.4.sha256sum] = "cd945864f07fe9f5371a27ad7b52a172b4b499999f1d97574c9fa68373937e12"
SRC_URI[linuxfb-0.3.1.sha256sum] = "7e51f313066dd86b36a3bf20ba93cfa57b5513da811745b296481923606d0bec"
SRC_URI[litemap-0.8.1.sha256sum] = "6373607a59f0be73a39b6fe456b8192fcc3585f602af20751600e974dd455e77"
SRC_URI[lock_api-0.4.14.sha256sum] = "224399e74b87b5f3557511d98dff8b14089b3dadafcab6bb93eab67d3aace965"
SRC_URI[log-0.4.29.sha256sum] = "5e5032e24019045c762d3c0f28f5b6b8bbf38563a65908389bf7978758920897"
SRC_URI[loop9-0.1.5.sha256sum] = "0fae87c125b03c1d2c0150c90365d7d6bcc53fb73a9acaef207d2d065860f062"
SRC_URI[lyon_algorithms-1.0.19.sha256sum] = "9815fac08e6fd96733a11dce4f9d15a3f338e96a2e2311ee21e1b738efc2bc0f"
SRC_URI[lyon_extra-1.1.0.sha256sum] = "7755f08423275157ad1680aaecc9ccb7e0cc633da3240fea2d1522935cc15c72"
SRC_URI[lyon_geom-1.0.19.sha256sum] = "4336502e29e32af93cf2dad2214ed6003c17ceb5bd499df77b1de663b9042b92"
SRC_URI[lyon_path-1.0.19.sha256sum] = "5c463f9c428b7fc5ec885dcd39ce4aa61e29111d0e33483f6f98c74e89d8621e"
SRC_URI[maybe-rayon-0.1.1.sha256sum] = "8ea1f30cedd69f0a2954655f7188c6a834246d2bcf1e315e2ac40c4b24dc9519"
SRC_URI[memchr-2.8.0.sha256sum] = "f8ca58f447f06ed17d5fc4043ce1b10dd205e060fb3ce5b979b8ed8e59ff3f79"
SRC_URI[memmap-0.7.0.sha256sum] = "6585fd95e7bb50d6cc31e20d4cf9afb4e2ba16c5846fc76793f11218da9c475b"
SRC_URI[memmap2-0.9.10.sha256sum] = "714098028fe011992e1c3962653c96b2d578c4b4bce9036e15ff220319b1e0e3"
SRC_URI[memoffset-0.9.1.sha256sum] = "488016bfae457b036d996092f6cb448677611ce4449e970ceaf42695203f218a"
SRC_URI[minimal-lexical-0.2.1.sha256sum] = "68354c5c6bd36d73ff3feceb05efa59b6acb7626617f4962be322a825e61f79a"
SRC_URI[miniz_oxide-0.8.9.sha256sum] = "1fa76a2c86f704bdb222d66965fb3d63269ce38518b83cb0575fca855ebb6316"
SRC_URI[mio-1.1.1.sha256sum] = "a69bcab0ad47271a0234d9422b131806bf3968021e5dc9328caf2d4cd58557fc"
SRC_URI[moxcms-0.8.1.sha256sum] = "bb85c154ba489f01b25c0d36ae69a87e4a1c73a72631fc6c0eb6dde34a73e44b"
SRC_URI[muda-0.17.1.sha256sum] = "01c1738382f66ed56b3b9c8119e794a2e23148ac8ea214eda86622d4cb9d415a"
SRC_URI[native-tls-0.2.18.sha256sum] = "465500e14ea162429d264d44189adc38b199b62b1c21eea9f69e4b73cb03bbf2"
SRC_URI[natord-1.0.9.sha256sum] = "308d96db8debc727c3fd9744aac51751243420e46edf401010908da7f8d5e57c"
SRC_URI[ndk-0.9.0.sha256sum] = "c3f42e7bbe13d351b6bead8286a43aac9534b82bd3cc43e47037f012ebfd62d4"
SRC_URI[ndk-context-0.1.1.sha256sum] = "27b02d87554356db9e9a873add8782d4ea6e3e58ea071a9adb9a2e8ddb884a8b"
SRC_URI[ndk-sys-0.6.0+11769913.sha256sum] = "ee6cda3051665f1fb8d9e08fc35c96d5a244fb1be711a03b71118828afc9a873"
SRC_URI[new_debug_unreachable-1.0.6.sha256sum] = "650eef8c711430f1a879fdd01d4745a7deea475becfb90269c06775983bbf086"
SRC_URI[nix-0.29.0.sha256sum] = "71e2746dc3a24dd78b3cfcb7be93368c6de9963d30f43a6a73998a9cf4b17b46"
SRC_URI[nix-0.30.1.sha256sum] = "74523f3a35e05aba87a1d978330aef40f67b0304ac79c1c00b294c9830543db6"
SRC_URI[nom-7.1.3.sha256sum] = "d273983c5a657a70a3e8f2a01329822f3b8c8172b73826411a55751e404a0a4a"
SRC_URI[nom-8.0.0.sha256sum] = "df9761775871bdef83bee530e60050f7e54b1105350d6884eb0fb4f46c2f9405"
SRC_URI[noop_proc_macro-0.3.0.sha256sum] = "0676bb32a98c1a483ce53e500a81ad9c3d5b3f7c920c28c24e9cb0980d0b5bc8"
SRC_URI[num-bigint-0.4.6.sha256sum] = "a5e44f723f1133c9deac646763579fdb3ac745e418f2a7af9cd0c431da1f20b9"
SRC_URI[num-derive-0.4.2.sha256sum] = "ed3955f1a9c7c0c15e092f9c887db08b1fc683305fdf6eb6684f22555355e202"
SRC_URI[num-integer-0.1.46.sha256sum] = "7969661fd2958a5cb096e56c8e1ad0444ac2bbcd0061bd28660485a44879858f"
SRC_URI[num-rational-0.4.2.sha256sum] = "f83d14da390562dca69fc84082e73e548e1ad308d24accdedd2720017cb37824"
SRC_URI[num-traits-0.2.19.sha256sum] = "071dfc062690e90b734c0b2273ce72ad0ffa95f0c74596bc250dcfd960262841"
SRC_URI[num_enum-0.7.6.sha256sum] = "5d0bca838442ec211fa11de3a8b0e0e8f3a4522575b5c4c06ed722e005036f26"
SRC_URI[num_enum_derive-0.7.6.sha256sum] = "680998035259dcfcafe653688bf2aa6d3e2dc05e98be6ab46afb089dc84f1df8"
SRC_URI[objc-sys-0.3.5.sha256sum] = "cdb91bdd390c7ce1a8607f35f3ca7151b65afc0ff5ff3b34fa350f7d7c7e4310"
SRC_URI[objc2-0.5.2.sha256sum] = "46a785d4eeff09c14c487497c162e92766fbb3e4059a71840cecc03d9a50b804"
SRC_URI[objc2-0.6.4.sha256sum] = "3a12a8ed07aefc768292f076dc3ac8c48f3781c8f2d5851dd3d98950e8c5a89f"
SRC_URI[objc2-app-kit-0.2.2.sha256sum] = "e4e89ad9e3d7d297152b17d39ed92cd50ca8063a89a9fa569046d41568891eff"
SRC_URI[objc2-app-kit-0.3.2.sha256sum] = "d49e936b501e5c5bf01fda3a9452ff86dc3ea98ad5f283e1455153142d97518c"
SRC_URI[objc2-cloud-kit-0.2.2.sha256sum] = "74dd3b56391c7a0596a295029734d3c1c5e7e510a4cb30245f8221ccea96b009"
SRC_URI[objc2-cloud-kit-0.3.2.sha256sum] = "73ad74d880bb43877038da939b7427bba67e9dd42004a18b809ba7d87cee241c"
SRC_URI[objc2-contacts-0.2.2.sha256sum] = "a5ff520e9c33812fd374d8deecef01d4a840e7b41862d849513de77e44aa4889"
SRC_URI[objc2-core-data-0.2.2.sha256sum] = "617fbf49e071c178c0b24c080767db52958f716d9eabdf0890523aeae54773ef"
SRC_URI[objc2-core-data-0.3.2.sha256sum] = "0b402a653efbb5e82ce4df10683b6b28027616a2715e90009947d50b8dd298fa"
SRC_URI[objc2-core-foundation-0.3.2.sha256sum] = "2a180dd8642fa45cdb7dd721cd4c11b1cadd4929ce112ebd8b9f5803cc79d536"
SRC_URI[objc2-core-graphics-0.3.2.sha256sum] = "e022c9d066895efa1345f8e33e584b9f958da2fd4cd116792e15e07e4720a807"
SRC_URI[objc2-core-image-0.2.2.sha256sum] = "55260963a527c99f1819c4f8e3b47fe04f9650694ef348ffd2227e8196d34c80"
SRC_URI[objc2-core-image-0.3.2.sha256sum] = "e5d563b38d2b97209f8e861173de434bd0214cf020e3423a52624cd1d989f006"
SRC_URI[objc2-core-location-0.2.2.sha256sum] = "000cfee34e683244f284252ee206a27953279d370e309649dc3ee317b37e5781"
SRC_URI[objc2-core-text-0.3.2.sha256sum] = "0cde0dfb48d25d2b4862161a4d5fcc0e3c24367869ad306b0c9ec0073bfed92d"
SRC_URI[objc2-core-video-0.3.2.sha256sum] = "d425caf1df73233f29fd8a5c3e5edbc30d2d4307870f802d18f00d83dc5141a6"
SRC_URI[objc2-encode-4.1.0.sha256sum] = "ef25abbcd74fb2609453eb695bd2f860d389e457f67dc17cafc8b8cbc89d0c33"
SRC_URI[objc2-foundation-0.2.2.sha256sum] = "0ee638a5da3799329310ad4cfa62fbf045d5f56e3ef5ba4149e7452dcf89d5a8"
SRC_URI[objc2-foundation-0.3.2.sha256sum] = "e3e0adef53c21f888deb4fa59fc59f7eb17404926ee8a6f59f5df0fd7f9f3272"
SRC_URI[objc2-io-surface-0.3.2.sha256sum] = "180788110936d59bab6bd83b6060ffdfffb3b922ba1396b312ae795e1de9d81d"
SRC_URI[objc2-link-presentation-0.2.2.sha256sum] = "a1a1ae721c5e35be65f01a03b6d2ac13a54cb4fa70d8a5da293d7b0020261398"
SRC_URI[objc2-metal-0.2.2.sha256sum] = "dd0cba1276f6023976a406a14ffa85e1fdd19df6b0f737b063b95f6c8c7aadd6"
SRC_URI[objc2-metal-0.3.2.sha256sum] = "a0125f776a10d00af4152d74616409f0d4a2053a6f57fa5b7d6aa2854ac04794"
SRC_URI[objc2-quartz-core-0.2.2.sha256sum] = "e42bee7bff906b14b167da2bac5efe6b6a07e6f7c0a21a7308d40c960242dc7a"
SRC_URI[objc2-quartz-core-0.3.2.sha256sum] = "96c1358452b371bf9f104e21ec536d37a650eb10f7ee379fff67d2e08d537f1f"
SRC_URI[objc2-symbols-0.2.2.sha256sum] = "0a684efe3dec1b305badae1a28f6555f6ddd3bb2c2267896782858d5a78404dc"
SRC_URI[objc2-ui-kit-0.2.2.sha256sum] = "b8bb46798b20cd6b91cbd113524c490f1686f4c4e8f49502431415f3512e2b6f"
SRC_URI[objc2-ui-kit-0.3.2.sha256sum] = "d87d638e33c06f577498cbcc50491496a3ed4246998a7fbba7ccb98b1e7eab22"
SRC_URI[objc2-uniform-type-identifiers-0.2.2.sha256sum] = "44fa5f9748dbfe1ca6c0b79ad20725a11eca7c2218bceb4b005cb1be26273bfe"
SRC_URI[objc2-user-notifications-0.2.2.sha256sum] = "76cfcbf642358e8689af64cee815d139339f3ed8ad05103ed5eaf73db8d84cb3"
SRC_URI[once_cell-1.21.4.sha256sum] = "9f7c3e4beb33f85d45ae3e3a1792185706c8e16d043238c593331cc7cd313b50"
SRC_URI[once_cell_polyfill-1.70.2.sha256sum] = "384b8ab6d37215f3c5301a95a4accb5d64aa607f1fcb26a11b5303878451b4fe"
SRC_URI[openssl-0.10.76.sha256sum] = "951c002c75e16ea2c65b8c7e4d3d51d5530d8dfa7d060b4776828c88cfb18ecf"
SRC_URI[openssl-macros-0.1.1.sha256sum] = "a948666b637a0f465e8564c73e89d4dde00d72d4d473cc972f390fc3dcee7d9c"
SRC_URI[openssl-probe-0.2.1.sha256sum] = "7c87def4c32ab89d880effc9e097653c8da5d6ef28e6b539d313baaacfbafcbe"
SRC_URI[openssl-src-300.5.5+3.5.5.sha256sum] = "3f1787d533e03597a7934fd0a765f0d28e94ecc5fb7789f8053b1e699a56f709"
SRC_URI[openssl-sys-0.9.112.sha256sum] = "57d55af3b3e226502be1526dfdba67ab0e9c96fc293004e79576b2b9edb0dbdb"
SRC_URI[optional_struct-0.5.2.sha256sum] = "14199f59efce6ed2c5854f0abc725c32eedfbd02c6ef82c9733c726f3fc6dc91"
SRC_URI[optional_struct_macro-0.5.2.sha256sum] = "e5eba042d9efe5e108e0df9ce2f85c540fc4f94f41c6821cbcf70ed47c1221da"
SRC_URI[orbclient-0.3.51.sha256sum] = "59aed3b33578edcfa1bc96a321d590d31832b6ad55a26f0313362ce687e9abd6"
SRC_URI[ordered-stream-0.2.0.sha256sum] = "9aa2b01e1d916879f73a53d01d1d6cee68adbb31d6d9177a8cfce093cced1d50"
SRC_URI[owned_ttf_parser-0.25.1.sha256sum] = "36820e9051aca1014ddc75770aab4d68bc1e9e632f0f5627c4086bc216fb583b"
SRC_URI[parking-2.2.1.sha256sum] = "f38d5652c16fde515bb1ecef450ab0f6a219d619a7274976324d5e377f7dceba"
SRC_URI[parking_lot-0.12.5.sha256sum] = "93857453250e3077bd71ff98b6a65ea6621a19bb0f559a85248955ac12c45a1a"
SRC_URI[parking_lot_core-0.9.12.sha256sum] = "2621685985a2ebf1c516881c026032ac7deafcda1a2c9b7850dc81e3dfcb64c1"
SRC_URI[parley-0.7.0.sha256sum] = "ada5338c3a9794af7342e6f765b6e78740db37378aced034d7bf72c96b94ed94"
SRC_URI[paste-1.0.15.sha256sum] = "57c0d7b74b563b49d38dae00a0c37d4d6de9b432382b2892f0574ddcae73fd0a"
SRC_URI[pastey-0.1.1.sha256sum] = "35fb2e5f958ec131621fdd531e9fc186ed768cbe395337403ae56c17a74c68ec"
SRC_URI[peeking_take_while-0.1.2.sha256sum] = "19b17cddbe7ec3f8bc800887bab5e717348c95ea2ca0b1bf0837fb964dc67099"
SRC_URI[percent-encoding-2.3.2.sha256sum] = "9b4f627cb1b25917193a259e49bdad08f671f8d9708acfd5fe0a8c1455d87220"
SRC_URI[pico-args-0.5.0.sha256sum] = "5be167a7af36ee22fe3115051bc51f6e6c7054c9348e28deb4f49bd6f705a315"
SRC_URI[pin-project-1.1.11.sha256sum] = "f1749c7ed4bcaf4c3d0a3efc28538844fb29bcdd7d2b67b2be7e20ba861ff517"
SRC_URI[pin-project-internal-1.1.11.sha256sum] = "d9b20ed30f105399776b9c883e68e536ef602a16ae6f596d2c473591d6ad64c6"
SRC_URI[pin-project-lite-0.2.17.sha256sum] = "a89322df9ebe1c1578d689c92318e070967d1042b512afbe49518723f4e6d5cd"
SRC_URI[pin-utils-0.1.0.sha256sum] = "8b870d8c151b6f2fb93e84a13146138f05d02ed11c7e7c54f8826aaaf7c9f184"
SRC_URI[pin-weak-1.1.0.sha256sum] = "b330c9d1b92dfe68442ca20b009c717d5f0b1e3cf4965e62f704c3c6e95a1305"
SRC_URI[piper-0.2.5.sha256sum] = "c835479a4443ded371d6c535cbfd8d31ad92c5d23ae9770a61bc155e4992a3c1"
SRC_URI[pkg-config-0.3.32.sha256sum] = "7edddbd0b52d732b21ad9a5fab5c704c14cd949e5e9a1ec5929a24fded1b904c"
SRC_URI[plain-0.2.3.sha256sum] = "b4596b6d070b27117e987119b4dac604f3c58cfb0b191112e24771b2faeac1a6"
SRC_URI[png-0.17.16.sha256sum] = "82151a2fc869e011c153adc57cf2789ccb8d9906ce52c0b39a6b5697749d7526"
SRC_URI[png-0.18.1.sha256sum] = "60769b8b31b2a9f263dae2776c37b1b28ae246943cf719eb6946a1db05128a61"
SRC_URI[polling-3.11.0.sha256sum] = "5d0e4f59085d47d8241c88ead0f274e8a0cb551f3625263c05eb8dd897c34218"
SRC_URI[portable-atomic-1.13.1.sha256sum] = "c33a9471896f1c69cecef8d20cbe2f7accd12527ce60845ff44c153bb2a21b49"
SRC_URI[potential_utf-0.1.4.sha256sum] = "b73949432f5e2a09657003c25bca5e19a0e9c84f8058ca374f49e0ebe605af77"
SRC_URI[ppv-lite86-0.2.21.sha256sum] = "85eae3c4ed2f50dcfe72643da4befc30deadb458a9b590d720cde2f2b1e97da9"
SRC_URI[prettyplease-0.2.37.sha256sum] = "479ca8adacdd7ce8f1fb39ce9ecccbfe93a3f1344b3d0d97f20bc0196208f62b"
SRC_URI[proc-macro-crate-3.5.0.sha256sum] = "e67ba7e9b2b56446f1d419b1d807906278ffa1a658a8a5d8a39dcb1f5a78614f"
SRC_URI[proc-macro2-1.0.106.sha256sum] = "8fd00f0bb2e90d81d1044c2b32617f68fcb9fa3bb7640c23e9c748e53fb30934"
SRC_URI[profiling-1.0.17.sha256sum] = "3eb8486b569e12e2c32ad3e204dbaba5e4b5b216e9367044f25f1dba42341773"
SRC_URI[profiling-procmacros-1.0.17.sha256sum] = "52717f9a02b6965224f95ca2a81e2e0c5c43baacd28ca057577988930b6c3d5b"
SRC_URI[pulldown-cmark-0.13.1.sha256sum] = "83c41efbf8f90ac44de7f3a868f0867851d261b56291732d0cbf7cceaaeb55a6"
SRC_URI[pulldown-cmark-escape-0.11.0.sha256sum] = "007d8adb5ddab6f8e3f491ac63566a7d5002cc7ed73901f72057943fa71ae1ae"
SRC_URI[pxfm-0.1.28.sha256sum] = "b5a041e753da8b807c9255f28de81879c78c876392ff2469cde94799b2896b9d"
SRC_URI[qoi-0.4.1.sha256sum] = "7f6d64c71eb498fe9eae14ce4ec935c555749aef511cca85b5568910d6e48001"
SRC_URI[qttypes-0.2.12.sha256sum] = "c7edf5b38c97ad8900ad2a8418ee44b4adceaa866a4a3405e2f1c909871d7ebd"
SRC_URI[quick-error-2.0.1.sha256sum] = "a993555f31e5a609f617c12db6250dedcac1b0a85076912c436e6fc9b2c8e6a3"
SRC_URI[quick-xml-0.38.4.sha256sum] = "b66c2058c55a409d601666cffe35f04333cf1013010882cec174a7467cd4e21c"
SRC_URI[quick-xml-0.39.2.sha256sum] = "958f21e8e7ceb5a1aa7fa87fab28e7c75976e0bfe7e23ff069e0a260f894067d"
SRC_URI[quote-1.0.45.sha256sum] = "41f2619966050689382d2b44f664f4bc593e129785a36d6ee376ddf37259b924"
SRC_URI[r-efi-5.3.0.sha256sum] = "69cdb34c158ceb288df11e18b4bd39de994f6657d83847bdffdbd7f346754b0f"
SRC_URI[r-efi-6.0.0.sha256sum] = "f8dcc9c7d52a811697d2151c701e0d08956f92b0e24136cf4cf27b57a6a0d9bf"
SRC_URI[radium-0.7.0.sha256sum] = "dc33ff2d4973d518d823d61aa239014831e521c75da58e3df4840d3f47749d09"
SRC_URI[rand-0.8.5.sha256sum] = "34af8d1a0e25924bc5b7c43c079c942339d8f0a8b57c39049bef581b46327404"
SRC_URI[rand-0.9.2.sha256sum] = "6db2770f06117d490610c7488547d543617b21bfa07796d7a12f6f1bd53850d1"
SRC_URI[rand_chacha-0.3.1.sha256sum] = "e6c10a63a0fa32252be49d21e7709d4d4baf8d231c2dbce1eaa8141b9b127d88"
SRC_URI[rand_chacha-0.9.0.sha256sum] = "d3022b5f1df60f26e1ffddd6c66e8aa15de382ae63b3a0c1bfc0e4d3e3f325cb"
SRC_URI[rand_core-0.6.4.sha256sum] = "ec0be4795e2f6a28069bec0b5ff3e2ac9bafc99e6a9a7dc3547996c5c816922c"
SRC_URI[rand_core-0.9.5.sha256sum] = "76afc826de14238e6e8c374ddcc1fa19e374fd8dd986b0d2af0d02377261d83c"
SRC_URI[rav1e-0.8.1.sha256sum] = "43b6dd56e85d9483277cde964fd1bdb0428de4fec5ebba7540995639a21cb32b"
SRC_URI[ravif-0.13.0.sha256sum] = "e52310197d971b0f5be7fe6b57530dcd27beb35c1b013f29d66c1ad73fbbcc45"
SRC_URI[raw-window-handle-0.6.2.sha256sum] = "20675572f6f24e9e76ef639bc5552774ed45f1c30e2951e1e99c59888861c539"
SRC_URI[raw-window-metal-1.1.0.sha256sum] = "40d213455a5f1dc59214213c7330e074ddf8114c9a42411eb890c767357ce135"
SRC_URI[rayon-1.11.0.sha256sum] = "368f01d005bf8fd9b1206fb6fa653e6c4a81ceb1466406b81792d87c5677a58f"
SRC_URI[rayon-core-1.13.0.sha256sum] = "22e18b0f0062d30d4230b2e85ff77fdfe4326feb054b9783a3460d8435c8ab91"
SRC_URI[read-fonts-0.35.0.sha256sum] = "6717cf23b488adf64b9d711329542ba34de147df262370221940dfabc2c91358"
SRC_URI[redox_syscall-0.4.1.sha256sum] = "4722d768eff46b75989dd134e5c353f0d6296e5aaa3132e776cbdb56be7731aa"
SRC_URI[redox_syscall-0.5.18.sha256sum] = "ed2bf2547551a7053d6fdfafda3f938979645c44812fbfcda098faae3f1a362d"
SRC_URI[redox_syscall-0.7.3.sha256sum] = "6ce70a74e890531977d37e532c34d45e9055d2409ed08ddba14529471ed0be16"
SRC_URI[regex-1.12.3.sha256sum] = "e10754a14b9137dd7b1e3e5b0493cc9171fdd105e0ab477f51b72e7f3ac0e276"
SRC_URI[regex-automata-0.4.14.sha256sum] = "6e1dd4122fc1595e8162618945476892eefca7b88c52820e74af6262213cae8f"
SRC_URI[regex-syntax-0.8.10.sha256sum] = "dc897dd8d9e8bd1ed8cdad82b5966c3e0ecae09fb1907d58efaa013543185d0a"
SRC_URI[reqwest-0.13.2.sha256sum] = "ab3f43e3283ab1488b624b44b0e988d0acea0b3214e694730a055cb6b2efa801"
SRC_URI[resvg-0.46.0.sha256sum] = "b563218631706d614e23059436526d005b50ab5f2d506b55a17eb65c5eb83419"
SRC_URI[rgb-0.8.53.sha256sum] = "47b34b781b31e5d73e9fbc8689c70551fd1ade9a19e3e28cfec8580a79290cc4"
SRC_URI[rowan-0.16.1.sha256sum] = "417a3a9f582e349834051b8a10c8d71ca88da4211e4093528e36b9845f6b5f21"
SRC_URI[roxmltree-0.21.1.sha256sum] = "f1964b10c76125c36f8afe190065a4bf9a87bf324842c05701330bba9f1cacbb"
SRC_URI[rspolib-0.1.2.sha256sum] = "4fda9a7796aff63a7b1b39ccc93fffaaf65e20042984b4843041a49ca4677535"
SRC_URI[rustc-hash-1.1.0.sha256sum] = "08d43f7aa6b08d49f382cde6a7982047c3426db949b1424bc4b7ec9ae12c6ce2"
SRC_URI[rustc-hash-2.1.1.sha256sum] = "357703d41365b4b27c590e3ed91eabb1b663f07c4c084095e60cbed4362dff0d"
SRC_URI[rustc_version-0.4.1.sha256sum] = "cfcb3a22ef46e85b45de6ee7e79d063319ebb6594faafcf1c225ea92ab6e9b92"
SRC_URI[rustix-0.38.44.sha256sum] = "fdb5bc1ae2baa591800df16c9ca78619bf65c0488b41b96ccec5d11220d8c154"
SRC_URI[rustix-1.1.4.sha256sum] = "b6fe4565b9518b83ef4f91bb47ce29620ca828bd32cb7e408f0062e9930ba190"
SRC_URI[rustls-pki-types-1.14.0.sha256sum] = "be040f8b0a225e40375822a563fa9524378b9d63112f53e19ffff34df5d33fdd"
SRC_URI[rustversion-1.0.22.sha256sum] = "b39cdef0fa800fc44525c84ccb54a029961a8215f9619753635a9c0d2538d46d"
SRC_URI[rustybuzz-0.20.1.sha256sum] = "fd3c7c96f8a08ee34eff8857b11b49b07d71d1c3f4e88f8a88d4c9e9f90b1702"
SRC_URI[same-file-1.0.6.sha256sum] = "93fc1dc3aaa9bfed95e02e6eadabb4baf7e3078b0bd1b4d7b6b0b68378900502"
SRC_URI[schannel-0.1.29.sha256sum] = "91c1b7e4904c873ef0710c1f407dde2e6287de2bebc1bbbf7d430bb7cbffd939"
SRC_URI[scoped-tls-1.0.1.sha256sum] = "e1cf6437eb19a8f4a6cc0f7dca544973b0b78843adbfeb3683d1a94a0024a294"
SRC_URI[scoped-tls-hkt-0.1.5.sha256sum] = "e9603871ffe5df3ac39cb624790c296dbd47a400d202f56bf3e414045099524d"
SRC_URI[scopeguard-1.2.0.sha256sum] = "94143f37725109f92c262ed2cf5e59bce7498c01bcc1502d7b9afe439a4e9f49"
SRC_URI[sctk-adwaita-0.10.1.sha256sum] = "b6277f0217056f77f1d8f49f2950ac6c278c0d607c45f5ee99328d792ede24ec"
SRC_URI[security-framework-3.7.0.sha256sum] = "b7f4bc775c73d9a02cde8bf7b2ec4c9d12743edf609006c7facc23998404cd1d"
SRC_URI[security-framework-sys-2.17.0.sha256sum] = "6ce2691df843ecc5d231c0b14ece2acc3efb62c0a398c7e1d875f3983ce020e3"
SRC_URI[semver-1.0.27.sha256sum] = "d767eb0aabc880b29956c35734170f26ed551a859dbd361d140cdbeca61ab1e2"
SRC_URI[serde-1.0.228.sha256sum] = "9a8e94ea7f378bd32cbbd37198a4a91436180c5bb472411e48b5ec2e2124ae9e"
SRC_URI[serde_core-1.0.228.sha256sum] = "41d385c7d4ca58e59fc732af25c3983b67ac852c1a25000afe1175de458b67ad"
SRC_URI[serde_derive-1.0.228.sha256sum] = "d540f220d3187173da220f885ab66608367b6574e925011a9353e4badda91d79"
SRC_URI[serde_json-1.0.149.sha256sum] = "83fc039473c5595ace860d8c4fafa220ff474b3fc6bfdb4293327f1a37e94d86"
SRC_URI[serde_repr-0.1.20.sha256sum] = "175ee3e80ae9982737ca543e96133087cbd9a485eecc3bc4de9c1a37b47ea59c"
SRC_URI[serde_spanned-1.0.4.sha256sum] = "f8bbf91e5a4d6315eee45e704372590b30e260ee83af6639d64557f51b067776"
SRC_URI[sha1-0.10.6.sha256sum] = "e3bf829a2d51ab4a5ddf1352d8470c140cadc8301b2ae1789db023f01cedd6ba"
SRC_URI[shlex-1.3.0.sha256sum] = "0fda2ff0d084019ba4d7c6f371c95d8fd75ce3524c3cb8fb653a3023f6323e64"
SRC_URI[signal-hook-registry-1.4.8.sha256sum] = "c4db69cba1110affc0e9f7bcd48bbf87b3f4fc7c61fc9155afd4c469eb3d6c1b"
SRC_URI[simd-adler32-0.3.8.sha256sum] = "e320a6c5ad31d271ad523dcf3ad13e2767ad8b1cb8f047f75a8aeaf8da139da2"
SRC_URI[simd_helpers-0.1.0.sha256sum] = "95890f873bec569a0362c235787f3aca6e1e887302ba4840839bcc6459c42da6"
SRC_URI[simdutf8-0.1.5.sha256sum] = "e3a9fe34e3e7a50316060351f37187a3f546bce95496156754b601a5fa71b76e"
SRC_URI[simplecss-0.2.2.sha256sum] = "7a9c6883ca9c3c7c90e888de77b7a5c849c779d25d74a1269b0218b14e8b136c"
SRC_URI[siphasher-1.0.2.sha256sum] = "b2aa850e253778c88a04c3d7323b043aeda9d3e30d5971937c1855769763678e"
SRC_URI[skia-bindings-0.90.0.sha256sum] = "8f6f96e00735f14a781aac8a6870c862b8cc831df6d8e4ad77ab78e11411b9af"
SRC_URI[skia-safe-0.90.0.sha256sum] = "6a71c01d325d40b1031dee67d251a5e0132e79e2a9ec272149a4f4a0d4b8b3be"
SRC_URI[skrifa-0.37.0.sha256sum] = "8c31071dedf532758ecf3fed987cdb4bd9509f900e026ab684b4ecb81ea49841"
SRC_URI[slab-0.4.12.sha256sum] = "0c790de23124f9ab44544d7ac05d60440adc586479ce501c1d6d7da3cd8c9cf5"
SRC_URI[slint-1.15.1.sha256sum] = "d25b87d458205e79efb30545cae083aec2ccb1b192c46a55ae6d54403cdacb33"
SRC_URI[slint-build-1.15.1.sha256sum] = "064ef470cc8ab046319db94d0727f080fbd05322d07d774eb6de607d97defb8d"
SRC_URI[slint-macros-1.15.1.sha256sum] = "5ecc09bbc42c780d5b7ed7d41f7573dfd67343e11cdac27c07b88a8f933958e6"
SRC_URI[slotmap-1.1.1.sha256sum] = "bdd58c3c93c3d278ca835519292445cb4b0d4dc59ccfdf7ceadaab3f8aeb4038"
SRC_URI[smallvec-1.15.1.sha256sum] = "67b1b7a3b5fe4f1376887184045fcf45c69e92af734b7aaddc05fb777b6fbd03"
SRC_URI[smithay-client-toolkit-0.19.2.sha256sum] = "3457dea1f0eb631b4034d61d4d8c32074caa6cd1ab2d59f2327bd8461e2c0016"
SRC_URI[smithay-client-toolkit-0.20.0.sha256sum] = "0512da38f5e2b31201a93524adb8d3136276fa4fe4aafab4e1f727a82b534cc0"
SRC_URI[smithay-clipboard-0.7.3.sha256sum] = "71704c03f739f7745053bde45fa203a46c58d25bc5c4efba1d9a60e9dba81226"
SRC_URI[smol_str-0.2.2.sha256sum] = "dd538fb6910ac1099850255cf94a94df6551fbdd602454387d0adb2d1ca6dead"
SRC_URI[smol_str-0.3.6.sha256sum] = "4aaa7368fcf4852a4c2dd92df0cace6a71f2091ca0a23391ce7f3a31833f1523"
SRC_URI[snafu-0.8.9.sha256sum] = "6e84b3f4eacbf3a1ce05eac6763b4d629d60cbc94d632e4092c54ade71f1e1a2"
SRC_URI[snafu-derive-0.8.9.sha256sum] = "c1c97747dbf44bb1ca44a561ece23508e99cb592e862f22222dcf42f51d1e451"
SRC_URI[socket2-0.6.3.sha256sum] = "3a766e1110788c36f4fa1c2b71b387a7815aa65f88ce0229841826633d93723e"
SRC_URI[softbuffer-0.4.8.sha256sum] = "aac18da81ebbf05109ab275b157c22a653bb3c12cf884450179942f81bcbf6c3"
SRC_URI[spin_on-0.1.1.sha256sum] = "076e103ed41b9864aa838287efe5f4e3a7a0362dd00671ae62a212e5e4612da2"
SRC_URI[stable_deref_trait-1.2.1.sha256sum] = "6ce2be8dc25455e1f91df71bfa12ad37d7af1092ae736f3a6cd0e37bc7810596"
SRC_URI[static_assertions-1.1.0.sha256sum] = "a2eb9349b6444b326872e140eb1cf5e7c522154d69e7a0ffb0fb81c06b37543f"
SRC_URI[strict-num-0.1.1.sha256sum] = "6637bab7722d379c8b41ba849228d680cc12d0a45ba1fa2b48f2a30577a06731"
SRC_URI[strsim-0.11.1.sha256sum] = "7da8b5736845d9f2fcb837ea5d9e2628564b3b043a70948a3f0b778838c5fb4f"
SRC_URI[strum-0.27.2.sha256sum] = "af23d6f6c1a224baef9d3f61e287d2761385a5b88fdab4eb4c6f11aeb54c4bcf"
SRC_URI[strum_macros-0.27.2.sha256sum] = "7695ce3845ea4b33927c055a39dc438a45b059f7c1b3d91d38d10355fb8cbca7"
SRC_URI[svgtypes-0.16.1.sha256sum] = "695b5790b3131dafa99b3bbfd25a216edb3d216dad9ca208d4657bfb8f2abc3d"
SRC_URI[swash-0.2.6.sha256sum] = "47846491253e976bdd07d0f9cc24b7daf24720d11309302ccbbc6e6b6e53550a"
SRC_URI[syn-2.0.117.sha256sum] = "e665b8803e7b1d2a727f4023456bbbbe74da67099c585258af0ad9c5013b9b99"
SRC_URI[sync_wrapper-1.0.2.sha256sum] = "0bf256ce5efdfa370213c1dabab5935a12e49f2c58d15e9eac2870d3b4f27263"
SRC_URI[synstructure-0.13.2.sha256sum] = "728a70f3dbaf5bab7f0c4b1ac8d7ae5ea60a4b5549c8a5914361c99147a709d2"
SRC_URI[sys-locale-0.3.2.sha256sum] = "8eab9a99a024a169fe8a903cf9d4a3b3601109bcc13bd9e3c6fff259138626c4"
SRC_URI[tap-1.0.1.sha256sum] = "55937e1799185b12863d447f42597ed69d9928686b8d88a1df17376a097d8369"
SRC_URI[tar-0.4.44.sha256sum] = "1d863878d212c87a19c1a610eb53bb01fe12951c0501cf5a0d65f724914a667a"
SRC_URI[tempfile-3.27.0.sha256sum] = "32497e9a4c7b38532efcdebeef879707aa9f794296a4f0244f6f69e9bc8574bd"
SRC_URI[text-size-1.1.1.sha256sum] = "f18aa187839b2bdb1ad2fa35ead8c4c2976b64e4363c386d45ac0f7ee85c9233"
SRC_URI[thiserror-1.0.69.sha256sum] = "b6aaf5339b578ea85b50e080feb250a3e8ae8cfcdff9a461c9ec2904bc923f52"
SRC_URI[thiserror-2.0.18.sha256sum] = "4288b5bcbc7920c07a1149a35cf9590a2aa808e0bc1eafaade0b80947865fbc4"
SRC_URI[thiserror-impl-1.0.69.sha256sum] = "4fee6c4efc90059e10f81e6d42c60a18f76588c3d74cb83a0b242a2b6c7504c1"
SRC_URI[thiserror-impl-2.0.18.sha256sum] = "ebc4ee7f67670e9b64d05fa4253e753e016c6c95ff35b89b7941d6b856dec1d5"
SRC_URI[tiff-0.11.3.sha256sum] = "b63feaf3343d35b6ca4d50483f94843803b0f51634937cc2ec519fc32232bc52"
SRC_URI[tiny-skia-0.11.4.sha256sum] = "83d13394d44dae3207b52a326c0c85a8bf87f1541f23b0d143811088497b09ab"
SRC_URI[tiny-skia-path-0.11.4.sha256sum] = "9c9e7fc0c2e86a30b117d0462aa261b72b7a99b7ebd7deb3a14ceda95c5bdc93"
SRC_URI[tiny-xlib-0.2.4.sha256sum] = "0324504befd01cab6e0c994f34b2ffa257849ee019d3fb3b64fb2c858887d89e"
SRC_URI[tinystr-0.8.2.sha256sum] = "42d3e9c45c09de15d06dd8acf5f4e0e399e85927b7f00711024eb7ae10fa4869"
SRC_URI[tinyvec-1.11.0.sha256sum] = "3e61e67053d25a4e82c844e8424039d9745781b3fc4f32b8d55ed50f5f667ef3"
SRC_URI[tinyvec_macros-0.1.1.sha256sum] = "1f3ccbac311fea05f86f61904b462b55fb3df8837a366dfc601a0161d0532f20"
SRC_URI[tokio-1.50.0.sha256sum] = "27ad5e34374e03cfffefc301becb44e9dc3c17584f414349ebe29ed26661822d"
SRC_URI[tokio-macros-2.6.1.sha256sum] = "5c55a2eff8b69ce66c84f85e1da1c233edc36ceb85a2058d11b0d6a3c7e7569c"
SRC_URI[tokio-native-tls-0.3.1.sha256sum] = "bbae76ab933c85776efabc971569dd6119c580d8f5d448769dec1764bf796ef2"
SRC_URI[toml-0.9.12+spec-1.1.0.sha256sum] = "cf92845e79fc2e2def6a5d828f0801e29a2f8acc037becc5ab08595c7d5e9863"
SRC_URI[toml_datetime-0.7.5+spec-1.1.0.sha256sum] = "92e1cfed4a3038bc5a127e35a2d360f145e1f4b971b551a2ba5fd7aedf7e1347"
SRC_URI[toml_datetime-1.0.0+spec-1.1.0.sha256sum] = "32c2555c699578a4f59f0cc68e5116c8d7cabbd45e1409b989d4be085b53f13e"
SRC_URI[toml_edit-0.24.1+spec-1.1.0.sha256sum] = "01f2eadbbc6b377a847be05f60791ef1058d9f696ecb51d2c07fe911d8569d8e"
SRC_URI[toml_edit-0.25.4+spec-1.1.0.sha256sum] = "7193cbd0ce53dc966037f54351dbbcf0d5a642c7f0038c382ef9e677ce8c13f2"
SRC_URI[toml_parser-1.0.9+spec-1.1.0.sha256sum] = "702d4415e08923e7e1ef96cd5727c0dfed80b4d2fa25db9647fe5eb6f7c5a4c4"
SRC_URI[toml_writer-1.0.6+spec-1.1.0.sha256sum] = "ab16f14aed21ee8bfd8ec22513f7287cd4a91aa92e44edfe2c17ddd004e92607"
SRC_URI[tower-0.5.3.sha256sum] = "ebe5ef63511595f1344e2d5cfa636d973292adc0eec1f0ad45fae9f0851ab1d4"
SRC_URI[tower-http-0.6.8.sha256sum] = "d4e6559d53cc268e5031cd8429d05415bc4cb4aefc4aa5d6cc35fbf5b924a1f8"
SRC_URI[tower-layer-0.3.3.sha256sum] = "121c2a6cda46980bb0fcd1647ffaf6cd3fc79a013de288782836f6df9c48780e"
SRC_URI[tower-service-0.3.3.sha256sum] = "8df9b6e13f2d32c91b9bd719c00d1958837bc7dec474d94952798cc8e69eeec3"
SRC_URI[tracing-0.1.44.sha256sum] = "63e71662fa4b2a2c3a26f570f037eb95bb1f85397f3cd8076caed2f026a6d100"
SRC_URI[tracing-attributes-0.1.31.sha256sum] = "7490cfa5ec963746568740651ac6781f701c9c5ea257c58e057f3ba8cf69e8da"
SRC_URI[tracing-core-0.1.36.sha256sum] = "db97caf9d906fbde555dd62fa95ddba9eecfd14cb388e4f491a66d74cd5fb79a"
SRC_URI[try-lock-0.2.5.sha256sum] = "e421abadd41a4225275504ea4d6566923418b7f05506fbc9c0fe86ba7396114b"
SRC_URI[ttf-parser-0.21.1.sha256sum] = "2c591d83f69777866b9126b24c6dd9a18351f177e49d625920d19f989fd31cf8"
SRC_URI[ttf-parser-0.25.1.sha256sum] = "d2df906b07856748fa3f6e0ad0cbaa047052d4a7dd609e231c4f72cee8c36f31"
SRC_URI[typed-index-collections-3.5.0.sha256sum] = "898160f1dfd383b4e92e17f0512a7d62f3c51c44937b23b6ffc3a1614a8eaccd"
SRC_URI[typenum-1.19.0.sha256sum] = "562d481066bde0658276a35467c4af00bdc6ee726305698a55b86e61d7ad82bb"
SRC_URI[udev-0.9.3.sha256sum] = "af4e37e9ea4401fc841ff54b9ddfc9be1079b1e89434c1a6a865dd68980f7e9f"
SRC_URI[uds_windows-1.2.1.sha256sum] = "f2f6fb2847f6742cd76af783a2a2c49e9375d0a111c7bef6f71cd9e738c72d6e"
SRC_URI[unicase-2.9.0.sha256sum] = "dbc4bc3a9f746d862c45cb89d705aa10f187bb96c76001afab07a0d35ce60142"
SRC_URI[unicode-bidi-0.3.18.sha256sum] = "5c1cb5db39152898a79168971543b1cb5020dff7fe43c8dc468b0885f5e29df5"
SRC_URI[unicode-bidi-mirroring-0.4.0.sha256sum] = "5dfa6e8c60bb66d49db113e0125ee8711b7647b5579dc7f5f19c42357ed039fe"
SRC_URI[unicode-ccc-0.4.0.sha256sum] = "ce61d488bcdc9bc8b5d1772c404828b17fc481c0a582b5581e95fb233aef503e"
SRC_URI[unicode-ident-1.0.24.sha256sum] = "e6e4313cd5fcd3dad5cafa179702e2b244f760991f45397d14d4ebf38247da75"
SRC_URI[unicode-linebreak-0.1.5.sha256sum] = "3b09c83c3c29d37506a3e260c08c03743a6bb66a9cd432c6934ab501a190571f"
SRC_URI[unicode-properties-0.1.4.sha256sum] = "7df058c713841ad818f1dc5d3fd88063241cc61f49f5fbea4b951e8cf5a8d71d"
SRC_URI[unicode-script-0.5.8.sha256sum] = "383ad40bb927465ec0ce7720e033cb4ca06912855fc35db31b5755d0de75b1ee"
SRC_URI[unicode-segmentation-1.12.0.sha256sum] = "f6ccf251212114b54433ec949fd6a7841275f9ada20dddd2f29e9ceea4501493"
SRC_URI[unicode-vo-0.1.0.sha256sum] = "b1d386ff53b415b7fe27b50bb44679e2cc4660272694b7b6f3326d8480823a94"
SRC_URI[unicode-width-0.2.2.sha256sum] = "b4ac048d71ede7ee76d585517add45da530660ef4390e49b098733c6e897f254"
SRC_URI[unicode-xid-0.2.6.sha256sum] = "ebc1c04c71510c7f702b52b7c350734c9ff1295c464a03335b00bb84fc54f853"
SRC_URI[unty-0.0.4.sha256sum] = "6d49784317cd0d1ee7ec5c716dd598ec5b4483ea832a2dced265471cc0f690ae"
SRC_URI[url-2.5.8.sha256sum] = "ff67a8a4397373c3ef660812acab3268222035010ab8680ec4215f38ba3d0eed"
SRC_URI[usvg-0.46.0.sha256sum] = "e419dff010bb12512b0ae9e3d2f318dfbdf0167fde7eb05465134d4e8756076f"
SRC_URI[utf-8-0.7.6.sha256sum] = "09cc8ee72d2a9becf2f2febe0205bbed8fc6615b7cb429ad062dc7b7ddd036a9"
SRC_URI[utf8_iter-1.0.4.sha256sum] = "b6c140620e7ffbb22c2dee59cafe6084a59b5ffc27a8859a5f0d494b5d52b6be"
SRC_URI[utf8parse-0.2.2.sha256sum] = "06abde3611657adf66d383f00b093d7faecc7fa57071cce2578660c9f1010821"
SRC_URI[uuid-1.22.0.sha256sum] = "a68d3c8f01c0cfa54a75291d83601161799e4a89a39e0929f4b0354d88757a37"
SRC_URI[v_frame-0.3.9.sha256sum] = "666b7727c8875d6ab5db9533418d7c764233ac9c0cff1d469aec8fa127597be2"
SRC_URI[vcpkg-0.2.15.sha256sum] = "accd4ea62f7bb7a82fe23066fb0957d48ef677f6eeb8215f372f52e48bb32426"
SRC_URI[version_check-0.9.5.sha256sum] = "0b928f33d975fc6ad9f86c8f283853ad26bdd5b10b7f1542aa2fa15e2289105a"
SRC_URI[vtable-0.3.0.sha256sum] = "753be81c38dff787d177b5939af1fa16f72f0d0d21a6b7d74ae56e29cd26f2a6"
SRC_URI[vtable-macro-0.3.0.sha256sum] = "8cfcf6171aa2b0f85718ca5888ca32f6edf61d1849f8e4b3786ad890e5b68f68"
SRC_URI[walkdir-2.5.0.sha256sum] = "29790946404f91d9c5d06f9874efddea1dc06c5efe94541a7d6863108e3a5e4b"
SRC_URI[want-0.3.1.sha256sum] = "bfa7760aed19e106de2c7c0b581b509f2f25d3dacaf737cb82ac61bc6d760b0e"
SRC_URI[wasi-0.11.1+wasi-snapshot-preview1.sha256sum] = "ccf3ec651a847eb01de73ccad15eb7d99f80485de043efb2f370cd654f4ea44b"
SRC_URI[wasip2-1.0.2+wasi-0.2.9.sha256sum] = "9517f9239f02c069db75e65f174b3da828fe5f5b945c4dd26bd25d89c03ebcf5"
SRC_URI[wasip3-0.4.0+wasi-0.3.0-rc-2026-01-06.sha256sum] = "5428f8bf88ea5ddc08faddef2ac4a67e390b88186c703ce6dbd955e1c145aca5"
SRC_URI[wasm-bindgen-0.2.114.sha256sum] = "6532f9a5c1ece3798cb1c2cfdba640b9b3ba884f5db45973a6f442510a87d38e"
SRC_URI[wasm-bindgen-futures-0.4.64.sha256sum] = "e9c5522b3a28661442748e09d40924dfb9ca614b21c00d3fd135720e48b67db8"
SRC_URI[wasm-bindgen-macro-0.2.114.sha256sum] = "18a2d50fcf105fb33bb15f00e7a77b772945a2ee45dcf454961fd843e74c18e6"
SRC_URI[wasm-bindgen-macro-support-0.2.114.sha256sum] = "03ce4caeaac547cdf713d280eda22a730824dd11e6b8c3ca9e42247b25c631e3"
SRC_URI[wasm-bindgen-shared-0.2.114.sha256sum] = "75a326b8c223ee17883a4251907455a2431acc2791c98c26279376490c378c16"
SRC_URI[wasm-encoder-0.244.0.sha256sum] = "990065f2fe63003fe337b932cfb5e3b80e0b4d0f5ff650e6985b1048f62c8319"
SRC_URI[wasm-metadata-0.244.0.sha256sum] = "bb0e353e6a2fbdc176932bbaab493762eb1255a7900fe0fea1a2f96c296cc909"
SRC_URI[wasmparser-0.244.0.sha256sum] = "47b807c72e1bac69382b3a6fb3dbe8ea4c0ed87ff5629b8685ae6b9a611028fe"
SRC_URI[wayland-backend-0.3.14.sha256sum] = "aa75f400b7f719bcd68b3f47cd939ba654cedeef690f486db71331eec4c6a406"
SRC_URI[wayland-client-0.31.13.sha256sum] = "ab51d9f7c071abeee76007e2b742499e535148035bb835f97aaed1338cf516c3"
SRC_URI[wayland-csd-frame-0.3.0.sha256sum] = "625c5029dbd43d25e6aa9615e88b829a5cad13b2819c4ae129fdbb7c31ab4c7e"
SRC_URI[wayland-cursor-0.31.13.sha256sum] = "4b3298683470fbdc6ca40151dfc48c8f2fd4c41a26e13042f801f85002384091"
SRC_URI[wayland-protocols-0.32.11.sha256sum] = "b23b5df31ceff1328f06ac607591d5ba360cf58f90c8fad4ac8d3a55a3c4aec7"
SRC_URI[wayland-protocols-experimental-20250721.0.1.sha256sum] = "40a1f863128dcaaec790d7b4b396cc9b9a7a079e878e18c47e6c2d2c5a8dcbb1"
SRC_URI[wayland-protocols-misc-0.3.11.sha256sum] = "429b99200febaf95d4f4e46deff6fe4382bcff3280ee16a41cf887b3c3364984"
SRC_URI[wayland-protocols-plasma-0.3.11.sha256sum] = "d392fc283a87774afc9beefcd6f931582bb97fe0e6ced0b306a62cb1d026527c"
SRC_URI[wayland-protocols-wlr-0.3.11.sha256sum] = "78248e4cc0eff8163370ba5c158630dcae1f3497a586b826eca2ef5f348d6235"
SRC_URI[wayland-scanner-0.31.9.sha256sum] = "c86287151a309799b821ca709b7345a048a2956af05957c89cb824ab919fa4e3"
SRC_URI[wayland-sys-0.31.10.sha256sum] = "374f6b70e8e0d6bf9461a32988fd553b59ff630964924dad6e4a4eb6bd538d17"
SRC_URI[web-sys-0.3.91.sha256sum] = "854ba17bb104abfb26ba36da9729addc7ce7f06f5c0f90f3c391f8461cca21f9"
SRC_URI[web-time-1.1.0.sha256sum] = "5a6580f308b1fad9207618087a65c04e7a10bc77e02c8e84e9b00dd4b12fa0bb"
SRC_URI[weezl-0.1.12.sha256sum] = "a28ac98ddc8b9274cb41bb4d9d4d5c425b6020c50c46f25559911905610b4a88"
SRC_URI[which-4.4.2.sha256sum] = "87ba24419a2078cd2b0f2ede2691b6c66d8e47836da3b6db8265ebad47afbfc7"
SRC_URI[winapi-0.3.9.sha256sum] = "5c839a674fcd7a98952e593242ea400abe93992746761e38641405d28b00f419"
SRC_URI[winapi-i686-pc-windows-gnu-0.4.0.sha256sum] = "ac3b87c63620426dd9b991e5ce0329eff545bccbbb34f3be09ff6fb6ab51b7b6"
SRC_URI[winapi-util-0.1.11.sha256sum] = "c2a7b1c03c876122aa43f3020e6c3c3ee5c05081c9a00739faf7503aeba10d22"
SRC_URI[winapi-x86_64-pc-windows-gnu-0.4.0.sha256sum] = "712e227841d057c1ee1cd2fb22fa7e5a5461ae8e48fa2ca79ec42cfc1931183f"
SRC_URI[windows-0.58.0.sha256sum] = "dd04d41d93c4992d421894c18c8b43496aa748dd4c081bac0dc93eb0489272b6"
SRC_URI[windows-0.61.3.sha256sum] = "9babd3a767a4c1aef6900409f85f5d53ce2544ccdfaa86dad48c91782c6d6893"
SRC_URI[windows-0.62.2.sha256sum] = "527fadee13e0c05939a6a05d5bd6eec6cd2e3dbd648b9f8e447c6518133d8580"
SRC_URI[windows-collections-0.2.0.sha256sum] = "3beeceb5e5cfd9eb1d76b381630e82c4241ccd0d27f1a39ed41b2760b255c5e8"
SRC_URI[windows-collections-0.3.2.sha256sum] = "23b2d95af1a8a14a3c7367e1ed4fc9c20e0a26e79551b1454d72583c97cc6610"
SRC_URI[windows-core-0.58.0.sha256sum] = "6ba6d44ec8c2591c134257ce647b7ea6b20335bf6379a27dac5f1641fcf59f99"
SRC_URI[windows-core-0.61.2.sha256sum] = "c0fdd3ddb90610c7638aa2b3a3ab2904fb9e5cdbecc643ddb3647212781c4ae3"
SRC_URI[windows-core-0.62.2.sha256sum] = "b8e83a14d34d0623b51dce9581199302a221863196a1dde71a7663a4c2be9deb"
SRC_URI[windows-future-0.2.1.sha256sum] = "fc6a41e98427b19fe4b73c550f060b59fa592d7d686537eebf9385621bfbad8e"
SRC_URI[windows-future-0.3.2.sha256sum] = "e1d6f90251fe18a279739e78025bd6ddc52a7e22f921070ccdc67dde84c605cb"
SRC_URI[windows-implement-0.58.0.sha256sum] = "2bbd5b46c938e506ecbce286b6628a02171d56153ba733b6c741fc627ec9579b"
SRC_URI[windows-implement-0.60.2.sha256sum] = "053e2e040ab57b9dc951b72c264860db7eb3b0200ba345b4e4c3b14f67855ddf"
SRC_URI[windows-interface-0.58.0.sha256sum] = "053c4c462dc91d3b1504c6fe5a726dd15e216ba718e84a0e46a88fbe5ded3515"
SRC_URI[windows-interface-0.59.3.sha256sum] = "3f316c4a2570ba26bbec722032c4099d8c8bc095efccdc15688708623367e358"
SRC_URI[windows-link-0.1.3.sha256sum] = "5e6ad25900d524eaabdbbb96d20b4311e1e7ae1699af4fb28c17ae66c80d798a"
SRC_URI[windows-link-0.2.1.sha256sum] = "f0805222e57f7521d6a62e36fa9163bc891acd422f971defe97d64e70d0a4fe5"
SRC_URI[windows-numerics-0.2.0.sha256sum] = "9150af68066c4c5c07ddc0ce30421554771e528bde427614c61038bc2c92c2b1"
SRC_URI[windows-numerics-0.3.1.sha256sum] = "6e2e40844ac143cdb44aead537bbf727de9b044e107a0f1220392177d15b0f26"
SRC_URI[windows-result-0.2.0.sha256sum] = "1d1043d8214f791817bab27572aaa8af63732e11bf84aa21a45a78d6c317ae0e"
SRC_URI[windows-result-0.3.4.sha256sum] = "56f42bd332cc6c8eac5af113fc0c1fd6a8fd2aa08a0119358686e5160d0586c6"
SRC_URI[windows-result-0.4.1.sha256sum] = "7781fa89eaf60850ac3d2da7af8e5242a5ea78d1a11c49bf2910bb5a73853eb5"
SRC_URI[windows-strings-0.1.0.sha256sum] = "4cd9b125c486025df0eabcb585e62173c6c9eddcec5d117d3b6e8c30e2ee4d10"
SRC_URI[windows-strings-0.4.2.sha256sum] = "56e6c93f3a0c3b36176cb1327a4958a0353d5d166c2a35cb268ace15e91d3b57"
SRC_URI[windows-strings-0.5.1.sha256sum] = "7837d08f69c77cf6b07689544538e017c1bfcf57e34b4c0ff58e6c2cd3b37091"
SRC_URI[windows-sys-0.45.0.sha256sum] = "75283be5efb2831d37ea142365f009c02ec203cd29a3ebecbc093d52315b66d0"
SRC_URI[windows-sys-0.48.0.sha256sum] = "677d2418bec65e3338edb076e806bc1ec15693c5d0104683f2efe857f61056a9"
SRC_URI[windows-sys-0.52.0.sha256sum] = "282be5f36a8ce781fad8c8ae18fa3f9beff57ec1b52cb3de0789201425d9a33d"
SRC_URI[windows-sys-0.59.0.sha256sum] = "1e38bc4d79ed67fd075bcc251a1c39b32a1776bbe92e5bef1f0bf1f8c531853b"
SRC_URI[windows-sys-0.60.2.sha256sum] = "f2f500e4d28234f72040990ec9d39e3a6b950f9f22d3dba18416c35882612bcb"
SRC_URI[windows-sys-0.61.2.sha256sum] = "ae137229bcbd6cdf0f7b80a31df61766145077ddf49416a728b02cb3921ff3fc"
SRC_URI[windows-targets-0.42.2.sha256sum] = "8e5180c00cd44c9b1c88adb3693291f1cd93605ded80c250a75d472756b4d071"
SRC_URI[windows-targets-0.48.5.sha256sum] = "9a2fa6e2155d7247be68c096456083145c183cbbbc2764150dda45a87197940c"
SRC_URI[windows-targets-0.52.6.sha256sum] = "9b724f72796e036ab90c1021d4780d4d3d648aca59e491e6b98e725b84e99973"
SRC_URI[windows-targets-0.53.5.sha256sum] = "4945f9f551b88e0d65f3db0bc25c33b8acea4d9e41163edf90dcd0b19f9069f3"
SRC_URI[windows-threading-0.1.0.sha256sum] = "b66463ad2e0ea3bbf808b7f1d371311c80e115c0b71d60efc142cafbcfb057a6"
SRC_URI[windows-threading-0.2.1.sha256sum] = "3949bd5b99cafdf1c7ca86b43ca564028dfe27d66958f2470940f73d86d75b37"
SRC_URI[windows_aarch64_gnullvm-0.42.2.sha256sum] = "597a5118570b68bc08d8d59125332c54f1ba9d9adeedeef5b99b02ba2b0698f8"
SRC_URI[windows_aarch64_gnullvm-0.48.5.sha256sum] = "2b38e32f0abccf9987a4e3079dfb67dcd799fb61361e53e2882c3cbaf0d905d8"
SRC_URI[windows_aarch64_gnullvm-0.52.6.sha256sum] = "32a4622180e7a0ec044bb555404c800bc9fd9ec262ec147edd5989ccd0c02cd3"
SRC_URI[windows_aarch64_gnullvm-0.53.1.sha256sum] = "a9d8416fa8b42f5c947f8482c43e7d89e73a173cead56d044f6a56104a6d1b53"
SRC_URI[windows_aarch64_msvc-0.42.2.sha256sum] = "e08e8864a60f06ef0d0ff4ba04124db8b0fb3be5776a5cd47641e942e58c4d43"
SRC_URI[windows_aarch64_msvc-0.48.5.sha256sum] = "dc35310971f3b2dbbf3f0690a219f40e2d9afcf64f9ab7cc1be722937c26b4bc"
SRC_URI[windows_aarch64_msvc-0.52.6.sha256sum] = "09ec2a7bb152e2252b53fa7803150007879548bc709c039df7627cabbd05d469"
SRC_URI[windows_aarch64_msvc-0.53.1.sha256sum] = "b9d782e804c2f632e395708e99a94275910eb9100b2114651e04744e9b125006"
SRC_URI[windows_i686_gnu-0.42.2.sha256sum] = "c61d927d8da41da96a81f029489353e68739737d3beca43145c8afec9a31a84f"
SRC_URI[windows_i686_gnu-0.48.5.sha256sum] = "a75915e7def60c94dcef72200b9a8e58e5091744960da64ec734a6c6e9b3743e"
SRC_URI[windows_i686_gnu-0.52.6.sha256sum] = "8e9b5ad5ab802e97eb8e295ac6720e509ee4c243f69d781394014ebfe8bbfa0b"
SRC_URI[windows_i686_gnu-0.53.1.sha256sum] = "960e6da069d81e09becb0ca57a65220ddff016ff2d6af6a223cf372a506593a3"
SRC_URI[windows_i686_gnullvm-0.52.6.sha256sum] = "0eee52d38c090b3caa76c563b86c3a4bd71ef1a819287c19d586d7334ae8ed66"
SRC_URI[windows_i686_gnullvm-0.53.1.sha256sum] = "fa7359d10048f68ab8b09fa71c3daccfb0e9b559aed648a8f95469c27057180c"
SRC_URI[windows_i686_msvc-0.42.2.sha256sum] = "44d840b6ec649f480a41c8d80f9c65108b92d89345dd94027bfe06ac444d1060"
SRC_URI[windows_i686_msvc-0.48.5.sha256sum] = "8f55c233f70c4b27f66c523580f78f1004e8b5a8b659e05a4eb49d4166cca406"
SRC_URI[windows_i686_msvc-0.52.6.sha256sum] = "240948bc05c5e7c6dabba28bf89d89ffce3e303022809e73deaefe4f6ec56c66"
SRC_URI[windows_i686_msvc-0.53.1.sha256sum] = "1e7ac75179f18232fe9c285163565a57ef8d3c89254a30685b57d83a38d326c2"
SRC_URI[windows_x86_64_gnu-0.42.2.sha256sum] = "8de912b8b8feb55c064867cf047dda097f92d51efad5b491dfb98f6bbb70cb36"
SRC_URI[windows_x86_64_gnu-0.48.5.sha256sum] = "53d40abd2583d23e4718fddf1ebec84dbff8381c07cae67ff7768bbf19c6718e"
SRC_URI[windows_x86_64_gnu-0.52.6.sha256sum] = "147a5c80aabfbf0c7d901cb5895d1de30ef2907eb21fbbab29ca94c5b08b1a78"
SRC_URI[windows_x86_64_gnu-0.53.1.sha256sum] = "9c3842cdd74a865a8066ab39c8a7a473c0778a3f29370b5fd6b4b9aa7df4a499"
SRC_URI[windows_x86_64_gnullvm-0.42.2.sha256sum] = "26d41b46a36d453748aedef1486d5c7a85db22e56aff34643984ea85514e94a3"
SRC_URI[windows_x86_64_gnullvm-0.48.5.sha256sum] = "0b7b52767868a23d5bab768e390dc5f5c55825b6d30b86c844ff2dc7414044cc"
SRC_URI[windows_x86_64_gnullvm-0.52.6.sha256sum] = "24d5b23dc417412679681396f2b49f3de8c1473deb516bd34410872eff51ed0d"
SRC_URI[windows_x86_64_gnullvm-0.53.1.sha256sum] = "0ffa179e2d07eee8ad8f57493436566c7cc30ac536a3379fdf008f47f6bb7ae1"
SRC_URI[windows_x86_64_msvc-0.42.2.sha256sum] = "9aec5da331524158c6d1a4ac0ab1541149c0b9505fde06423b02f5ef0106b9f0"
SRC_URI[windows_x86_64_msvc-0.48.5.sha256sum] = "ed94fce61571a4006852b7389a063ab983c02eb1bb37b47f8272ce92d06d9538"
SRC_URI[windows_x86_64_msvc-0.52.6.sha256sum] = "589f6da84c646204747d1270a2a5661ea66ed1cced2631d546fdfb155959f9ec"
SRC_URI[windows_x86_64_msvc-0.53.1.sha256sum] = "d6bbff5f0aada427a1e5a6da5f1f98158182f26556f345ac9e04d36d0ebed650"
SRC_URI[winit-0.30.13.sha256sum] = "a6755fa58a9f8350bd1e472d4c3fcc25f824ec358933bba33306d0b63df5978d"
SRC_URI[winnow-0.7.15.sha256sum] = "df79d97927682d2fd8adb29682d1140b343be4ac0f08fd68b7765d9c059d3945"
SRC_URI[wit-bindgen-0.51.0.sha256sum] = "d7249219f66ced02969388cf2bb044a09756a083d0fab1e566056b04d9fbcaa5"
SRC_URI[wit-bindgen-core-0.51.0.sha256sum] = "ea61de684c3ea68cb082b7a88508a8b27fcc8b797d738bfc99a82facf1d752dc"
SRC_URI[wit-bindgen-rust-0.51.0.sha256sum] = "b7c566e0f4b284dd6561c786d9cb0142da491f46a9fbed79ea69cdad5db17f21"
SRC_URI[wit-bindgen-rust-macro-0.51.0.sha256sum] = "0c0f9bfd77e6a48eccf51359e3ae77140a7f50b1e2ebfe62422d8afdaffab17a"
SRC_URI[wit-component-0.244.0.sha256sum] = "9d66ea20e9553b30172b5e831994e35fbde2d165325bec84fc43dbf6f4eb9cb2"
SRC_URI[wit-parser-0.244.0.sha256sum] = "ecc8ac4bc1dc3381b7f59c34f00b67e18f910c2c0f50015669dde7def656a736"
SRC_URI[write-fonts-0.43.0.sha256sum] = "886614b5ce857341226aa091f3c285e450683894acaaa7887f366c361efef79d"
SRC_URI[writeable-0.6.2.sha256sum] = "9edde0db4769d2dc68579893f2306b26c6ecfbe0ef499b013d731b7b9247e0b9"
SRC_URI[wyz-0.5.1.sha256sum] = "05f360fc0b24296329c78fda852a1e9ae82de9cf7b27dae4b7f62f118f77b9ed"
SRC_URI[x11-clipboard-0.9.3.sha256sum] = "662d74b3d77e396b8e5beb00b9cad6a9eccf40b2ef68cc858784b14c41d535a3"
SRC_URI[x11-dl-2.21.0.sha256sum] = "38735924fedd5314a6e548792904ed8c6de6636285cb9fec04d5b1db85c1516f"
SRC_URI[x11rb-0.13.2.sha256sum] = "9993aa5be5a26815fe2c3eacfc1fde061fc1a1f094bf1ad2a18bf9c495dd7414"
SRC_URI[x11rb-protocol-0.13.2.sha256sum] = "ea6fc2961e4ef194dcbfe56bb845534d0dc8098940c7e5c012a258bfec6701bd"
SRC_URI[xattr-1.6.1.sha256sum] = "32e45ad4206f6d2479085147f02bc2ef834ac85886624a23575ae137c8aa8156"
SRC_URI[xcursor-0.3.10.sha256sum] = "bec9e4a500ca8864c5b47b8b482a73d62e4237670e5b5f1d6b9e3cae50f28f2b"
SRC_URI[xkbcommon-0.9.0.sha256sum] = "a7a974f48060a14e95705c01f24ad9c3345022f4d97441b8a36beb7ed5c4a02d"
SRC_URI[xkbcommon-dl-0.4.2.sha256sum] = "d039de8032a9a8856a6be89cea3e5d12fdd82306ab7c94d74e6deab2460651c5"
SRC_URI[xkeysym-0.2.1.sha256sum] = "b9cc00251562a284751c9973bace760d86c0276c471b4be569fe6b068ee97a56"
SRC_URI[xml-rs-0.8.28.sha256sum] = "3ae8337f8a065cfc972643663ea4279e04e7256de865aa66fe25cec5fb912d3f"
SRC_URI[xmlwriter-0.1.0.sha256sum] = "ec7a2a501ed189703dba8b08142f057e887dfc4b2cc4db2d343ac6376ba3e0b9"
SRC_URI[y4m-0.8.0.sha256sum] = "7a5a4b21e1a62b67a2970e6831bc091d7b87e119e7f9791aef9702e3bef04448"
SRC_URI[yazi-0.2.1.sha256sum] = "e01738255b5a16e78bbb83e7fbba0a1e7dd506905cfc53f4622d89015a03fbb5"
SRC_URI[yeslogic-fontconfig-sys-6.0.0.sha256sum] = "503a066b4c037c440169d995b869046827dbc71263f6e8f3be6d77d4f3229dbd"
SRC_URI[yoke-0.8.1.sha256sum] = "72d6e5c6afb84d73944e5cedb052c4680d5657337201555f9f2a16b7406d4954"
SRC_URI[yoke-derive-0.8.1.sha256sum] = "b659052874eb698efe5b9e8cf382204678a0086ebf46982b79d6ca3182927e5d"
SRC_URI[zbus-5.14.0.sha256sum] = "ca82f95dbd3943a40a53cfded6c2d0a2ca26192011846a1810c4256ef92c60bc"
SRC_URI[zbus-lockstep-0.5.2.sha256sum] = "6998de05217a084b7578728a9443d04ea4cd80f2a0839b8d78770b76ccd45863"
SRC_URI[zbus-lockstep-macros-0.5.2.sha256sum] = "10da05367f3a7b7553c8cdf8fa91aee6b64afebe32b51c95177957efc47ca3a0"
SRC_URI[zbus_macros-5.14.0.sha256sum] = "897e79616e84aac4b2c46e9132a4f63b93105d54fe8c0e8f6bffc21fa8d49222"
SRC_URI[zbus_names-4.3.1.sha256sum] = "ffd8af6d5b78619bab301ff3c560a5bd22426150253db278f164d6cf3b72c50f"
SRC_URI[zbus_xml-5.1.0.sha256sum] = "441a0064125265655bccc3a6af6bef56814d9277ac83fce48b1cd7e160b80eac"
SRC_URI[zeno-0.3.3.sha256sum] = "6df3dc4292935e51816d896edcd52aa30bc297907c26167fec31e2b0c6a32524"
SRC_URI[zerocopy-0.8.42.sha256sum] = "f2578b716f8a7a858b7f02d5bd870c14bf4ddbbcf3a4c05414ba6503640505e3"
SRC_URI[zerocopy-derive-0.8.42.sha256sum] = "7e6cc098ea4d3bd6246687de65af3f920c430e236bee1e3bf2e441463f08a02f"
SRC_URI[zerofrom-0.1.6.sha256sum] = "50cc42e0333e05660c3587f3bf9d0478688e15d870fab3346451ce7f8c9fbea5"
SRC_URI[zerofrom-derive-0.1.6.sha256sum] = "d71e5d6e06ab090c67b5e44993ec16b72dcbaabc526db883a360057678b48502"
SRC_URI[zeroize-1.8.2.sha256sum] = "b97154e67e32c85465826e8bcc1c59429aaaf107c1e4a9e53c8d8ccd5eff88d0"
SRC_URI[zerotrie-0.2.3.sha256sum] = "2a59c17a5562d507e4b54960e8569ebee33bee890c70aa3fe7b97e85a9fd7851"
SRC_URI[zerovec-0.11.5.sha256sum] = "6c28719294829477f525be0186d13efa9a3c602f7ec202ca9e353d310fb9a002"
SRC_URI[zerovec-derive-0.11.2.sha256sum] = "eadce39539ca5cb3985590102671f2567e659fca9666581ad3411d59207951f3"
SRC_URI[zmij-1.0.21.sha256sum] = "b8848ee67ecc8aedbaf3e4122217aff892639231befc6a1b58d29fff4c2cabaa"
SRC_URI[zune-core-0.5.1.sha256sum] = "cb8a0807f7c01457d0379ba880ba6322660448ddebc890ce29bb64da71fb40f9"
SRC_URI[zune-inflate-0.2.54.sha256sum] = "73ab332fe2f6680068f3582b16a24f90ad7096d5d39b974d1c0aff0125116f02"
SRC_URI[zune-jpeg-0.5.13.sha256sum] = "ec5f41c76397b7da451efd19915684f727d7e1d516384ca6bd0ec43ec94de23c"
SRC_URI[zvariant-5.10.0.sha256sum] = "5708299b21903bbe348e94729f22c49c55d04720a004aa350f1f9c122fd2540b"
SRC_URI[zvariant_derive-5.10.0.sha256sum] = "5b59b012ebe9c46656f9cc08d8da8b4c726510aef12559da3e5f1bf72780752c"
SRC_URI[zvariant_utils-3.3.0.sha256sum] = "f75c23a64ef8f40f13a6989991e643554d9bef1d682a281160cf0c1bc389c5e9"
