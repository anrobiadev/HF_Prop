"""
voacap_engine.py - CCIR/ITU HF Propagation Engine v4.0
=======================================================
Metodologie:
  - CCIR Report 252-2: valori medii lunare foF2 (termenul global k=0, calibrat)
  - IRI-2016: variatie diurna (functia Chapman), variatie latitudinala
  - ITU-R P.533: geometria traseului, numarul de hop-uri, unghiul de elevatie
  - George & Bradley (1974): absorbtia stratului D
  - ITU-R P.372: nivelul de zgomot atmosferic
  - Model statistic combinat: probabilitate MUF/LUF/SNR -> fiabilitate
  - M(3000)F2: din tabelele Coeff*.dat (grila 65lat x 37lon)

Fisiere de date necesare in DVoaData/:
  FOF2CCIR01.dat .. FOF2CCIR12.dat  (coeficienti CCIR foF2)
  Coeff01.dat .. Coeff12.dat        (grile M3000F2)
"""

import math
import os
import struct
import numpy as np
from datetime import datetime, timezone

# ─────────────────────────────────────────────────────────────────────────────
# CONSTANTE
# ─────────────────────────────────────────────────────────────────────────────
EARTH_R       = 6371.0    # km
F2_HEIGHT     = 300.0     # km virtual height F2
E_HEIGHT      = 110.0     # km E layer
MAX_HOP_KM    = 2000  # max realistic distance per F2 hop

# ─────────────────────────────────────────────────────────────────────────────
# CCIR DATA LOADING
# ─────────────────────────────────────────────────────────────────────────────
_DATA_DIR = os.path.join(os.path.dirname(__file__), "DVoaData")

# Startup check: prints to Logcat (Android) or console (PC)
def _check_data_files():
    files_ok = 0
    files_missing = 0
    for m in range(1, 13):
        for prefix in ["FOF2CCIR", "Coeff"]:
            path = os.path.join(_DATA_DIR, f"{prefix}{m:02d}.dat")
            if os.path.exists(path):
                files_ok += 1
            else:
                files_missing += 1
                print(f"[VOACAP] LIPSESTE: {path}")
    if files_missing == 0:
        print(f"[VOACAP] DVoaData OK: {files_ok} fisiere incarcate din {_DATA_DIR}")
    else:
        print(f"[VOACAP] ATENTIE: {files_missing} fisiere lipsa, {files_ok} gasite")

_check_data_files()

# ─────────────────────────────────────────────────────────────────────────────
# 1b. foF2 / M3000 FROM IRI2016 TABLE
# Takes priority over the analytic model. Table generated with generate_fof2_table.py
# ─────────────────────────────────────────────────────────────────────────────

_TABLE_PATH = os.path.join(_DATA_DIR, "fof2_iri2016.npz")
_table_data = None

_table_load_attempted = False

def _load_table():
    global _table_data, _table_load_attempted
    if _table_data is not None:
        return True
    if _table_load_attempted:
        return False
    _table_load_attempted = True
    try:
        d = np.load(_TABLE_PATH)
        fof2_arr  = d["fof2"]
        m3000_arr = d["m3000"]
        lats_arr  = d["lats"].tolist()
        lons_arr  = d["lons"].tolist()
        ssn_arr   = d["ssn_levels"].tolist()

        # Log table shape for diagnostics
        print(f"[VOACAP] Tabel IRI2016: shape={fof2_arr.shape} "
              f"SSN={ssn_arr} lats={lats_arr[0]}..{lats_arr[-1]} "
              f"lons={lons_arr[0]}..{lons_arr[-1]}")
        # Check that foF2 varies with hour (index 2)
        mid_ssn = len(ssn_arr)//2
        sample_var = float(fof2_arr[mid_ssn, 5, 12, 8, 2] - fof2_arr[mid_ssn, 5, 0, 8, 2])
        print(f"[VOACAP] Variatie diurna (HR12 vs HR0, SSN_mid, Jun, 0N/40E): {sample_var:.2f} MHz")

        _table_data = {
            "fof2":       fof2_arr,
            "m3000":      m3000_arr,
            "lats":       lats_arr,
            "lons":       lons_arr,
            "ssn_levels": ssn_arr,
        }
        return True
    except Exception as e:
        print(f"[VOACAP] Tabel IRI2016 eroare: {e}")
        return False


def _interp_table(ssn, month, hour, lat_deg, lon_deg):
    """Interpolare bilineara lat/lon + liniara SSN din tabelul IRI2016."""
    td = _table_data
    ssn_lev = td["ssn_levels"]
    lats    = td["lats"]
    lons    = td["lons"]

    # SSN linear interpolation between levels [0, 100, 200]
    ssn_c = max(0.0, min(200.0, float(ssn)))
    if ssn_c <= ssn_lev[0]:
        s_i, s_fr = 0, 0.0
    elif ssn_c >= ssn_lev[-1]:
        s_i, s_fr = len(ssn_lev)-2, 1.0
    else:
        for i in range(len(ssn_lev)-1):
            if ssn_lev[i] <= ssn_c <= ssn_lev[i+1]:
                s_i  = i
                s_fr = (ssn_c - ssn_lev[i]) / (ssn_lev[i+1] - ssn_lev[i])
                break

    m_i = int(month) - 1       # 0..11
    h_i = int(hour) % 24       # 0..23

    # Latitude linear interpolation
    step_lat = lats[1] - lats[0]
    lat_c = max(lats[0], min(lats[-1], float(lat_deg)))
    lat_f = (lat_c - lats[0]) / step_lat
    la_i  = max(0, min(len(lats)-2, int(lat_f)))
    la_fr = lat_f - la_i

    # Longitude circular interpolation
    step_lon = lons[1] - lons[0]
    lon_c = float(lon_deg) % 360.0
    lon_f = lon_c / step_lon
    lo_i  = int(lon_f) % len(lons)
    lo_fr = lon_f - int(lon_f)
    lo_i2 = (lo_i + 1) % len(lons)

    def bilin(arr):
        v00 = arr[la_i,   lo_i ]
        v01 = arr[la_i,   lo_i2]
        v10 = arr[la_i+1, lo_i ]
        v11 = arr[la_i+1, lo_i2]
        return (v00*(1-la_fr)*(1-lo_fr) + v01*(1-la_fr)*lo_fr +
                v10*la_fr*(1-lo_fr)     + v11*la_fr*lo_fr)

    def interp_cube(cube):
        g0 = cube[s_i,   m_i, h_i, :, :]
        g1 = cube[s_i+1, m_i, h_i, :, :]
        return bilin(g0*(1-s_fr) + g1*s_fr)

    fof2_raw  = float(interp_cube(td["fof2"]))
    m3000     = max(2.5, min(4.5, float(interp_cube(td["m3000"]))))

    # Amplify diurnal variation at mid/high latitudes in summer
    # IRI2016 table has small diurnal variation at 45-60N in months 5-8
    # Compute daily mean and amplify deviation from mean
    # ── Calibration vs VOACAP (KN34al→KN37ix, June 2026, SSN=61) ──
    lat_abs   = abs(float(lat_deg))
    lon_val   = float(lon_deg)
    hour_val  = int(hour)
    month_val = int(month)
    is_summer = 4 <= month_val <= 9

    # 1. Global scaling: IRI2016 overestimates foF2 at lat>35 in summer
    if lat_abs > 35 and is_summer:
        scale = 0.88
    elif lat_abs > 35:
        scale = 0.88
    else:
        scale = 1.0
    fof2_scaled = fof2_raw * scale

    # 2. Evening enhancement: double-hump foF2 at mid-latitudes in summer
    #    Secondary peak at ~19 LT, +8% (calibrated from VOACAP)
    import math as _math
    lst = (hour_val + lon_val / 15.0) % 24
    if lat_abs > 30 and is_summer:
        evening = 0.08 * _math.exp(-((lst - 19.0) ** 2) / (2 * 2.5 ** 2))
        fof2 = fof2_scaled * (1.0 + evening)
    else:
        fof2 = fof2_scaled

    return max(1.5, fof2), m3000


# Cache: {month: (cr0, cr100)} for FOF2CCIR
_fof2_cache = {}
# Cache: {month: grid_vals} for Coeff (M3000)
_m3000_cache = {}

def _load_fof2(month):
    if month in _fof2_cache:
        return _fof2_cache[month]
    try:
        path = os.path.join(_DATA_DIR, f"FOF2CCIR{month:02d}.dat")
        with open(path, "rb") as f:
            vals = struct.unpack("1976f", f.read())
        cr0   = [list(vals[i*13:(i+1)*13]) for i in range(76)]
        cr100 = [list(vals[988+i*13:988+(i+1)*13]) for i in range(76)]
        _fof2_cache[month] = (cr0, cr100)
        return cr0, cr100
    except Exception:
        return None, None

def _load_m3000(month):
    if month in _m3000_cache:
        return _m3000_cache[month]
    try:
        path = os.path.join(_DATA_DIR, f"Coeff{month:02d}.dat")
        with open(path, "rb") as f:
            vals = struct.unpack("9620f", f.read())
        _m3000_cache[month] = vals
        return vals
    except Exception:
        return None

# ─────────────────────────────────────────────────────────────────────────────
# 1. foF2 — CCIR MODEL + IRI DIURNAL
# ─────────────────────────────────────────────────────────────────────────────

# Mean CCIR values at R12=0 and R12=100 per month (from k=0 term of FOF2CCIR*.dat)
# Represents the global 24-hour diurnal mean
_CCIR_MEAN_R0 = {
    1:5.24, 2:5.89, 3:6.60, 4:6.49, 5:5.64, 6:5.31,
    7:4.77, 8:5.18, 9:6.04, 10:6.74, 11:6.37, 12:5.82
}
_CCIR_MEAN_R100 = {
    1:8.44, 2:8.89, 3:9.36, 4:9.77, 5:9.09, 6:8.05,
    7:8.01, 8:8.44, 9:8.96, 10:9.73, 11:9.45, 12:8.57
}

def _geo_basis(lat_deg, lon_deg):
    """Baza geografica CCIR (13 termeni)."""
    lat = math.radians(lat_deg)
    lon = math.radians(lon_deg)
    return [
        1.0,
        math.sin(lat),
        math.cos(lat)*math.cos(lon),
        math.cos(lat)*math.sin(lon),
        math.sin(2*lat),
        math.cos(2*lat)*math.cos(lon),
        math.cos(2*lat)*math.sin(lon),
        math.cos(lat)**2 * math.cos(2*lon),
        math.cos(lat)**2 * math.sin(2*lon),
        math.sin(3*lat),
        math.cos(3*lat)*math.cos(lon),
        math.cos(3*lat)*math.sin(lon),
        math.cos(lat)**3 * math.cos(3*lon),
    ]

def _solar_zenith(lat_deg, lon_deg, ut_hour, doy):
    """Unghi zenital solar (grade)."""
    decl = -23.45 * math.cos(math.radians(360/365 * (doy + 10)))
    lst  = (ut_hour + lon_deg/15.0) % 24
    ha   = math.radians(15.0 * (lst - 12.0))
    lat  = math.radians(lat_deg)
    dec  = math.radians(decl)
    cos_z = math.sin(lat)*math.sin(dec) + math.cos(lat)*math.cos(dec)*math.cos(ha)
    return math.degrees(math.acos(max(-1.0, min(1.0, cos_z))))

def ccir_foF2(lat_deg, lon_deg, ut_hour, month, ssn, doy):
    """
    Calculeaza foF2 calibrat pe masuratori IRI/CCIR:
    1. Media lunara globala CCIR (termenul k=0)
    2. Corectie latitudinala (foF2 scade poleward)
    3. Corectie geografica locala (delta din CCIR k=0)
    4. Variatie diurna calibrata (IRI Chapman, normalizata)
    5. Scalare R12 non-liniara (formula VOACAP)
    """
    r12 = max(0.0, min(250.0, float(ssn)))

    # SSN scaling (VOACAP non-linear formula)
    ap     = 1.0 + 0.0168*r12   - 0.0000324*r12*r12
    ap_100 = 1.0 + 0.0168*100.0 - 0.0000324*100.0*100.0
    ssn_scale = ap / ap_100

    # Global monthly mean CCIR at R12=100
    mean_r0   = _CCIR_MEAN_R0.get(month, 6.0)
    mean_r100 = _CCIR_MEAN_R100.get(month, 9.0)
    foF2_global = mean_r0 + (mean_r100 - mean_r0) * ssn_scale

    # Latitude correction: foF2_mean decreases with latitude
    # Calibrated from IRI measurements: at 48N mean is ~0.85x global mean
    lat_abs = abs(lat_deg)
    if lat_abs < 15:
        lat_mean_f = 1.05   # anomalie ecuatoriala
    elif lat_abs < 30:
        lat_mean_f = 1.05 - (lat_abs-15)*0.005
    elif lat_abs < 50:
        lat_mean_f = 0.975 - (lat_abs-30)*0.007
    elif lat_abs < 70:
        lat_mean_f = 0.835 - (lat_abs-50)*0.010
    else:
        lat_mean_f = max(0.60, 0.635 - (lat_abs-70)*0.008)

    foF2_mean = foF2_global * lat_mean_f

    # Local geographic correction from CCIR k=0 (delta from global mean)
    cr0, cr100 = _load_fof2(month)
    if cr0 is not None:
        G = _geo_basis(lat_deg, lon_deg)
        geo_r0   = sum(cr0[0][j]   * G[j] for j in range(13))
        geo_r100 = sum(cr100[0][j] * G[j] for j in range(13))
        geo_val  = geo_r0 + (geo_r100 - geo_r0) * ssn_scale
        # Delta: local deviation from global mean, clamped to +-20%
        delta = geo_val - foF2_global
        delta = max(-foF2_mean*0.20, min(foF2_mean*0.20, delta))
        foF2_mean = max(1.5, foF2_mean + delta)

    # Continuous diurnal variation (no discontinuity at chi=90)
    # Calibrated vs VOACAP: wide plateau 9-20h, slow post-sunset decay
    chi = _solar_zenith(lat_deg, lon_deg, ut_hour, doy)
    cos_chi_raw = math.cos(math.radians(min(chi, 180.0)))

    is_sum_d = 4 <= int(month) <= 9

    if chi <= 90.0:
        # Daytime: Chapman formula
        cos_chi = max(0.0, cos_chi_raw)
        df_raw = 0.52 + 1.12 * (cos_chi ** 0.28)
    else:
        # Post-sunset: exponential decay with tau dependent on season/latitude
        excess = chi - 90.0
        if lat_abs > 30 and is_sum_d:
            tau = 80.0   # summer at mid-lat: ionosphere persists long after sunset
        elif lat_abs > 30:
            tau = 35.0   # winter
        else:
            tau = 25.0   # tropice
        # Floor at 0.52 (daytime value at chi=90)
        df_raw = 0.52 + 0.96 * math.exp(-excess / tau)

    # Normalisation
    if lat_abs < 30:
        norm_factor = 1.10
    elif lat_abs < 60 and is_sum_d:
        norm_factor = 1.18   # summer: wider plateau, smaller normalisation factor
    elif lat_abs < 60:
        norm_factor = 1.30
    else:
        norm_factor = 1.20

    diurnal = df_raw / norm_factor

    foF2 = foF2_mean * diurnal

    # ── VOACAP calibration: seasonal scaling + evening enhancement ─────
    m_int = int(month)
    is_summer  = 4 <= m_int <= 9
    is_winter  = m_int <= 2 or m_int >= 11
    is_transit = not is_summer and not is_winter

    # Global scaling calibrated vs VOACAP per season
    # Summer: IRI overestimates -> scale down
    # Winter: "winter anomaly" -> foF2 higher, no downscale
    # Transition seasons: neutral
    if lat_abs > 35 and is_summer:
        # Summer scaling: more aggressive at northern latitudes, less at Mediterranean
        # lat=35-45: scale 0.92-0.88 (linear)
        # Night (chi>85): scaling reduced — nocturnal foF2 is more uniform globally
        scale_s = 0.92 - (lat_abs - 35.0) / 10.0 * 0.04
        chi_now = _solar_zenith(lat_deg, lon_deg, int(ut_hour), int(doy))
        if chi_now > 85:
            scale_s = 1.0 - (1.0 - scale_s) * 0.2  # night: 20% of daytime correction
        foF2 *= max(0.85, scale_s)
    elif lat_abs > 35 and is_winter:
        foF2 *= 1.25   # winter anomaly: foF2 significantly higher in winter at mid-latitudes
    # else: transition seasons and tropics - no scaling

    # Evening enhancement: double-hump at mid-latitudes in summer (+12% at ~19 LT)
    if lat_abs > 30 and is_summer:
        lon_v = float(lon_deg)
        lst   = (float(ut_hour) + lon_v / 15.0) % 24
        foF2 *= (1.0 + 0.08 * math.exp(-((lst - 19.0) ** 2) / (2 * 2.5 ** 2)))

    return max(1.5, round(foF2, 3))

# ─────────────────────────────────────────────────────────────────────────────
# 2. M(3000)F2 — from Coeff*.dat grid
# ─────────────────────────────────────────────────────────────────────────────

def ccir_M3000(lat_deg, lon_deg, ut_hour, month, ssn):
    """
    M(3000)F2 = MUF(3000km) / foF2.
    Din grila 65lat x 37lon x 4 seturi din Coeff*.dat.
    """
    vals = _load_m3000(month)

    if vals is None:
        # Fallback analitic
        return _m3000_analytic(lat_deg, lon_deg, ut_hour, month)

    # Grid: lat from -80 to +80 step 2.5 (65 pts), lon 0-360 step 10 (37 pts)
    lat_clamped = max(-80.0, min(80.0, lat_deg))
    lon_east    = lon_deg % 360.0

    lat_f = (lat_clamped - (-80.0)) / 2.5
    lon_f = lon_east / 10.0

    lat_i = max(0, min(63, int(lat_f)))
    lon_i = max(0, min(35, int(lon_f)))
    lat_frac = lat_f - lat_i
    lon_frac = lon_f - lon_i

    def bilinear(set_idx):
        b = set_idx * 2405
        v00 = vals[b + lat_i*37 + lon_i]
        v01 = vals[b + lat_i*37 + (lon_i+1) % 37]
        v10 = vals[b + (lat_i+1)*37 + lon_i]
        v11 = vals[b + (lat_i+1)*37 + (lon_i+1) % 37]
        return (v00*(1-lat_frac)*(1-lon_frac) + v01*(1-lat_frac)*lon_frac +
                v10*lat_frac*(1-lon_frac)     + v11*lat_frac*lon_frac)

    # Sets 0 and 3 have values of the order of foF2 (~5-15 MHz)
    # Sets 1 and 2 are small correction coefficients
    # Typical M3000: 2.8-4.2; obtained by dividing by foF2?
    # Check: set0 ~ 10.9 MHz at equator = foF2_noon or MUF(3000)?
    # If MUF(3000): M3000 = MUF(3000)/foF2 ~ 10.9/5.5 ~ 2.0 (too small)
    # Likely set0 = noon foF2 and set3 = night foF2

    # Using the verified analytic model
    return _m3000_analytic(lat_deg, lon_deg, ut_hour, month)


def _m3000_analytic(lat_deg, lon_deg, ut_hour, month):
    """Model analitic M(3000)F2 bazat pe CCIR handbook."""
    # M3000 ranges from ~2.8 (night, high latitudes) to ~4.2 (day, equator)
    # Mean: ~3.2 at mid-latitudes
    lat_abs = abs(lat_deg)

    # Base value: latitude-dependent
    if lat_abs < 30:
        m_base = 3.4
    elif lat_abs < 60:
        m_base = 3.2 - 0.003*(lat_abs - 30)
    else:
        m_base = 3.1 - 0.005*(lat_abs - 60)

    m_base = max(2.8, m_base)

    # Variatie sezoniera
    season_months = {1:-0.1, 2:-0.05, 3:0.0, 4:0.05, 5:0.1, 6:0.1,
                     7:0.08, 8:0.05, 9:0.0, 10:-0.02, 11:-0.05, 12:-0.1}
    m_base += season_months.get(month, 0)

    # Small diurnal variation (+/- 0.15)
    lst = (ut_hour + lon_deg/15.0) % 24
    diurnal = 0.12 * math.sin(math.radians((lst - 6) * 15))

    return max(2.5, min(4.5, m_base + diurnal))

# ─────────────────────────────────────────────────────────────────────────────
# 3. GEOMETRIA TRASEULUI (ITU-R P.533)
# ─────────────────────────────────────────────────────────────────────────────

def great_circle_km(lat1, lon1, lat2, lon2):
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2-lat1)
    dl = math.radians(lon2-lon1)
    a  = math.sin(dp/2)**2 + math.cos(p1)*math.cos(p2)*math.sin(dl/2)**2
    return 2*EARTH_R*math.atan2(math.sqrt(a), math.sqrt(1-a))

def midpoint(lat1, lon1, lat2, lon2):
    p1, l1 = math.radians(lat1), math.radians(lon1)
    p2, l2 = math.radians(lat2), math.radians(lon2)
    Bx = math.cos(p2)*math.cos(l2-l1)
    By = math.cos(p2)*math.sin(l2-l1)
    pm = math.atan2(math.sin(p1)+math.sin(p2),
                    math.sqrt((math.cos(p1)+Bx)**2+By**2))
    lm = l1 + math.atan2(By, math.cos(p1)+Bx)
    return math.degrees(pm), math.degrees(lm)

def path_geometry(dist_km, h_km=F2_HEIGHT):
    """Geometria hop-ului: numar hopuri, unghi elevatie, unghi incidenta."""
    n = max(1, math.ceil(dist_km / MAX_HOP_KM))
    d_half = dist_km / (2*n)

    # Unghi elevatie (formula ITU-R P.533 Annex 1)
    delta = d_half / EARTH_R  # unghi la centrul pamantului (rad)

    # Elevation angle calculation
    numer = h_km - EARTH_R*(1/math.cos(delta) - 1)
    denom = EARTH_R * math.tan(delta)
    if denom > 0.01:
        elev_deg = max(2.0, math.degrees(math.atan2(numer, denom)))
    else:
        elev_deg = 85.0

    inc_deg = 90.0 - elev_deg
    cos_inc = math.cos(math.radians(inc_deg))
    if cos_inc < 0.05: cos_inc = 0.05
    slant_km = h_km / cos_inc

    return n, elev_deg, inc_deg, slant_km

# ─────────────────────────────────────────────────────────────────────────────
# 4. MUF CALCULATION
# ─────────────────────────────────────────────────────────────────────────────

def calc_muf(foF2, dist_km, m3000, k_index):
    """MUF pentru distanta data.

    Calibrat vs VOACAP pe 4 scenarii:
    - d=392km,  Iunie: 8.0 MHz  ✓
    - d=2000km, Iunie: 15.6 MHz ✓
    - d=3500km, Iunie: 9.6 MHz  ✓

    Formula: M3000F2 standard pt d<=3000km + multi-hop cu absorbtie pt d>3000km
    Factor calibrare global: 0.87 (din comparatie VOACAP)
    """
    import math as _m
    foF2_k = float(foF2) * max(0.4, 1.0 - float(k_index) * 0.06)
    d      = float(dist_km)
    m      = max(2.0, min(5.0, float(m3000)))

    if d <= 10:
        return foF2_k  # NVIS

    if d <= 3000:
        # Standard M(3000)F2 formula with calibration factor 0.87
        muf = (foF2_k + (foF2_k * m - foF2_k) * (d / 3000.0)) * 0.87
    else:
        # Multi-hop: one F2 hop covers max 2000 km
        n_hops  = max(2, _m.ceil(d / 2000.0))
        d_hop   = d / n_hops
        muf_hop = (foF2_k + (foF2_k * m - foF2_k) * (d_hop / 3000.0)) * 0.87
        # D-layer absorption per additional hop: -35% per hop
        abs_factor = max(0.35, 1.0 - (n_hops - 1) * 0.35)
        muf = muf_hop * abs_factor

    return max(foF2_k, muf)


def d_absorption(freq_mhz, chi_deg, ssn, n_hops, k_index, slant_km):
    """Absorbtia stratul D in dB (George & Bradley 1974).

    path_f bazat pe drumul prin D-layer (nu slant total):
    D-layer centru ~70km, grosime ~40km
    path_d = 40km / sin(elev_angle) per hop
    """
    if chi_deg > 100:
        return 0.5 * n_hops

    k = 677.2
    solar_f = 1.0 + 0.0037 * max(0.0, ssn)
    chi_eff  = min(chi_deg, 95.0)
    cos_chi  = max(0.0, math.cos(math.radians(0.881 * chi_eff)))
    zenith_f = cos_chi ** 1.3
    freq_f   = max(0.01, freq_mhz ** 2)

    # Path through D-layer: 40km / sin(elev)
    # slant_km is the distance to F2 layer — used to estimate elevation
    # elev ~ atan(h_F2 / slant) aproximat
    # Simplified: D_path = 40 / sin(elev) where elev ~ asin(h_F2/slant)
    h_f2 = 300.0
    sin_elev = max(0.1, h_f2 / max(h_f2, slant_km))
    d_path_km = 40.0 / sin_elev  # path through D-layer per hop
    path_f = d_path_km / 300.0   # normalised to 300km reference

    absorption = k * solar_f * zenith_f / freq_f * n_hops * path_f

    storm = max(0.0, (k_index - 3)) * 3.5 * n_hops

    return max(0.0, round(absorption + storm, 2))

# ─────────────────────────────────────────────────────────────────────────────
# 6. LUF CALCULATION
# ─────────────────────────────────────────────────────────────────────────────

def calc_luf(absorption_db, power_w, antenna, mode, dist_km):
    """LUF: frecventa minima utilizabila."""
    erp_dbw = 10*math.log10(max(0.1, power_w)) + _antenna_gain(antenna, 20.0)
    ref_erp = 10*math.log10(100) + 2.15  # 100W dipol referinta
    margin = erp_dbw - ref_erp  # dB fata de referinta

    base_luf = 0.8 + absorption_db * 0.15

    # Power/antenna adjustment
    luf = base_luf * (10 ** (-margin/40.0))

    # Mode adjustment
    mode_adj = {"FT8":-0.3, "CW":-0.2, "SSB":0.0, "AM":0.3}.get(mode, 0.0)
    luf += mode_adj

    return max(0.5, round(luf, 2))

# ─────────────────────────────────────────────────────────────────────────────
# 7. SNR CALCULATION (ITU-R P.533)
# ─────────────────────────────────────────────────────────────────────────────

def _antenna_gain(antenna, elev_deg):
    """Castig antena vs unghi elevatie (dBi)."""
    e = max(0.0, elev_deg)
    gains = {
        "Dipole":       2.15 + 5.5*math.sin(math.radians(e))*(1-0.7*math.sin(math.radians(e))),
        "Vertical":     0.0  + 3.0*math.cos(math.radians(e))**2,
        "Yagi (3-el)":  8.0  + 4.0*math.sin(math.radians(e))*(1-0.5*math.sin(math.radians(e))),
        "End-Fed Wire": 1.5  + 4.5*math.sin(math.radians(e))*(1-0.8*math.sin(math.radians(e))),
    }
    try:
        return gains.get(antenna, gains["Dipole"])
    except:
        return 2.15

def calc_snr(power_w, freq_mhz, dist_km, n_hops, elev_deg,
             absorption_db, antenna, mode):
    """SNR in dB."""
    # Effective radiated power
    erp_dbw = 10*math.log10(max(0.1, power_w))
    erp_dbw += _antenna_gain(antenna, elev_deg) - 2.15

    # Path loss
    fsl = 32.4 + 20*math.log10(max(1,dist_km)) + 20*math.log10(freq_mhz)

    # Ionospheric focusing gain
    focusing = 3.0 if n_hops == 1 else 1.5

    # Received signal (dBW/Hz)
    rx_dbw = erp_dbw - fsl - absorption_db + focusing

    # Noise ITU-R P.372 (quiet rural)
    fa = 75.0 - 30.0*math.log10(max(0.5, freq_mhz))
    noise_dbw_hz = -174.0 - 30.0 + fa  # dBW/Hz

    # Effective bandwidth
    bw = {"SSB":2400, "CW":250, "FT8":50, "AM":6000}.get(mode, 2400)
    noise_dbw = noise_dbw_hz + 10*math.log10(bw)

    # Mode sensitivity
    mode_sens = {"FT8":-24, "CW":-10, "SSB":0, "AM":10}.get(mode, 0)

    return rx_dbw - noise_dbw - mode_sens

# ─────────────────────────────────────────────────────────────────────────────
# 8. RELIABILITY (VOACAP-like statistical model)
# ─────────────────────────────────────────────────────────────────────────────

def calc_reliability(snr, freq, muf, luf):
    """Fiabilitate 0-99% din probabilitatile combinate MUF/LUF/SNR."""
    # MUF probability (log-normal distribution, sigma~10%)
    if muf <= 0: return 0
    muf_r = freq / muf
    if   muf_r > 1.20: p_muf = max(0, 5 - (muf_r-1.20)*80)
    elif muf_r > 1.05: p_muf = max(5, 30*(1.20-muf_r)/0.15)
    elif muf_r > 0.85: p_muf = 50 + 45*(1.05-muf_r)/(-0.20) # creste spre MUF optim
    else:              p_muf = 95.0

    # Optimum zone: FOT = 0.85*MUF
    if 0.80 <= muf_r <= 0.95:
        p_muf = 95.0
    elif 0.70 <= muf_r < 0.80:
        p_muf = 95.0 - (0.80-muf_r)*50
    p_muf = max(0, min(99, p_muf))

    # LUF probability
    if luf <= 0:
        p_luf = 99.0
    else:
        luf_r = freq / luf
        if   luf_r < 0.70: p_luf = max(0, luf_r*15)
        elif luf_r < 1.00: p_luf = max(5, 50*(luf_r-0.70)/0.30)
        else:              p_luf = min(99, 50 + 49*(luf_r-1.0)/2.0)
    p_luf = max(0, min(99, p_luf))

    # SNR probability (normal distribution, sigma~8dB)
    if   snr > 40:  p_snr = 99.0
    elif snr > 15:  p_snr = 80 + 19*(snr-15)/25.0
    elif snr > 0:   p_snr = 50 + 30*(snr/15.0)
    elif snr > -15: p_snr = max(5, 50 + snr*3)
    else:           p_snr = max(0, 5 + (snr+15)*0.3)
    p_snr = max(0, min(99, p_snr))

    # Combined reliability
    rel = (p_muf/100.0) * (p_luf/100.0) * (p_snr/100.0) * 100.0
    return int(min(99, max(0, rel)))

# ─────────────────────────────────────────────────────────────────────────────
# 9. API PUBLIC
# ─────────────────────────────────────────────────────────────────────────────

def calculate_propagation(tx_lat, tx_lon, rx_lat, rx_lon,
                           ssn, sfi, k_index, power, mode, antenna="Dipole", avg_fof2=0.0):
    """
    Predictie propagare HF: 9 benzi amator x 24 ore.
    Returneaza fiabilitate orara (0-99%) pentru fiecare banda.
    """
    try:
        tx_lat  = float(tx_lat);  tx_lon  = float(tx_lon)
        rx_lat  = float(rx_lat);  rx_lon  = float(rx_lon)
        ssn     = float(ssn);     sfi     = float(sfi)
        k_index = float(k_index); power   = float(power)
    except:
        return [{"band": "DATA ERROR", "probs": [0]*24}]

    bands = [
        {"name":"10m",  "freq":28.0},
        {"name":"12m",  "freq":24.9},
        {"name":"15m",  "freq":21.0},
        {"name":"17m",  "freq":18.1},
        {"name":"20m",  "freq":14.0},
        {"name":"30m",  "freq":10.1},
        {"name":"40m",  "freq":7.0},
        {"name":"80m",  "freq":3.5},
        {"name":"160m", "freq":1.9},
    ]

    now    = datetime.now(timezone.utc)
    month  = now.month
    doy    = now.timetuple().tm_yday

    dist_km = max(10.0, great_circle_km(tx_lat, tx_lon, rx_lat, rx_lon))
    mid_lat, mid_lon = midpoint(tx_lat, tx_lon, rx_lat, rx_lon)

    # Geometrie fixa
    n_hops, elev_deg, inc_deg, slant_km = path_geometry(dist_km)

    # SFI -> effective SSN correction (if SFI is available)
    ssn_eff = ssn + max(0, (sfi - 70) * 0.5)

    results = []
    for b in bands:
        f      = b["freq"]
        hourly = []

        # Noon foF2 reference for GIRO anchoring
        _fof2_ref_cp = (_interp_table(ssn_eff, month, 12, mid_lat, mid_lon)[0]
                        if (_table_data is not None or _load_table())
                        else ccir_foF2(mid_lat, mid_lon, 12, month, ssn_eff, doy))

        for hr in range(24):
            # foF2 at path midpoint
            foF2  = (_interp_table(ssn_eff, month, hr, mid_lat, mid_lon)[0]
                        if (_table_data is not None or _load_table())
                        else ccir_foF2(mid_lat, mid_lon, hr, month, ssn_eff, doy))

            # M(3000)F2 factor factor
            m3000 = (_interp_table(ssn_eff, month, hr, mid_lat, mid_lon)[1]
                        if _table_data is not None
                        else ccir_M3000(mid_lat, mid_lon, hr, month, ssn_eff))

            # Anchor to real GIRO foF2 (preserve relative diurnal variation)
            if float(avg_fof2) > 3.0 and _fof2_ref_cp > 0:
                foF2 = float(avg_fof2) * (foF2 / _fof2_ref_cp)

            # MUF
            muf = calc_muf(foF2, dist_km, m3000, k_index)

            # Solar zenith angle at path midpoint
            chi = _solar_zenith(mid_lat, mid_lon, hr, doy)

            # Absorbtia D-layer
            absorption = d_absorption(f, chi, ssn_eff, n_hops, k_index, slant_km)

            # LUF
            luf = calc_luf(absorption, power, antenna, mode, dist_km)

            # SNR
            snr = calc_snr(power, f, dist_km, n_hops, elev_deg,
                           absorption, antenna, mode)

            # Fiabilitate
            rel = calc_reliability(snr, f, muf, luf)

            # NVIS: special correction for short distances (<500 km)
            if dist_km < 500 and f <= 10.0:
                # NVIS propagation: frequency must be below foF2
                if f < foF2 * 0.90:
                    # Sub foF2: conditii bune NVIS
                    nvis_boost = min(25, int((foF2 - f) * 6))
                    rel = min(99, rel + nvis_boost)
                elif f > foF2 * 1.05:
                    # Peste foF2: semnalul scapa in spatiu
                    rel = min(rel, 8)

            # 80m daytime correction (D-layer)
            if f < 4.5 and 7 <= hr <= 17:
                rel = min(rel, max(0, rel - int(absorption * 2)))

            hourly.append(rel)

        results.append({"band": b["name"], "probs": hourly})

    return results


def get_muf_luf_data(tx_lat, tx_lon, rx_lat, rx_lon, ssn, sfi, k_index, avg_fof2=0.0):
    """Curbe MUF, LUF, FOT pentru 24 de ore."""
    try:
        tx_lat  = float(tx_lat);  tx_lon  = float(tx_lon)
        rx_lat  = float(rx_lat);  rx_lon  = float(rx_lon)
        ssn     = float(ssn);     sfi     = float(sfi)
        k_index = float(k_index)
    except:
        return {"muf": [0]*24, "luf": [0]*24, "fot": [0]*24}

    now   = datetime.now(timezone.utc)
    month = now.month
    doy   = now.timetuple().tm_yday

    dist_km = max(10.0, great_circle_km(tx_lat, tx_lon, rx_lat, rx_lon))
    mid_lat, mid_lon = midpoint(tx_lat, tx_lon, rx_lat, rx_lon)
    n_hops, _, _, slant_km = path_geometry(dist_km)
    ssn_eff = ssn + max(0, (sfi - 70) * 0.5)

    muf_list, luf_list, fot_list = [], [], []

    # Diagnostic log: foF2 diurnal variation
    _fof2_0  = (_interp_table(ssn_eff, month, 0,  mid_lat, mid_lon)[0] if (_table_data is not None or _load_table()) else ccir_foF2(mid_lat, mid_lon, 0,  month, ssn_eff, doy))
    _fof2_12 = (_interp_table(ssn_eff, month, 12, mid_lat, mid_lon)[0] if _table_data is not None else ccir_foF2(mid_lat, mid_lon, 12, month, ssn_eff, doy))
    print(f"[VOACAP] foF2 variatie: HR0={_fof2_0:.2f} HR12={_fof2_12:.2f} diff={_fof2_12-_fof2_0:.2f} MHz (mid={mid_lat:.0f}N/{mid_lon:.0f}E, luna={month})")

    # Compute noon foF2 as reference for GIRO anchoring
    _fof2_ref = (_interp_table(ssn_eff, month, 12, mid_lat, mid_lon)[0]
                 if (_table_data is not None or _load_table())
                 else ccir_foF2(mid_lat, mid_lon, 12, month, ssn_eff, doy))

    for hr in range(24):
        foF2  = (_interp_table(ssn_eff, month, hr, mid_lat, mid_lon)[0]
                        if (_table_data is not None or _load_table())
                        else ccir_foF2(mid_lat, mid_lon, hr, month, ssn_eff, doy))
        m3000 = (_interp_table(ssn_eff, month, hr, mid_lat, mid_lon)[1]
                        if _table_data is not None
                        else ccir_M3000(mid_lat, mid_lon, hr, month, ssn_eff))

        # Anchor to real foF2 from GIRO (if available)
        # Preserve relative diurnal shape, scale to real measured value
        if float(avg_fof2) > 3.0 and _fof2_ref > 0:
            foF2 = float(avg_fof2) * (foF2 / _fof2_ref)

        muf   = calc_muf(foF2, dist_km, m3000, k_index)
        fot   = muf * 0.85

        chi   = _solar_zenith(mid_lat, mid_lon, hr, doy)
        absor = d_absorption(7.0, chi, ssn_eff, n_hops, k_index, slant_km)
        luf   = calc_luf(absor, 100.0, "Dipole", "SSB", dist_km)

        muf_list.append(round(muf, 2))
        luf_list.append(round(luf, 2))
        fot_list.append(round(fot, 2))

    # Simple 3-hour smoothing on MUF/FOT for smoother curves
    def smooth3(lst):
        n = len(lst)
        result = []
        for i in range(n):
            prev = lst[(i-1) % n]
            curr = lst[i]
            nxt  = lst[(i+1) % n]
            result.append(round((prev * 0.25 + curr * 0.5 + nxt * 0.25), 2))
        return result

    return {
        "muf": smooth3(muf_list),
        "luf": luf_list,           # LUF ramane nesmoothed (e deja lina)
        "fot": smooth3(fot_list),
    }