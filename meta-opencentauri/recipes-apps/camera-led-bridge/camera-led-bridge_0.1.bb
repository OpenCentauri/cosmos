inherit cargo update-rc.d

SUMMARY = "Camera LED Bridge"
DESCRIPTION = "Control camera LED based on GPIO input"
HOMEPAGE = "https://github.com/OpenCentauri/OpenCentauri"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://../LICENSE;md5=0a18a528575a965515cdd877f88b3c4c"

SRC_URI = "git://github.com/OpenCentauri/OpenCentauri.git;protocol=https;branch=main \
    crate://crates.io/base64/0.22.1 \
    crate://crates.io/bitflags/2.11.0 \
    crate://crates.io/bytes/1.11.1 \
    crate://crates.io/cfg-if/1.0.4 \
    crate://crates.io/cfg_aliases/0.2.1 \
    crate://crates.io/cookie/0.18.1 \
    crate://crates.io/cookie_store/0.22.1 \
    crate://crates.io/deranged/0.5.8 \
    crate://crates.io/displaydoc/0.2.5 \
    crate://crates.io/document-features/0.2.12 \
    crate://crates.io/equivalent/1.0.2 \
    crate://crates.io/form_urlencoded/1.2.2 \
    crate://crates.io/hashbrown/0.16.1 \
    crate://crates.io/http/1.4.0 \
    crate://crates.io/httparse/1.10.1 \
    crate://crates.io/icu_collections/2.1.1 \
    crate://crates.io/icu_locale_core/2.1.1 \
    crate://crates.io/icu_normalizer/2.1.1 \
    crate://crates.io/icu_normalizer_data/2.1.1 \
    crate://crates.io/icu_properties/2.1.2 \
    crate://crates.io/icu_properties_data/2.1.2 \
    crate://crates.io/icu_provider/2.1.1 \
    crate://crates.io/idna/1.1.0 \
    crate://crates.io/idna_adapter/1.2.1 \
    crate://crates.io/indexmap/2.13.0 \
    crate://crates.io/itoa/1.0.18 \
    crate://crates.io/libc/0.2.183 \
    crate://crates.io/litemap/0.8.1 \
    crate://crates.io/litrs/1.0.0 \
    crate://crates.io/log/0.4.29 \
    crate://crates.io/memchr/2.8.0 \
    crate://crates.io/nix/0.31.2 \
    crate://crates.io/num-conv/0.2.0 \
    crate://crates.io/percent-encoding/2.3.2 \
    crate://crates.io/potential_utf/0.1.4 \
    crate://crates.io/powerfmt/0.2.0 \
    crate://crates.io/proc-macro2/1.0.106 \
    crate://crates.io/quote/1.0.45 \
    crate://crates.io/serde/1.0.228 \
    crate://crates.io/serde_core/1.0.228 \
    crate://crates.io/serde_derive/1.0.228 \
    crate://crates.io/serde_json/1.0.149 \
    crate://crates.io/smallvec/1.15.1 \
    crate://crates.io/stable_deref_trait/1.2.1 \
    crate://crates.io/syn/2.0.117 \
    crate://crates.io/synstructure/0.13.2 \
    crate://crates.io/time/0.3.47 \
    crate://crates.io/time-core/0.1.8 \
    crate://crates.io/time-macros/0.2.27 \
    crate://crates.io/tinystr/0.8.2 \
    crate://crates.io/unicode-ident/1.0.24 \
    crate://crates.io/ureq/3.3.0 \
    crate://crates.io/ureq-proto/0.6.0 \
    crate://crates.io/url/2.5.8 \
    crate://crates.io/utf8-zero/0.8.1 \
    crate://crates.io/utf8_iter/1.0.4 \
    crate://crates.io/version_check/0.9.5 \
    crate://crates.io/writeable/0.6.2 \
    crate://crates.io/yoke/0.8.1 \
    crate://crates.io/yoke-derive/0.8.1 \
    crate://crates.io/zerofrom/0.1.6 \
    crate://crates.io/zerofrom-derive/0.1.6 \
    crate://crates.io/zerotrie/0.2.3 \
    crate://crates.io/zerovec/0.11.5 \
    crate://crates.io/zerovec-derive/0.11.2 \
    crate://crates.io/zmij/1.0.21 \
    file://camera-led-bridge.init \
"
SRCREV = "d9b995f4662bcde7235e2b7c1e34c522306b9d85"

SRC_URI[base64-0.22.1.sha256sum] = "72b3254f16251a8381aa12e40e3c4d2f0199f8c6508fbecb9d91f575e0fbb8c6"
SRC_URI[bitflags-2.11.0.sha256sum] = "843867be96c8daad0d758b57df9392b6d8d271134fce549de6ce169ff98a92af"
SRC_URI[bytes-1.11.1.sha256sum] = "1e748733b7cbc798e1434b6ac524f0c1ff2ab456fe201501e6497c8417a4fc33"
SRC_URI[cfg-if-1.0.4.sha256sum] = "9330f8b2ff13f34540b44e946ef35111825727b38d33286ef986142615121801"
SRC_URI[cfg_aliases-0.2.1.sha256sum] = "613afe47fcd5fac7ccf1db93babcb082c5994d996f20b8b159f2ad1658eb5724"
SRC_URI[cookie-0.18.1.sha256sum] = "4ddef33a339a91ea89fb53151bd0a4689cfce27055c291dfa69945475d22c747"
SRC_URI[cookie_store-0.22.1.sha256sum] = "15b2c103cf610ec6cae3da84a766285b42fd16aad564758459e6ecf128c75206"
SRC_URI[deranged-0.5.8.sha256sum] = "7cd812cc2bc1d69d4764bd80df88b4317eaef9e773c75226407d9bc0876b211c"
SRC_URI[displaydoc-0.2.5.sha256sum] = "97369cbbc041bc366949bc74d34658d6cda5621039731c6310521892a3a20ae0"
SRC_URI[document-features-0.2.12.sha256sum] = "d4b8a88685455ed29a21542a33abd9cb6510b6b129abadabdcef0f4c55bc8f61"
SRC_URI[equivalent-1.0.2.sha256sum] = "877a4ace8713b0bcf2a4e7eec82529c029f1d0619886d18145fea96c3ffe5c0f"
SRC_URI[form_urlencoded-1.2.2.sha256sum] = "cb4cb245038516f5f85277875cdaa4f7d2c9a0fa0468de06ed190163b1581fcf"
SRC_URI[hashbrown-0.16.1.sha256sum] = "841d1cc9bed7f9236f321df977030373f4a4163ae1a7dbfe1a51a2c1a51d9100"
SRC_URI[http-1.4.0.sha256sum] = "e3ba2a386d7f85a81f119ad7498ebe444d2e22c2af0b86b069416ace48b3311a"
SRC_URI[httparse-1.10.1.sha256sum] = "6dbf3de79e51f3d586ab4cb9d5c3e2c14aa28ed23d180cf89b4df0454a69cc87"
SRC_URI[icu_collections-2.1.1.sha256sum] = "4c6b649701667bbe825c3b7e6388cb521c23d88644678e83c0c4d0a621a34b43"
SRC_URI[icu_locale_core-2.1.1.sha256sum] = "edba7861004dd3714265b4db54a3c390e880ab658fec5f7db895fae2046b5bb6"
SRC_URI[icu_normalizer-2.1.1.sha256sum] = "5f6c8828b67bf8908d82127b2054ea1b4427ff0230ee9141c54251934ab1b599"
SRC_URI[icu_normalizer_data-2.1.1.sha256sum] = "7aedcccd01fc5fe81e6b489c15b247b8b0690feb23304303a9e560f37efc560a"
SRC_URI[icu_properties-2.1.2.sha256sum] = "020bfc02fe870ec3a66d93e677ccca0562506e5872c650f893269e08615d74ec"
SRC_URI[icu_properties_data-2.1.2.sha256sum] = "616c294cf8d725c6afcd8f55abc17c56464ef6211f9ed59cccffe534129c77af"
SRC_URI[icu_provider-2.1.1.sha256sum] = "85962cf0ce02e1e0a629cc34e7ca3e373ce20dda4c4d7294bbd0bf1fdb59e614"
SRC_URI[idna-1.1.0.sha256sum] = "3b0875f23caa03898994f6ddc501886a45c7d3d62d04d2d90788d47be1b1e4de"
SRC_URI[idna_adapter-1.2.1.sha256sum] = "3acae9609540aa318d1bc588455225fb2085b9ed0c4f6bd0d9d5bcd86f1a0344"
SRC_URI[indexmap-2.13.0.sha256sum] = "7714e70437a7dc3ac8eb7e6f8df75fd8eb422675fc7678aff7364301092b1017"
SRC_URI[itoa-1.0.18.sha256sum] = "8f42a60cbdf9a97f5d2305f08a87dc4e09308d1276d28c869c684d7777685682"
SRC_URI[libc-0.2.183.sha256sum] = "b5b646652bf6661599e1da8901b3b9522896f01e736bad5f723fe7a3a27f899d"
SRC_URI[litemap-0.8.1.sha256sum] = "6373607a59f0be73a39b6fe456b8192fcc3585f602af20751600e974dd455e77"
SRC_URI[litrs-1.0.0.sha256sum] = "11d3d7f243d5c5a8b9bb5d6dd2b1602c0cb0b9db1621bafc7ed66e35ff9fe092"
SRC_URI[log-0.4.29.sha256sum] = "5e5032e24019045c762d3c0f28f5b6b8bbf38563a65908389bf7978758920897"
SRC_URI[memchr-2.8.0.sha256sum] = "f8ca58f447f06ed17d5fc4043ce1b10dd205e060fb3ce5b979b8ed8e59ff3f79"
SRC_URI[nix-0.31.2.sha256sum] = "5d6d0705320c1e6ba1d912b5e37cf18071b6c2e9b7fa8215a1e8a7651966f5d3"
SRC_URI[num-conv-0.2.0.sha256sum] = "cf97ec579c3c42f953ef76dbf8d55ac91fb219dde70e49aa4a6b7d74e9919050"
SRC_URI[percent-encoding-2.3.2.sha256sum] = "9b4f627cb1b25917193a259e49bdad08f671f8d9708acfd5fe0a8c1455d87220"
SRC_URI[potential_utf-0.1.4.sha256sum] = "b73949432f5e2a09657003c25bca5e19a0e9c84f8058ca374f49e0ebe605af77"
SRC_URI[powerfmt-0.2.0.sha256sum] = "439ee305def115ba05938db6eb1644ff94165c5ab5e9420d1c1bcedbba909391"
SRC_URI[proc-macro2-1.0.106.sha256sum] = "8fd00f0bb2e90d81d1044c2b32617f68fcb9fa3bb7640c23e9c748e53fb30934"
SRC_URI[quote-1.0.45.sha256sum] = "41f2619966050689382d2b44f664f4bc593e129785a36d6ee376ddf37259b924"
SRC_URI[serde-1.0.228.sha256sum] = "9a8e94ea7f378bd32cbbd37198a4a91436180c5bb472411e48b5ec2e2124ae9e"
SRC_URI[serde_core-1.0.228.sha256sum] = "41d385c7d4ca58e59fc732af25c3983b67ac852c1a25000afe1175de458b67ad"
SRC_URI[serde_derive-1.0.228.sha256sum] = "d540f220d3187173da220f885ab66608367b6574e925011a9353e4badda91d79"
SRC_URI[serde_json-1.0.149.sha256sum] = "83fc039473c5595ace860d8c4fafa220ff474b3fc6bfdb4293327f1a37e94d86"
SRC_URI[smallvec-1.15.1.sha256sum] = "67b1b7a3b5fe4f1376887184045fcf45c69e92af734b7aaddc05fb777b6fbd03"
SRC_URI[stable_deref_trait-1.2.1.sha256sum] = "6ce2be8dc25455e1f91df71bfa12ad37d7af1092ae736f3a6cd0e37bc7810596"
SRC_URI[syn-2.0.117.sha256sum] = "e665b8803e7b1d2a727f4023456bbbbe74da67099c585258af0ad9c5013b9b99"
SRC_URI[synstructure-0.13.2.sha256sum] = "728a70f3dbaf5bab7f0c4b1ac8d7ae5ea60a4b5549c8a5914361c99147a709d2"
SRC_URI[time-0.3.47.sha256sum] = "743bd48c283afc0388f9b8827b976905fb217ad9e647fae3a379a9283c4def2c"
SRC_URI[time-core-0.1.8.sha256sum] = "7694e1cfe791f8d31026952abf09c69ca6f6fa4e1a1229e18988f06a04a12dca"
SRC_URI[time-macros-0.2.27.sha256sum] = "2e70e4c5a0e0a8a4823ad65dfe1a6930e4f4d756dcd9dd7939022b5e8c501215"
SRC_URI[tinystr-0.8.2.sha256sum] = "42d3e9c45c09de15d06dd8acf5f4e0e399e85927b7f00711024eb7ae10fa4869"
SRC_URI[unicode-ident-1.0.24.sha256sum] = "e6e4313cd5fcd3dad5cafa179702e2b244f760991f45397d14d4ebf38247da75"
SRC_URI[ureq-3.3.0.sha256sum] = "dea7109cdcd5864d4eeb1b58a1648dc9bf520360d7af16ec26d0a9354bafcfc0"
SRC_URI[ureq-proto-0.6.0.sha256sum] = "e994ba84b0bd1b1b0cf92878b7ef898a5c1760108fe7b6010327e274917a808c"
SRC_URI[url-2.5.8.sha256sum] = "ff67a8a4397373c3ef660812acab3268222035010ab8680ec4215f38ba3d0eed"
SRC_URI[utf8-zero-0.8.1.sha256sum] = "b8848ee67ecc8aedbaf3e4122217aff892639231befc6a1b58d29fff4c2cabaa"
SRC_URI[utf8_iter-1.0.4.sha256sum] = "b6c140620e7ffbb22c2dee59cafe6084a59b5ffc27a8859a5f0d494b5d52b6be"
SRC_URI[version_check-0.9.5.sha256sum] = "0b928f33d975fc6ad9f86c8f283853ad26bdd5b10b7f1542aa2fa15e2289105a"
SRC_URI[writeable-0.6.2.sha256sum] = "9edde0db4769d2dc68579893f2306b26c6ecfbe0ef499b013d731b7b9247e0b9"
SRC_URI[yoke-0.8.1.sha256sum] = "72d6e5c6afb84d73944e5cedb052c4680d5657337201555f9f2a16b7406d4954"
SRC_URI[yoke-derive-0.8.1.sha256sum] = "b659052874eb698efe5b9e8cf382204678a0086ebf46982b79d6ca3182927e5d"
SRC_URI[zerofrom-0.1.6.sha256sum] = "50cc42e0333e05660c3587f3bf9d0478688e15d870fab3346451ce7f8c9fbea5"
SRC_URI[zerofrom-derive-0.1.6.sha256sum] = "d71e5d6e06ab090c67b5e44993ec16b72dcbaabc526db883a360057678b48502"
SRC_URI[zerotrie-0.2.3.sha256sum] = "2a59c17a5562d507e4b54960e8569ebee33bee890c70aa3fe7b97e85a9fd7851"
SRC_URI[zerovec-0.11.5.sha256sum] = "6c28719294829477f525be0186d13efa9a3c602f7ec202ca9e353d310fb9a002"
SRC_URI[zerovec-derive-0.11.2.sha256sum] = "eadce39539ca5cb3985590102671f2567e659fca9666581ad3411d59207951f3"
SRC_URI[zmij-1.0.21.sha256sum] = "b8848ee67ecc8aedbaf3e4122217aff892639231befc6a1b58d29fff4c2cabaa"

S = "${WORKDIR}/git/camera-led-bridge"

INITSCRIPT_NAME = "camera-led-bridge"
INITSCRIPT_PARAMS = "defaults 96 4"

INSANE_SKIP:${PN} += "already-stripped"

do_install:append() {
    install -d ${D}${sysconfdir}/init.d
    install -m 0755 ${WORKDIR}/camera-led-bridge.init ${D}${sysconfdir}/init.d/camera-led-bridge
}
