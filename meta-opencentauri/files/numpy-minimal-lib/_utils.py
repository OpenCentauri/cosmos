"""Minimal _utils stub — only set_module needed by function_base."""
def set_module(module):
    """Private decorator for overriding __module__ on a function or class."""
    def decorator(func):
        if module is not None:
            func.__module__ = module
        return func
    return decorator