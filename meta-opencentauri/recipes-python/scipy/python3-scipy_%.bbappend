PACKAGECONFIG = "openblas pythran"

# Limit parallelism for scipy-native to prevent OOM on the CI runner
PARALLEL_MAKE:pn-python3-scipy-native = "-j1"
