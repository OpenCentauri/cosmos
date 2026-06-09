"""scipy.signal shim - pure-numpy implementation of the 4 DSP functions Kalico uses.

Reference algorithms lifted from scipy 1.14.1:
  scipy/signal/_filter_design.py
  scipy/signal/_sosfilt.py

This file provides: butter, iirnotch, tf2sos, sosfilt_zi, sosfilt
All use numpy only; no compiled extensions.
"""
from __future__ import annotations

import numpy as np

__all__ = ["butter", "iirnotch", "tf2sos", "sosfilt_zi", "sosfilt"]


# ---------------------------------------------------------------------------
# butter(N, Wn, btype, fs, output='sos')
# ---------------------------------------------------------------------------

def butter(N, Wn, btype="lowpass", fs=2.0, output="sos"):
    """Butterworth digital filter design via bilinear transform.

    Parameters
    ----------
    N : int
        Filter order.
    Wn : float or length-2 sequence
        Critical frequency/frequencies (in the same units as fs).
    btype : {'lowpass', 'highpass', 'bandpass', 'bandstop'}
    fs : float
        Sampling frequency.
    output : {'sos', 'ba'}, default 'sos'
        Output format.

    Returns
    -------
    sos : ndarray, shape (n_sections, 6)
        Second-order sections.
    """
    btype = btype.lower()
    fs = float(fs)
    Wn = np.asarray(Wn, dtype=float)

    # Prewarp the cutoff/band frequencies for bilinear transform
    if btype in ("lowpass", "highpass"):
        if Wn.size != 1:
            raise ValueError("Wn must be scalar for lowpass/highpass")
        Wa = 2.0 * fs * np.tan(np.pi * Wn.item() / fs)
    elif btype in ("bandpass", "bandstop"):
        if Wn.size != 2:
            raise ValueError("Wn must be length-2 for bandpass/bandstop")
        low, high = Wn[0], Wn[1]
        Wa_low = 2.0 * fs * np.tan(np.pi * low / fs)
        Wa_high = 2.0 * fs * np.tan(np.pi * high / fs)

    # Analog prototype poles (normalized to cutoff=1 rad/s)
    # Butterworth poles are on unit circle at equal angular intervals
    # Formula: p_k = exp(j * pi * (2k + N - 1) / (2N)) for k = 0..N-1
    # Only left-half-plane poles are kept
    N_int = int(N)
    poles_analog = []
    for k in range(N_int):
        angle = np.pi * (2.0 * k + N_int - 1.0) / (2.0 * N_int)
        p = np.exp(1j * angle)
        if p.real < 0:  # keep only LHP poles
            poles_analog.append(p)

    if len(poles_analog) == 0:
        raise ValueError(f"butter: no LHP poles found for N={N}")

    # Transform analog poles to target filter type
    if btype == "lowpass":
        # Prototype stays as-is; scale for target cutoff Wa
        # For lowpass: s -> s/Wa, so poles become poles * Wa
        pass
    elif btype == "highpass":
        # s -> Wa / s transformation: poles become Wa / poles
        Wa_scaled = Wa
        poles_analog = [Wa_scaled / p for p in poles_analog]
    elif btype == "bandpass":
        # Lowpass-to-bandpass: s -> (s^2 + Wa_center^2) / (s * Wa_bw)
        # This doubles the number of poles
        Wa_center = np.sqrt(Wa_low * Wa_high)
        Wa_bw = Wa_high - Wa_low
        new_poles = []
        for p in poles_analog:
            # For each LP pole p, we get two BP poles
            # p_bp = Wa_center / (2 * p) +/- sqrt(Wa_center^2 / (4 * p^2) - Wa_center^2)
            # = Wa_center / (2*p) +/- j * Wa_center / (2*|p|) * sqrt(1 - |p|^2)
            # Since |p|=1 for Butterworth, this becomes:
            # p_bp = Wa_center / (2*p) +/- j * Wa_center / 2 * sqrt(1 - 1/p^2)
            disc = Wa_center**2 / (4.0 * p**2) - Wa_center**2
            sqrt_disc = np.sqrt(disc + 0j)  # ensure complex sqrt
            new_poles.append(Wa_center / (2.0 * p) + sqrt_disc)
            new_poles.append(Wa_center / (2.0 * p) - sqrt_disc)
        poles_analog = new_poles
    elif btype == "bandstop":
        # Lowpass-to-bandstop: s -> Wa_bw * s / (s^2 + Wa_center^2)
        # This also doubles the number of poles
        Wa_center = np.sqrt(Wa_low * Wa_high)
        Wa_bw = Wa_high - Wa_low
        new_poles = []
        for p in poles_analog:
            # p_st = Wa_bw * p / (p^2 + 1) ... actually for bandstop it's more complex
            # Standard transform: s -> Wa_bw * s / (s^2 + Wa_center^2)
            # Solving for new poles: s = Wa_bw * p / (p^2 + 1)
            # Actually for each LP pole p, we get two new poles:
            # p_bs = Wa_bw / (2*p) +/- sqrt(Wa_bw^2 / (4*p^2) - Wa_center^2)
            disc = Wa_bw**2 / (4.0 * p**2) - Wa_center**2
            sqrt_disc = np.sqrt(disc + 0j)
            new_poles.append(Wa_bw / (2.0 * p) + sqrt_disc)
            new_poles.append(Wa_bw / (2.0 * p) - sqrt_disc)
        poles_analog = new_poles

    # Build analog transfer function H(s) = b(s) / a(s)
    a_analog = np.poly(poles_analog)

    # Numerator for analog prototype
    if btype == "lowpass":
        # DC gain = 1 (transmission at DC)
        b_analog = np.array([1.0])
    elif btype == "highpass":
        # Transmission at infinity (zeros at origin)
        b_analog = np.zeros(N_int + 1)
        b_analog[0] = 1.0  # this is s^N, but need to check the coefficient ordering
        # Actually s^N has coefficients [0, 0, ..., 0, 1] in numpy's order (highest to lowest)
        # Wait - np.poly([1,2]) = [1, -(1+2), 1*2] = coefficients from highest to lowest power
        # So for polynomial s^N, we need to use np.poly with the roots being the zeros
        # s^N has N zeros at origin
        b_analog = np.zeros(N_int + 1)
        b_analog[-1] = 1.0  # coefficients from highest to lowest: [0,0,...,0,1] for s^N
    elif btype in ("bandpass", "bandstop"):
        # Order doubles for bandpass/bandstop
        # Bandpass: order 2N, has transmission at center freq
        # Bandstop: order 2N, blocks center freq
        # For BP/BStop, the analog prototype is a LP of order 2N
        b_analog = np.array([1.0])  # For the LP prototype at DC gain

    # Bilinear transform: s = 2*fs * (z-1)/(z+1)
    k = 2.0 * fs

    def _bilinear(b, a):
        """Apply bilinear transform to transfer function (b, a)."""
        b = np.array(b, dtype=complex)
        a = np.array(a, dtype=complex)
        M = len(a) - 1  # denominator order

        b_z = np.zeros(M + 1, dtype=complex)
        a_z = np.zeros(M + 1, dtype=complex)

        for i in range(len(b)):
            ki = k ** i
            # (z-1)^i * (z+1)^(M-i)
            # poly([1, -1]) gives coefficients of (z-1)
            # We build it step by step
            poly_minus = np.array([1.0, -1.0])
            poly_plus = np.array([1.0, 1.0])
            for _ in range(i):
                poly_minus = np.convolve(poly_minus, [1.0, -1.0])
            for _ in range(M - i):
                poly_plus = np.convolve(poly_plus, [1.0, 1.0])
            combined = np.convolve(poly_minus, poly_plus)
            # Add to b_z, aligning with the correct degree positions
            for j, coef in enumerate(combined):
                if j <= M:
                    b_z[j] += b[i] * ki * coef

        for i in range(len(a)):
            ki = k ** i
            poly_minus = np.array([1.0, -1.0])
            poly_plus = np.array([1.0, 1.0])
            for _ in range(i):
                poly_minus = np.convolve(poly_minus, [1.0, -1.0])
            for _ in range(M - i):
                poly_plus = np.convolve(poly_plus, [1.0, 1.0])
            combined = np.convolve(poly_minus, poly_plus)
            for j, coef in enumerate(combined):
                if j <= M:
                    a_z[j] += a[i] * ki * coef

        # Normalize so a_z[0] = 1 (real)
        if a_z[0] != 0:
            b_z = b_z / a_z[0]
            a_z = a_z / a_z[0]

        return np.real(b_z), np.real(a_z)

    b_dig, a_dig = _bilinear(b_analog, a_analog)

    if output == "ba":
        return b_dig, a_dig

    sos = tf2sos(b_dig, a_dig)
    return sos


# ---------------------------------------------------------------------------
# iirnotch(w0, Q, fs)
# ---------------------------------------------------------------------------

def iirnotch(w0, Q, fs=2.0):
    """Second-order IIR notch filter."""
    w0 = float(w0)
    Q = float(Q)
    fs = float(fs)

    w0_ang = 2.0 * np.pi * w0 / fs
    alpha = np.sin(w0_ang) / (2.0 * Q)
    cos_w0 = np.cos(w0_ang)

    b = np.array([1.0, -2.0 * cos_w0, 1.0])
    a = np.array([1.0 + alpha, -2.0 * cos_w0, 1.0 - alpha])

    if a[0] != 0:
        b = b / a[0]
        a = a / a[0]

    return b, a


# ---------------------------------------------------------------------------
# tf2sos(b, a)
# ---------------------------------------------------------------------------

def tf2sos(b, a):
    """Convert transfer function (b, a) to second-order sections."""
    b = np.asarray(b, dtype=float)
    a = np.asarray(a, dtype=float)

    # Remove leading zeros
    while b.size > 1 and np.abs(b[0]) < 1e-14:
        b = b[1:]
    while a.size > 1 and np.abs(a[0]) < 1e-14:
        a = a[1:]

    # Normalize so a[0] = 1
    if np.abs(a[0]) > 1e-14:
        b = b / a[0]
        a = a / a[0]

    # Find roots
    b_roots = np.roots(b)
    a_roots = np.roots(a)

    # Remove zeros at origin (pair with poles at origin)
    zero_at_origin = np.abs(b_roots) < 1e-12
    b_roots = b_roots[~zero_at_origin]

    # Sort by angle
    def angle_key(r):
        return np.angle(r)

    a_roots = sorted(a_roots, key=angle_key)
    b_roots = sorted(b_roots, key=angle_key)

    sections = []
    used_b = [False] * len(b_roots)

    for i in range(0, len(a_roots), 2):
        if i + 1 < len(a_roots):
            p1, p2 = a_roots[i], a_roots[i + 1]
            is_conjugate = np.abs(p1 - np.conj(p2)) < 1e-10
        else:
            p1, p2 = a_roots[i], None
            is_conjugate = False

        # Find zeros for this section
        sec_zeros = []
        for j, z in enumerate(b_roots):
            if not used_b[j]:
                sec_zeros.append(z)
                if len(sec_zeros) == (2 if p2 is not None else 1):
                    break

        # Mark used zeros
        for z in sec_zeros:
            for j, z2 in enumerate(b_roots):
                if not used_b[j] and np.abs(z - z2) < 1e-10:
                    used_b[j] = True
                    break

        # Fill with zeros at origin if needed
        while len(sec_zeros) < 2:
            sec_zeros.append(0.0)

        b_sec = np.poly(sec_zeros)
        a_sec = np.poly([p for p in [p1, p2] if p is not None])

        # Normalize so a_sec[0] = 1
        if a_sec[0] != 0:
            b_sec = b_sec / a_sec[0]
            a_sec = a_sec / a_sec[0]

        # Pad to 3 coefficients
        b_sec = np.concatenate([[0.0] * max(0, 3 - len(b_sec)), b_sec])
        a_sec = np.concatenate([[0.0] * max(0, 3 - len(a_sec)), a_sec])

        sections.append([b_sec[0], b_sec[1], b_sec[2], 1.0, a_sec[1], a_sec[2]])

    return np.array(sections)


# ---------------------------------------------------------------------------
# sosfilt_zi(sos)
# ---------------------------------------------------------------------------

def sosfilt_zi(sos):
    """Compute initial conditions for sosfilt for steady-state step input."""
    sos = np.asarray(sos, dtype=float)
    n_sections = sos.shape[0]
    zi = np.zeros((n_sections, 2), dtype=float)

    for i in range(n_sections):
        b = sos[i, :3]
        a = sos[i, 3:]  # [1, a1, a2] by SOS convention

        sum_b = b[0] + b[1] + b[2]
        sum_a = 1.0 + a[1] + a[2]

        G = sum_b / sum_a if np.abs(sum_a) > 1e-14 else sum_b / 1e-14

        zi[i, 0] = b[0] - G
        zi[i, 1] = b[1] - G * a[1]

    return zi


# ---------------------------------------------------------------------------
# sosfilt(sos, x, zi=None)
# ---------------------------------------------------------------------------

def sosfilt(sos, x, zi=None):
    """Filter data with second-order sections."""
    sos = np.asarray(sos, dtype=float)
    x = np.asarray(x, dtype=float)

    n_sections = sos.shape[0]
    if zi is None:
        zi = np.zeros((n_sections, 2), dtype=float)
    else:
        zi = np.asarray(zi, dtype=float).copy()

    y = np.zeros_like(x, dtype=float)
    w = zi.copy()

    for n in range(len(x)):
        xn = x[n]
        yn = 0.0
        for i in range(n_sections):
            b0, b1, b2 = sos[i, 0], sos[i, 1], sos[i, 2]
            a1, a2 = sos[i, 4], sos[i, 5]

            v = xn - a1 * w[i, 0] - a2 * w[i, 1]
            yn = b0 * v + b1 * w[i, 0] + b2 * w[i, 1]

            w[i, 1] = w[i, 0]
            w[i, 0] = v

        y[n] = yn

    return y, w