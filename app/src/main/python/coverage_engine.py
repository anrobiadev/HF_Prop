"""
coverage_engine.py - Signal Footprint Engine v3.0
===================================================
Logica bazata pe MUF real:
  - foF2 calculat la TX (consistent, fara asimetrie artificiala)
  - skip_min = distanta la care banda se "deschide" (MUF(d) = f)
  - d_max = raza max calibrata per banda x p_factor x mode_factor
  - Factor de disponibilitate: scade cand f se apropie de MUF
  - NVIS pentru f < foF2 (160m/80m/40m)
  - K-Index deprimare foF2
  - 36 directii pentru forma circulara corecta
"""

import math
import os
import sys
from datetime import datetime, timezone

_dir = os.path.dirname(os.path.abspath(__file__))
if _dir not in sys.path:
    sys.path.insert(0, _dir)

_ve = None
def _get_ve():
    global _ve
    if _ve is not None:
        return _ve
    try:
        import voacap_engine as ve
        _ve = ve
        return ve
    except Exception as e:
        print(f"[COVERAGE] voacap_engine import failed: {e}")
        return None

# ─────────────────────────────────────────────────────────────────────────────
BANDS = [
    {"name":"160m","freq":1.9, "color":"#910909","d_ref":  400},
    {"name":"80m", "freq":3.5, "color":"#B71C1C","d_ref":  800},
    {"name":"40m", "freq":7.0, "color":"#E64A19","d_ref": 3000},
    {"name":"30m", "freq":10.1,"color":"#FBC02D","d_ref": 4500},
    {"name":"20m", "freq":14.0,"color":"#43A047","d_ref": 6000},
    {"name":"17m", "freq":18.1,"color":"#1E88E5","d_ref": 7000},
    {"name":"15m", "freq":21.0,"color":"#3949AB","d_ref": 8000},
    {"name":"12m", "freq":24.9,"color":"#8E24AA","d_ref": 9000},
    {"name":"10m", "freq":28.0,"color":"#00ACC1","d_ref":11000},
]

N_BEARINGS = 72  # curbe mai fine

# ─────────────────────────────────────────────────────────────────────────────

def _dest(lat, lon, bearing_deg, dist_km):
    R = 6371.0
    d = dist_km / R
    b = math.radians(bearing_deg)
    p1 = math.radians(lat)
    l1 = math.radians(lon)
    p2 = math.asin(math.sin(p1)*math.cos(d) + math.cos(p1)*math.sin(d)*math.cos(b))
    l2 = l1 + math.atan2(math.sin(b)*math.sin(d)*math.cos(p1),
                          math.cos(d) - math.sin(p1)*math.sin(p2))
    return math.degrees(p2), math.degrees(l2)

def _band_geometry(freq, foF2_k, m3000, d_ref, p_factor, mode_factor, k):
    """
    Calculeaza skip_min si d_max pentru o banda.
    Returneaza (skip_min_km, outer_km, is_nvis, is_closed).
    """
    foF2_raw = foF2_k / max(0.4, 1.0 - k * 0.06)

    # NVIS: f < foF2 * 0.95
    if freq <= foF2_raw * 0.95:
        nvis_reach = min(600.0, (foF2_raw - freq) * 130.0 + 200.0)
        nvis_reach *= p_factor * mode_factor
        return 0.0, max(80.0, nvis_reach), True, False

    # Banda inchisa
    muf_max = foF2_k * m3000
    if freq > muf_max:
        return 9999.0, 0.0, False, True

    # Skip minima din geometria MUF
    denom = foF2_k * m3000 - foF2_k
    if denom <= 0:
        return 9999.0, 0.0, False, True

    skip_min = max(0.0, (freq - foF2_k) / denom * 3000.0)

    # Factor de disponibilitate: cat de departe e f de MUF
    # La f=FOT(0.85*MUF) -> availability=1.0
    # La f=MUF -> availability=0
    muf_ratio = freq / muf_max  # 0..1
    if muf_ratio < 0.7:
        availability = 1.0
    elif muf_ratio < 0.85:
        availability = 1.0 - (muf_ratio - 0.7) / 0.15 * 0.3
    else:
        availability = 0.7 * (1.0 - muf_ratio) / 0.15
        availability = max(0.1, availability)

    # Raza maxima
    outer = d_ref * p_factor * mode_factor * availability
    outer = max(outer, skip_min * 1.5)
    outer = min(outer, 16000.0)

    return skip_min, outer, False, False

# ─────────────────────────────────────────────────────────────────────────────

def get_area_coverage(tx_lat, tx_lon, ssn, sfi, k_index, power, mode):
    tx_lat  = float(tx_lat)
    tx_lon  = float(tx_lon)
    ssn     = float(ssn)
    sfi     = float(sfi)
    k_index = float(k_index)
    power   = float(power)

    ssn_eff = ssn + max(0.0, (sfi - 70.0) * 0.5)

    now   = datetime.now(timezone.utc)
    hour  = now.hour
    month = now.month
    doy   = now.timetuple().tm_yday

    p_factor    = math.log10(max(1.1, power)) / 2.0
    mode_factor = {"FT8":1.35,"CW":1.15,"SSB":1.0,"AM":0.85}.get(mode, 1.0)

    ve = _get_ve()

    # foF2 si M3000 la TX — evaluati o singura data, consistent pt toate directiile
    if ve:
        try:
            foF2  = ve.ccir_foF2(tx_lat, tx_lon, hour, month, ssn_eff, doy)
            m3000 = ve.ccir_M3000(tx_lat, tx_lon, hour, month, ssn_eff)
        except:
            foF2, m3000 = 7.0, 3.2
    else:
        ion   = ssn_eff / 20.0
        foF2  = max(3.0, 5.0 + ion)
        m3000 = 3.2

    # K-Index: deprimare foF2
    foF2_k = foF2 * max(0.4, 1.0 - k_index * 0.06)

    result = []
    for b in BANDS:
        f     = b["freq"]
        d_ref = b["d_ref"]

        skip_min, outer_km, is_nvis, is_closed = _band_geometry(
            f, foF2_k, m3000, d_ref, p_factor, mode_factor, k_index)

        outer_pts = []
        inner_pts = []

        for i in range(N_BEARINGS):
            bearing = i * (360.0 / N_BEARINGS)

            if is_closed:
                # Banda inchisa: groundwave minimal
                pt = _dest(tx_lat, tx_lon, bearing, 80.0 * p_factor)
                outer_pts.append([pt[0], pt[1]])
            else:
                # Mic jitter realist (2%) pentru a evita cercuri perfecte
                jitter = 1.0 + 0.02 * math.sin(math.radians(bearing * 3 + f * 10))
                outer_d = max(50.0, outer_km * jitter)
                pt_out = _dest(tx_lat, tx_lon, bearing, outer_d)
                outer_pts.append([pt_out[0], pt_out[1]])

                # Skip zone interioara
                if not is_nvis and skip_min > 100:
                    inner_d = skip_min * 0.90 * jitter
                    pt_in = _dest(tx_lat, tx_lon, bearing, inner_d)
                    inner_pts.append([pt_in[0], pt_in[1]])

        result.append({
            "band":         b["name"],
            "color":        b["color"],
            "points":       outer_pts,
            "inner_points": inner_pts,
            "is_nvis":      is_nvis,
        })

    return result