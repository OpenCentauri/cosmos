"""scipy shim — drop-in scipy replacement providing the scipy.signal functions Kalico's hx711s uses.
Import scipy.signal → this module. No Kalico source changes needed.
"""
__version__ = "1.14.1-shim.1"
__all__ = ["signal"]

_SHIMMED_ATTRS = {
    "signal", "_lib", "__version__", "__config__", "__all__", "__name__",
    "__file__", "__loader__", "__spec__", "__cached__", "__builtins__",
    "__doc__", "__package__", "__path__",
}

def __getattr__(name):
    import importlib
    if name in _SHIMMED_ATTRS:
        if name in ("signal", "_lib", "__config__"):
            return importlib.import_module(f"scipy.{name}")
        raise AttributeError(name)
    raise AttributeError(
        f"scipy shim: '{name}' is not provided. "
        f"The hx711s tap filter only needs scipy.signal.butter/iirnotch/tf2sos/sosfilt_zi. "
        f"If Kalico now needs more, extend the shim or revert to full scipy."
    )