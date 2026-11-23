PACKAGECONFIG:append = " gpl x264"
PACKAGECONFIG:remove = "avdevice alsa theora bzlib lzma xv xcb"

EXTRA_FFCONF = " \
    --disable-everything \
    --enable-protocol=file \
    --enable-demuxer=image2 \
    --enable-parser=mjpeg \
    --enable-decoder=mjpeg \
    --enable-filter=scale \
    --enable-encoder=libx264 \
    --enable-encoder=mjpeg \
    --enable-muxer=mp4 \
    --enable-muxer=image2 \
"
