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
    sudo vim nano curl \
    && rm -rf /var/lib/apt/lists/*

# Generate required locales
RUN sed -i 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
    locale-gen en_US.UTF-8
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

# Create build user
RUN useradd -m -s /bin/bash builder && \
    echo "builder ALL=(ALL) NOPASSWD:ALL" >> /etc/sudoers

WORKDIR /home/builder

# Set up environment
USER builder

# Clone repository (will be overwritten by volume mount)
RUN git config --global user.email "builder@localhost" && \
    git config --global user.name "Builder"

# Default command
CMD ["/bin/bash"]
