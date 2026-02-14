DEPENDS += "u-boot-tools-native"

FILESEXTRAPATHS:prepend := "${THISDIR}/${PN}:"

PACKAGE_ARCH = "${MACHINE_ARCH}"

SRC_URI:append:elegoo-centauri-carbon1 = " file://elegoo-centauri-carbon1.dts;subdir=linux-6.6.85/arch/${ARCH}/boot/dts/allwinner \
	file://0001-Add-elegoo-centauri-carbon-dts.patch"

#do_install:append() {
#    if [ -f ${D}/${KERNEL_IMAGEDEST}/zImage-${KERNEL_VERSION} ]; then
#        ln -sf zImage-${KERNEL_VERSION} ${D}/${KERNEL_IMAGEDEST}/zImage
#    fi
#}

#do_deploy:append() {
#    if [ -e ${DEPLOYDIR}/zImage-${MACHINE}.dtb.bin ]; then
#        mkimage -A arm -O linux -T kernel -C none \
#            -a 0x43000000 -e 0x43000000 \
#            -n "Linux" \
#            -d ${DEPLOYDIR}/zImage-${MACHINE}.dtb.bin \
#            ${DEPLOYDIR}/uImage-dtb
#    fi
#}

#do_deploy:append() {
#    DTB_BASENAME=$(basename ${KERNEL_DTB})
#
#    if [ -e ${DEPLOYDIR}/${DTB_BASENAME} ]; then
#        # Extract the raw zImage from the uImage by stripping the 64-byte header
#        dd if=${DEPLOYDIR}/uImage bs=64 skip=1 of=${DEPLOYDIR}/zImage-extracted
#
#        # Append the DTB to the raw zImage
#        cat ${DEPLOYDIR}/zImage-extracted ${DEPLOYDIR}/${DTB_BASENAME} > ${DEPLOYDIR}/zImage-dtb
#
#        # Re-wrap as uImage
#        mkimage -A arm -O linux -T kernel -C none \
#            -a ${UBOOT_ENTRYPOINT} -e ${UBOOT_ENTRYPOINT} \
#            -n "Linux-${KERNEL_VERSION}" \
#            -d ${DEPLOYDIR}/zImage-dtb \
#            ${DEPLOYDIR}/uImage-dtb
#
#        # Clean up intermediate files
#        rm -f ${DEPLOYDIR}/zImage-extracted ${DEPLOYDIR}/zImage-dtb
#
#        bbnote "Created uImage-dtb with appended ${DTB_BASENAME} at load address ${UBOOT_ENTRYPOINT}"
#    else
#        bbwarn "Could not find ${DTB_BASENAME} in ${DEPLOYDIR}"
#    fi
#}
