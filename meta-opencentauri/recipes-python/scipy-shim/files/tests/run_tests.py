#!/usr/bin/env python3
"""No-dep test runner for scipy shim - runs test_signal.py standalone."""
import sys
import os

# Add parent dir to path so we can import scipy
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

import numpy as np

# Import shim directly
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "scipy"))
import signal as shim_signal

# Check if real scipy is available
try:
    import scipy.signal as real_signal
    HAS_SCIPY = True
except ImportError:
    HAS_SCIPY = False

def run_tests():
    """Test butter, iirnotch, tf2sos, sosfilt_zi against real scipy if available."""
    np.random.seed(42)
    
    tests_passed = 0
    tests_failed = 0
    
    print("=" * 60)
    print("scipy shim validation tests")
    print("=" * 60)
    
    if not HAS_SCIPY:
        print("\nWARNING: real scipy not available - running smoke tests only")
        print("         (butter/iirnotch output shape checks)")
    
    # ---------- butter ----------
    print("\n[butter] testing 10 random parameter sets...")
    for i in range(10):
        N = np.random.randint(1, 6)
        fs = np.random.uniform(10, 200)
        wn = np.random.uniform(1, fs / 2 - 1)
        btype = np.random.choice(["lowpass", "highpass"])
        
        try:
            sos_shim = shim_signal.butter(N, wn, btype, fs=fs, output="sos")
            
            if HAS_SCIPY:
                sos_real = real_signal.butter(N, wn, btype, fs=fs, output="sos")
                if np.allclose(sos_shim, sos_real, atol=1e-9):
                    print(f"  [{i+1}] PASS  N={N}, Wn={wn:.2f}, {btype}, fs={fs:.1f}")
                    tests_passed += 1
                else:
                    print(f"  [{i+1}] FAIL  N={N}, Wn={wn:.2f}, {btype}, fs={fs:.1f}")
                    print(f"         shim shape={sos_shim.shape}, real shape={sos_real.shape}")
                    tests_failed += 1
            else:
                # Smoke test: check shape is reasonable
                if sos_shim.shape[1] == 6 and sos_shim.shape[0] >= 1:
                    print(f"  [{i+1}] PASS  N={N}, Wn={wn:.2f}, {btype}, fs={fs:.1f} (shape={sos_shim.shape})")
                    tests_passed += 1
                else:
                    print(f"  [{i+1}] FAIL  bad shape: {sos_shim.shape}")
                    tests_failed += 1
        except Exception as e:
            print(f"  [{i+1}] ERROR: {e}")
            tests_failed += 1
    
    # ---------- iirnotch ----------
    print("\n[iirnotch] testing 10 random parameter sets...")
    for i in range(10):
        fs = np.random.uniform(10, 200)
        w0 = np.random.uniform(1, fs / 2 - 1)
        Q = np.random.uniform(0.5, 10)
        
        try:
            ba_shim = shim_signal.iirnotch(w0, Q, fs=fs)
            
            if HAS_SCIPY:
                ba_real = real_signal.iirnotch(w0, Q, fs=fs)
                if np.allclose(ba_shim[0], ba_real[0], atol=1e-9) and np.allclose(ba_shim[1], ba_real[1], atol=1e-9):
                    print(f"  [{i+1}] PASS  w0={w0:.2f}, Q={Q:.2f}, fs={fs:.1f}")
                    tests_passed += 1
                else:
                    print(f"  [{i+1}] FAIL  w0={w0:.2f}, Q={Q:.2f}, fs={fs:.1f}")
                    tests_failed += 1
            else:
                if len(ba_shim[0]) == 3 and len(ba_shim[1]) == 3:
                    print(f"  [{i+1}] PASS  w0={w0:.2f}, Q={Q:.2f}, fs={fs:.1f} (shape=3,3)")
                    tests_passed += 1
                else:
                    print(f"  [{i+1}] FAIL  bad shape")
                    tests_failed += 1
        except Exception as e:
            print(f"  [{i+1}] ERROR: {e}")
            tests_failed += 1
    
    # ---------- tf2sos ----------
    print("\n[tf2sos] testing 10 random parameter sets...")
    for i in range(10):
        # Random filter orders
        nb = np.random.randint(1, 6)
        na = np.random.randint(1, 6)
        b = np.random.randn(nb)
        a = np.random.randn(na)
        a[0] = 1.0  # Ensure monic
        
        try:
            sos_shim = shim_signal.tf2sos(b, a)
            
            if HAS_SCIPY:
                sos_real = real_signal.tf2sos(b, a)
                if np.allclose(sos_shim, sos_real, atol=1e-9):
                    print(f"  [{i+1}] PASS  nb={nb}, na={na}")
                    tests_passed += 1
                else:
                    print(f"  [{i+1}] FAIL  nb={nb}, na={na}")
                    print(f"         shim shape={sos_shim.shape}, real shape={sos_real.shape}")
                    tests_failed += 1
            else:
                if sos_shim.shape[1] == 6 and sos_shim.shape[0] >= 1:
                    print(f"  [{i+1}] PASS  nb={nb}, na={na} (shape={sos_shim.shape})")
                    tests_passed += 1
                else:
                    print(f"  [{i+1}] FAIL  bad shape: {sos_shim.shape}")
                    tests_failed += 1
        except Exception as e:
            print(f"  [{i+1}] ERROR: {e}")
            tests_failed += 1
    
    # ---------- sosfilt_zi ----------
    print("\n[sosfilt_zi] testing 10 random parameter sets...")
    for i in range(10):
        n_sections = np.random.randint(1, 5)
        sos = np.random.randn(n_sections, 6)
        sos[:, 3] = 1.0  # Force a0=1 for SOS format
        
        try:
            zi_shim = shim_signal.sosfilt_zi(sos)
            
            if HAS_SCIPY:
                zi_real = real_signal.sosfilt_zi(sos)
                if np.allclose(zi_shim, zi_real, atol=1e-9):
                    print(f"  [{i+1}] PASS  n_sections={n_sections}")
                    tests_passed += 1
                else:
                    print(f"  [{i+1}] FAIL  n_sections={n_sections}")
                    tests_failed += 1
            else:
                if zi_shim.shape == (n_sections, 2):
                    print(f"  [{i+1}] PASS  n_sections={n_sections} (shape={zi_shim.shape})")
                    tests_passed += 1
                else:
                    print(f"  [{i+1}] FAIL  bad shape: {zi_shim.shape}")
                    tests_failed += 1
        except Exception as e:
            print(f"  [{i+1}] ERROR: {e}")
            tests_failed += 1
    
    # ---------- sosfilt (basic check) ----------
    print("\n[sosfilt] smoke test...")
    try:
        sos = shim_signal.butter(2, 10, 'lowpass', fs=80, output='sos')
        x = np.random.randn(100)
        y, zf = shim_signal.sosfilt(sos, x)
        if len(y) == len(x):
            print(f"  PASS  output length matches input ({len(y)})")
            tests_passed += 1
        else:
            print(f"  FAIL  output length mismatch")
            tests_failed += 1
    except Exception as e:
        print(f"  ERROR: {e}")
        tests_failed += 1
    
    # ---------- summary ----------
    print("\n" + "=" * 60)
    print(f"Results: {tests_passed} passed, {tests_failed} failed")
    print("=" * 60)
    
    return 0 if tests_failed == 0 else 1

if __name__ == "__main__":
    sys.exit(run_tests())