PACKAGECONFIG = "openblas pythran"

# Limit parallelism for scipy to prevent OOM on the CI runner.
# Both native and target builds use meson/ninja via python_mesonpy,
# so we pass -j1 via PEP517_BUILD_OPTS to serialise compilation.
PEP517_BUILD_OPTS:append = " -Ccompile-args=-j1"
