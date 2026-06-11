"""Minimal numpy.lib replacement — only function_base (kaiser, interp).

All other lib/ submodules (index_tricks, histograms, npyio, type_check, etc.)
are never used by kalico and have been removed to save space.
"""
from . import function_base
from .function_base import *

__all__ = function_base.__all__