PACKAGECONFIG:remove:class-target = "gdbm tk tcl readline"
PACKAGECONFIG:append:class-target = " lto pgo"

BAD_RECOMMENDATIONS += " \
    python3-tkinter \
    python3-idle \
    python3-pydoc \
    python3-doctest \
    python3-2to3 \
    python3-dev \
"
