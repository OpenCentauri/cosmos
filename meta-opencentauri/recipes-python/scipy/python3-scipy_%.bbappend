PACKAGECONFIG = "openblas pythran"

# Limit parallelism for scipy-native to prevent OOM on the CI runner.
# PARALLEL_MAKE only affects make-based builds; scipy uses meson/ninja via
# python_mesonpy, so we must pass -j1 via PEP517_BUILD_OPTS compile-args.
PEP517_BUILD_OPTS:append:class-native = " -Ccompile-args=-j1"
