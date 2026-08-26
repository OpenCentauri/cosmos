# OpenCentauri Yocto Build Environment
FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

# Install required packages
RUN apt-get update && apt-get install -y \
    gawk wget git diffstat unzip texinfo gcc build-essential \
    chrpath socat cpio python3 python3-pip python3-pexpect xz-utils debianutils \
    iputils-ping python3-git python3-jinja2 libegl1-mesa libsdl1.2-dev \
    pylint xterm python3-subunit mesa-common-dev zstd liblz4-tool file locales \
    bmap-tools sunxi-tools \
    clang libclang-dev \
    sudo vim nano curl git-lfs \
    && rm -rf /var/lib/apt/lists/*

# Generate required locales
RUN sed -i 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    locale-gen en_US.UTF-8
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

# Create build user with a high-entropy, non-default UID. A volume-mounted
# host directory (typically root-owned or 1000-owned) is not writable by the
# container builder if their uid matches neither owner nor group, so use an
# exotic uid to maximize compatibility.
RUN useradd -m -s /bin/bash -u 1234 builder && \
    echo "builder ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

# Allow other users (e.g. host uid mapped via --user) to traverse into /home/builder.
RUN chmod 755 /home/builder

# Pre-create the build directory owned by builder so volume-mounting a
# root-owned host directory doesn't leave bitbake.lock unreadable (and to
# pre-populate any needed files before switching user).
RUN mkdir -p /home/builder/opencentauri-repo/build && \
    chown -R builder:builder /home/builder/opencentauri-repo

WORKDIR /home/builder/opencentauri-repo

# Set up environment
USER builder

# Clone repository (will be overwritten by volume mount)
RUN git config --global user.email "builder@localhost" && \
    git config --global user.name "Builder"

# Configure git for large/remote repos: longer timeout, retries, larger buffer.
# Prevents 'early EOF' / 'invalid index-pack output' on big bare-clone transfers
# (e.g. lvgl ~500MB) over flaky links inside the container.
RUN git config --global http.postBuffer 524288000 && \
    git config --global http.lowSpeedLimit 1000 && \
    git config --global http.lowSpeedTime 600

# Default command
CMD ["/bin/bash"]
