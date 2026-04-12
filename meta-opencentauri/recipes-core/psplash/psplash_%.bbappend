FILESEXTRAPATHS:prepend := "${THISDIR}/files:"
SRC_URI += " \
    file://psplash-colors.h;subdir=git \
    file://psplash-bar.png \
"
SPLASH_IMAGES = " \
    file://cosmos_1.png;outsuffix=default \
"

do_compile:prepend() {
    import subprocess
    workdir = d.getVar('WORKDIR')
    s = d.getVar('S')
    convertscript = "%s/make-image-header.sh" % s
    if subprocess.call([convertscript, os.path.join(workdir, 'psplash-bar.png'), 'BAR'], cwd=workdir):
        bb.fatal("Error converting bar image")
    import shutil
    shutil.copyfile(os.path.join(workdir, 'psplash-bar-img.h'), '%s/psplash-bar-img.h' % d.getVar('B'))
}

do_install:append() {
    install -d ${D}/var
    install -d ${D}/var/psplash
}
