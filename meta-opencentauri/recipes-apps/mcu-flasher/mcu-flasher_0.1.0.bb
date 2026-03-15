inherit cargo

SUMMARY = "Serial Multiplexer"
HOMEPAGE = "https://github.com/OpenCentauri/OpenCentauri"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://../LICENSE;md5=0a18a528575a965515cdd877f88b3c4c"

SRC_URI += " \
    git://github.com/OpenCentauri/OpenCentauri.git;protocol=https;nobranch=1;branch=main \
"
SRCREV = "fb829bd0a469e1c54cb9d2aa1bd98e14afd9f351"
PR = "r1"

S = "${WORKDIR}/git/mcu-flasher"
CARGO_SRC_DIR = ""

DEPENDS += "eudev pkgconfig-native"

INSANE_SKIP:${PN} += "already-stripped"

SRC_URI += " \
    crate://crates.io/anstream/0.6.21 \
    crate://crates.io/anstyle-parse/0.2.7 \
    crate://crates.io/anstyle-query/1.1.5 \
    crate://crates.io/anstyle-wincon/3.0.11 \
    crate://crates.io/anstyle/1.0.13 \
    crate://crates.io/bitflags/1.3.2 \
    crate://crates.io/bitflags/2.11.0 \
    crate://crates.io/block-buffer/0.10.4 \
    crate://crates.io/cfg-if/1.0.4 \
    crate://crates.io/clap/4.5.60 \
    crate://crates.io/clap_builder/4.5.60 \
    crate://crates.io/clap_derive/4.5.55 \
    crate://crates.io/clap_lex/1.0.0 \
    crate://crates.io/colorchoice/1.0.4 \
    crate://crates.io/core-foundation-sys/0.8.7 \
    crate://crates.io/core-foundation/0.10.0 \
    crate://crates.io/crc16/0.3.4 \
    crate://crates.io/crypto-common/0.1.7 \
    crate://crates.io/digest/0.10.7 \
    crate://crates.io/generic-array/0.14.7 \
    crate://crates.io/heck/0.5.0 \
    crate://crates.io/io-kit-sys/0.4.1 \
    crate://crates.io/is_terminal_polyfill/1.70.2 \
    crate://crates.io/libc/0.2.183 \
    crate://crates.io/libudev-sys/0.1.4 \
    crate://crates.io/libudev/0.3.0 \
    crate://crates.io/log/0.3.9 \
    crate://crates.io/log/0.4.29 \
    crate://crates.io/mach2/0.4.3 \
    crate://crates.io/md-5/0.10.6 \
    crate://crates.io/nix/0.26.4 \
    crate://crates.io/once_cell_polyfill/1.70.2 \
    crate://crates.io/pkg-config/0.3.32 \
    crate://crates.io/proc-macro2/1.0.106 \
    crate://crates.io/quote/1.0.40 \
    crate://crates.io/scopeguard/1.2.0 \
    crate://crates.io/serialport/4.8.1 \
    crate://crates.io/strsim/0.11.1 \
    crate://crates.io/syn/2.0.117 \
    crate://crates.io/thiserror-impl/2.0.18 \
    crate://crates.io/thiserror/2.0.18 \
    crate://crates.io/typenum/1.19.0 \
    crate://crates.io/unescaper/0.1.8 \
    crate://crates.io/unicode-ident/1.0.24 \
    crate://crates.io/utf8parse/0.2.2 \
    crate://crates.io/version_check/0.9.5 \
    crate://crates.io/windows-link/0.2.1 \
    crate://crates.io/windows-sys/0.52.0 \
    crate://crates.io/windows-sys/0.61.2 \
    crate://crates.io/windows-targets/0.52.6 \
    crate://crates.io/windows_aarch64_gnullvm/0.52.6 \
    crate://crates.io/windows_aarch64_msvc/0.52.6 \
    crate://crates.io/windows_i686_gnu/0.52.6 \
    crate://crates.io/windows_i686_gnullvm/0.52.6 \
    crate://crates.io/windows_i686_msvc/0.52.6 \
    crate://crates.io/windows_x86_64_gnu/0.52.6 \
    crate://crates.io/windows_x86_64_gnullvm/0.52.6 \
    crate://crates.io/windows_x86_64_msvc/0.52.6 \
"

SRC_URI[anstream-0.6.21.sha256sum] = "43d5b281e737544384e969a5ccad3f1cdd24b48086a0fc1b2a5262a26b8f4f4a"
SRC_URI[anstyle-parse-0.2.7.sha256sum] = "4e7644824f0aa2c7b9384579234ef10eb7efb6a0deb83f9630a49594dd9c15c2"
SRC_URI[anstyle-query-1.1.5.sha256sum] = "40c48f72fd53cd289104fc64099abca73db4166ad86ea0b4341abe65af83dadc"
SRC_URI[anstyle-wincon-3.0.11.sha256sum] = "291e6a250ff86cd4a820112fb8898808a366d8f9f58ce16d1f538353ad55747d"
SRC_URI[anstyle-1.0.13.sha256sum] = "5192cca8006f1fd4f7237516f40fa183bb07f8fbdfedaa0036de5ea9b0b45e78"
SRC_URI[bitflags-1.3.2.sha256sum] = "bef38d45163c2f1dde094a7dfd33ccf595c92905c8f8f4fdc18d06fb1037718a"
SRC_URI[bitflags-2.11.0.sha256sum] = "843867be96c8daad0d758b57df9392b6d8d271134fce549de6ce169ff98a92af"
SRC_URI[block-buffer-0.10.4.sha256sum] = "3078c7629b62d3f0439517fa394996acacc5cbc91c5a20d8c658e77abd503a71"
SRC_URI[cfg-if-1.0.4.sha256sum] = "9330f8b2ff13f34540b44e946ef35111825727b38d33286ef986142615121801"
SRC_URI[clap-4.5.60.sha256sum] = "2797f34da339ce31042b27d23607e051786132987f595b02ba4f6a6dffb7030a"
SRC_URI[clap_builder-4.5.60.sha256sum] = "24a241312cea5059b13574bb9b3861cabf758b879c15190b37b6d6fd63ab6876"
SRC_URI[clap_derive-4.5.55.sha256sum] = "a92793da1a46a5f2a02a6f4c46c6496b28c43638adea8306fcb0caa1634f24e5"
SRC_URI[clap_lex-1.0.0.sha256sum] = "3a822ea5bc7590f9d40f1ba12c0dc3c2760f3482c6984db1573ad11031420831"
SRC_URI[colorchoice-1.0.4.sha256sum] = "b05b61dc5112cbb17e4b6cd61790d9845d13888356391624cbe7e41efeac1e75"
SRC_URI[core-foundation-sys-0.8.7.sha256sum] = "773648b94d0e5d620f64f280777445740e61fe701025087ec8b57f45c791888b"
SRC_URI[core-foundation-0.10.0.sha256sum] = "b55271e5c8c478ad3f38ad24ef34923091e0548492a266d19b3c0b4d82574c63"
SRC_URI[crc16-0.3.4.sha256sum] = "11a65c4797332f3e3a5945e0377875afc79b1bdc87082a4f98ac1ef15b47e2dd"
SRC_URI[crypto-common-0.1.7.sha256sum] = "78c8292055d1c1df0cce5d180393dc8cce0abec0a7102adb6c7b1eef6016d60a"
SRC_URI[digest-0.10.7.sha256sum] = "9ed9a281f7bc9b7576e61468ba615a66a5c8cfdff42420a70aa82701a3b1e292"
SRC_URI[generic-array-0.14.7.sha256sum] = "85649ca51fd72272d7821adaf274ad91c288277713d9c18820d8499a7ff69e9a"
SRC_URI[heck-0.5.0.sha256sum] = "2304e00983f87ffb38b55b444b5e3b60a884b5d30c0fca7d82fe33449bbe55ea"
SRC_URI[io-kit-sys-0.4.1.sha256sum] = "617ee6cf8e3f66f3b4ea67a4058564628cde41901316e19f559e14c7c72c5e7b"
SRC_URI[is_terminal_polyfill-1.70.2.sha256sum] = "a6cb138bb79a146c1bd460005623e142ef0181e3d0219cb493e02f7d08a35695"
SRC_URI[libc-0.2.183.sha256sum] = "b5b646652bf6661599e1da8901b3b9522896f01e736bad5f723fe7a3a27f899d"
SRC_URI[libudev-sys-0.1.4.sha256sum] = "3c8469b4a23b962c1396b9b451dda50ef5b283e8dd309d69033475fa9b334324"
SRC_URI[libudev-0.3.0.sha256sum] = "78b324152da65df7bb95acfcaab55e3097ceaab02fb19b228a9eb74d55f135e0"
SRC_URI[log-0.3.9.sha256sum] = "e19e8d5c34a3e0e2223db8e060f9e8264aeeb5c5fc64a4ee9965c062211c024b"
SRC_URI[log-0.4.29.sha256sum] = "5e5032e24019045c762d3c0f28f5b6b8bbf38563a65908389bf7978758920897"
SRC_URI[mach2-0.4.3.sha256sum] = "d640282b302c0bb0a2a8e0233ead9035e3bed871f0b7e81fe4a1ec829765db44"
SRC_URI[md-5-0.10.6.sha256sum] = "d89e7ee0cfbedfc4da3340218492196241d89eefb6dab27de5df917a6d2e78cf"
SRC_URI[nix-0.26.4.sha256sum] = "598beaf3cc6fdd9a5dfb1630c2800c7acd31df7aaf0f565796fba2b53ca1af1b"
SRC_URI[once_cell_polyfill-1.70.2.sha256sum] = "384b8ab6d37215f3c5301a95a4accb5d64aa607f1fcb26a11b5303878451b4fe"
SRC_URI[pkg-config-0.3.32.sha256sum] = "7edddbd0b52d732b21ad9a5fab5c704c14cd949e5e9a1ec5929a24fded1b904c"
SRC_URI[proc-macro2-1.0.106.sha256sum] = "8fd00f0bb2e90d81d1044c2b32617f68fcb9fa3bb7640c23e9c748e53fb30934"
SRC_URI[quote-1.0.40.sha256sum] = "1885c039570dc00dcb4ff087a89e185fd56bae234ddc7f056a945bf36467248d"
SRC_URI[scopeguard-1.2.0.sha256sum] = "94143f37725109f92c262ed2cf5e59bce7498c01bcc1502d7b9afe439a4e9f49"
SRC_URI[serialport-4.8.1.sha256sum] = "21f60a586160667241d7702c420fc223939fb3c0bb8d3fac84f78768e8970dee"
SRC_URI[strsim-0.11.1.sha256sum] = "7da8b5736845d9f2fcb837ea5d9e2628564b3b043a70948a3f0b778838c5fb4f"
SRC_URI[syn-2.0.117.sha256sum] = "e665b8803e7b1d2a727f4023456bbbbe74da67099c585258af0ad9c5013b9b99"
SRC_URI[thiserror-impl-2.0.18.sha256sum] = "ebc4ee7f67670e9b64d05fa4253e753e016c6c95ff35b89b7941d6b856dec1d5"
SRC_URI[thiserror-2.0.18.sha256sum] = "4288b5bcbc7920c07a1149a35cf9590a2aa808e0bc1eafaade0b80947865fbc4"
SRC_URI[typenum-1.19.0.sha256sum] = "562d481066bde0658276a35467c4af00bdc6ee726305698a55b86e61d7ad82bb"
SRC_URI[unescaper-0.1.8.sha256sum] = "4064ed685c487dbc25bd3f0e9548f2e34bab9d18cefc700f9ec2dba74ba1138e"
SRC_URI[unicode-ident-1.0.24.sha256sum] = "e6e4313cd5fcd3dad5cafa179702e2b244f760991f45397d14d4ebf38247da75"
SRC_URI[utf8parse-0.2.2.sha256sum] = "06abde3611657adf66d383f00b093d7faecc7fa57071cce2578660c9f1010821"
SRC_URI[version_check-0.9.5.sha256sum] = "0b928f33d975fc6ad9f86c8f283853ad26bdd5b10b7f1542aa2fa15e2289105a"
SRC_URI[windows-link-0.2.1.sha256sum] = "f0805222e57f7521d6a62e36fa9163bc891acd422f971defe97d64e70d0a4fe5"
SRC_URI[windows-sys-0.52.0.sha256sum] = "282be5f36a8ce781fad8c8ae18fa3f9beff57ec1b52cb3de0789201425d9a33d"
SRC_URI[windows-sys-0.61.2.sha256sum] = "ae137229bcbd6cdf0f7b80a31df61766145077ddf49416a728b02cb3921ff3fc"
SRC_URI[windows-targets-0.52.6.sha256sum] = "9b724f72796e036ab90c1021d4780d4d3d648aca59e491e6b98e725b84e99973"
SRC_URI[windows_aarch64_gnullvm-0.52.6.sha256sum] = "32a4622180e7a0ec044bb555404c800bc9fd9ec262ec147edd5989ccd0c02cd3"
SRC_URI[windows_aarch64_msvc-0.52.6.sha256sum] = "09ec2a7bb152e2252b53fa7803150007879548bc709c039df7627cabbd05d469"
SRC_URI[windows_i686_gnu-0.52.6.sha256sum] = "8e9b5ad5ab802e97eb8e295ac6720e509ee4c243f69d781394014ebfe8bbfa0b"
SRC_URI[windows_i686_gnullvm-0.52.6.sha256sum] = "0eee52d38c090b3caa76c563b86c3a4bd71ef1a819287c19d586d7334ae8ed66"
SRC_URI[windows_i686_msvc-0.52.6.sha256sum] = "240948bc05c5e7c6dabba28bf89d89ffce3e303022809e73deaefe4f6ec56c66"
SRC_URI[windows_x86_64_gnu-0.52.6.sha256sum] = "147a5c80aabfbf0c7d901cb5895d1de30ef2907eb21fbbab29ca94c5b08b1a78"
SRC_URI[windows_x86_64_gnullvm-0.52.6.sha256sum] = "24d5b23dc417412679681396f2b49f3de8c1473deb516bd34410872eff51ed0d"
SRC_URI[windows_x86_64_msvc-0.52.6.sha256sum] = "589f6da84c646204747d1270a2a5661ea66ed1cced2631d546fdfb155959f9ec"
