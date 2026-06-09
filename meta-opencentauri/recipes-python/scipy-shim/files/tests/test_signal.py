"""Test scipy.signal shim against real scipy for 10 random param sets per function."""
import sys
import os

# Add scipy path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "scipy"))
import signal as shim_signal

import numpy as np

try:
    import scipy.signal as real_signal
    HAS_SCIPY = True
except ImportError:
    HAS_SCIPY = False

np.random.seed(42)


def test_butter():
    """Test butter(N, Wn, btype, fs, output='sos') against real scipy."""
    np.random.seed(42)
    failures = []
    for i in range(10):
        N = np.random.randint(1, 6)
        fs = np.random.uniform(10, 200)
        wn = np.random.uniform(1, fs / 2 - 1)
        btype = np.random.choice(["lowpass", "highpass"])
        
        sos_shim = shim_signal.butter(N, wn, btype, fs=fs, output="sos")
        
        if HAS_SCIPY:
            sos_real = real_signal.butter(N, wn, btype, fs=fs, output="sos")
            if not np.allclose(sos_shim, sos_real, atol=1e-9):
                failures.append(f"butter[{i}] N={N}, Wn={wn:.2f}, {btype}, fs={fs:.1f}")
        else:
            # Smoke test
            if sos_shim.shape[1] != 6 or sos_shim.shape[0] < 1:
                failures.append(f"butter[{i}] bad shape {sos_shim.shape}")
    
    if failures:
        raise AssertionError(f"butter failures: {failures}")


def test_iirnotch():
    """Test iirnotch(w0, Q, fs) against real scipy."""
    np.random.seed(43)
    failures = []
    for i in range(10):
        fs = np.random.uniform(10, 200)
        w0 = np.random.uniform(1, fs / 2 - 1)
        Q = np.random.uniform(0.5, 10)
        
        ba_shim = shim_signal.iirnotch(w0, Q, fs=fs)
        
        if HAS_SCIPY:
            ba_real = real_signal.iirnotch(w0, Q, fs=fs)
            if not (np.allclose(ba_shim[0], ba_real[0], atol=1e-9) and
                    np.allclose(ba_shim[1], ba_real[1], atol=1e-9)):
                failures.append(f"iirnotch[{i}] w0={w0:.2f}, Q={Q:.2f}, fs={fs:.1f}")
        else:
            if len(ba_shim[0]) != 3 or len(ba_shim[1]) != 3:
                failures.append(f"iirnotch[{i}] bad shape")
    
    if failures:
        raise AssertionError(f"iirnotch failures: {failures}")


def test_tf2sos():
    """Test tf2sos(b, a) against real scipy."""
    np.random.seed(44)
    failures = []
    for i in range(10):
        nb = np.random.randint(1, 6)
        na = np.random.randint(1, 6)
        b = np.random.randn(nb)
        a = np.random.randn(na)
        a[0] = 1.0  # Ensure monic
        
        sos_shim = shim_signal.tf2sos(b, a)
        
        if HAS_SCIPY:
            sos_real = real_signal.tf2sos(b, a)
            if not np.allclose(sos_shim, sos_real, atol=1e-9):
                failures.append(f"tf2sos[{i}] nb={nb}, na={na}")
        else:
            if sos_shim.shape[1] != 6 or sos_shim.shape[0] < 1:
                failures.append(f"tf2sos[{i}] bad shape {sos_shim.shape}")
    
    if failures:
        raise AssertionError(f"tf2sos failures: {failures}")


def test_sosfilt_zi():
    """Test sosfilt_zi(sos) against real scipy."""
    np.random.seed(45)
    failures = []
    for i in range(10):
        n_sections = np.random.randint(1, 5)
        sos = np.random.randn(n_sections, 6)
        sos[:, 3] = 1.0  # Force a0=1 for SOS format
        
        zi_shim = shim_signal.sosfilt_zi(sos)
        
        if HAS_SCIPY:
            zi_real = real_signal.sosfilt_zi(sos)
            if not np.allclose(zi_shim, zi_real, atol=1e-9):
                failures.append(f"sosfilt_zi[{i}] n_sections={n_sections}")
        else:
            if zi_shim.shape != (n_sections, 2):
                failures.append(f"sosfilt_zi[{i}] bad shape {zi_shim.shape}")
    
    if failures:
        raise AssertionError(f"sosfilt_zi failures: {failures}")


if __name__ == "__main__":
    if not HAS_SCIPY:
        print("WARNING: scipy not available - running smoke tests only")
    
    tests = [test_butter, test_iirnotch, test_tf2sos, test_sosfilt_zi]
    passed = 0
    failed = 0
    
    for test in tests:
        try:
            test()
            print(f"PASS: {test.__name__}")
            passed += 1
        except AssertionError as e:
            print(f"FAIL: {test.__name__} - {e}")
            failed += 1
        except Exception as e:
            print(f"ERROR: {test.__name__} - {e}")
            failed += 1
    
    print(f"\nResults: {passed} passed, {failed} failed")
    sys.exit(0 if failed == 0 else 1)