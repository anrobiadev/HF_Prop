"""
generate_fof2_table.py  v3
==========================
Genereaza tabelul IRI2016 foF2 pentru HF_Prop.
Ruleaza PE PC (Windows/Linux/Mac) - necesita cmake + gfortran.

INSTALARE:
  pip install iri2016 numpy

Pe Windows necesita si:
  - Visual Studio Build Tools (C++ si Fortran) SAU
  - MinGW-w64 cu gfortran
  - cmake (https://cmake.org/download/)

RULARE:
  python generate_fof2_table.py

OUTPUT:
  fof2_iri2016.npz  (~300 KB)
  Copiaza in: app/src/main/python/DVoaData/fof2_iri2016.npz
"""

import numpy as np
import time
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed

try:
    import iri2016
except ImportError:
    print("ERROR: pip install iri2016 numpy")
    sys.exit(1)

# ─── VERIFICARE RAPIDA ────────────────────────────────────────────────────────
print("Verificare IRI2016...")
errors = []
for test in [
    ("2002-06-15T12:00:00", 50.0, 20.0, "50N/20E ora 12"),
    ("2002-06-15T00:00:00", 50.0, 20.0, "50N/20E ora 00"),
    ("2002-01-15T12:00:00",  0.0, 40.0, "0N/40E Ianuarie"),
]:
    dt_str, lat, lon, desc = test
    try:
        r    = iri2016.IRI(dt_str, [200, 400, 100], lat, lon)
        fof2 = float(r['foF2'].values[0])
        print(f"  OK {desc}: foF2={fof2:.2f} MHz")
    except Exception as e:
        print(f"  EROARE {desc}: {e}")
        errors.append(str(e))

if errors:
    print("\nIRI2016 nu functioneaza corect. Erori:")
    for e in errors:
        print(f"  {e}")
    print("\nSolutii:")
    print("  Linux:   sudo apt install cmake gfortran && pip install iri2016")
    print("  Windows: instaleaza cmake + mingw64 + gfortran, apoi pip install iri2016")
    print("  Mac:     brew install cmake gfortran && pip install iri2016")
    sys.exit(1)

print("IRI2016 OK!\n")

# ─── DIMENSIUNI ──────────────────────────────────────────────────────────────
LATS   = list(range(-80, 81, 10))   # 17
LONS   = list(range(0, 360, 20))    # 18
HOURS  = list(range(24))            # 24
MONTHS = list(range(1, 13))         # 12
SSN_LEVELS = [0, 100, 200]
SSN_YEARS  = {0: 2009, 100: 2002, 200: 2000}

fof2_data  = np.zeros((3, 12, 24, 17, 18), dtype=np.float32)
m3000_data = np.zeros((3, 12, 24, 17, 18), dtype=np.float32)

TOTAL = 3 * 12 * 24 * 17 * 18  # = 264,384
print(f"Total puncte: {TOTAL:,}")
print(f"Estimat: ~{TOTAL*0.055/8/60:.0f} minute cu 8 threads\n")

error_count = 0

def get_point(args):
    s_idx, m_idx, h_idx, la_idx, lo_idx = args
    ssn   = SSN_LEVELS[s_idx]
    year  = SSN_YEARS[ssn]
    month = MONTHS[m_idx]
    hour  = HOURS[h_idx]
    lat   = LATS[la_idx]
    lon   = LONS[lo_idx]
    dt_str = f"{year}-{month:02d}-15T{hour:02d}:00:00"
    try:
        r     = iri2016.IRI(dt_str, [200, 400, 100], float(lat), float(lon))
        fof2  = float(r['foF2'].values[0])
        hmF2  = float(r['hmF2'].values[0])
        fof2  = max(1.0, min(30.0, fof2))
        m3000 = 4.17 - 0.0093 * (max(200.0, min(500.0, hmF2)) - 300) / 100.0
        m3000 = max(2.5, min(4.5, m3000))
        return s_idx, m_idx, h_idx, la_idx, lo_idx, fof2, m3000, None
    except Exception as e:
        return s_idx, m_idx, h_idx, la_idx, lo_idx, -1.0, 3.2, str(e)

# ─── GENERARE ────────────────────────────────────────────────────────────────
tasks = [
    (s, m, h, la, lo)
    for s  in range(3)
    for m  in range(12)
    for h  in range(24)
    for la in range(17)
    for lo in range(18)
]

t0   = time.time()
done = 0
error_count = 0
first_error  = None

with ThreadPoolExecutor(max_workers=8) as executor:
    futures = {executor.submit(get_point, t): t for t in tasks}
    for future in as_completed(futures):
        s, m, h, la, lo, fof2, m3000, err = future.result()
        if err:
            error_count += 1
            if first_error is None:
                first_error = err
            # Fallback: interpola din ora anterioara daca exista
            if h > 0 and fof2_data[s, m, h-1, la, lo] > 1.0:
                fof2  = fof2_data[s, m, h-1, la, lo]
                m3000 = m3000_data[s, m, h-1, la, lo]
            else:
                fof2 = 5.0
        fof2_data [s, m, h, la, lo] = fof2
        m3000_data[s, m, h, la, lo] = m3000
        done += 1
        if done % 20000 == 0:
            elapsed = time.time() - t0
            eta     = (TOTAL - done) / (done / elapsed) / 60
            print(f"  {done:,}/{TOTAL:,} ({done/TOTAL*100:.1f}%)  "
                  f"erori={error_count}  ETA {eta:.0f} min")

elapsed = time.time() - t0
print(f"\nComplet: {elapsed:.0f}s ({elapsed/60:.1f} min), erori={error_count}")
if first_error:
    print(f"Prima eroare: {first_error}")

# ─── VERIFICARE ──────────────────────────────────────────────────────────────
la_eu = LATS.index(50)
lo_eu = LONS.index(20)

print(f"\nVariatie diurna 50N/20E, SSN=100, Iunie:")
vals = [fof2_data[1, 5, h, la_eu, lo_eu] for h in range(24)]
for h in range(0, 24, 3):
    print(f"  HR={h:2d}: {vals[h]:.2f} MHz")

variation = max(vals) - min(vals)
print(f"  Variatie: {variation:.2f} MHz  (trebuie > 3 MHz)")

if variation < 1.0:
    print("\n  ATENTIE: variatie prea mica - tabelul poate fi incorect!")
    print("  Verifica ca IRI ruleaza corect (cmake + gfortran instalate)")
    sys.exit(1)

print(f"\nComparatie SSN, 50N/20E, ora 12, Iunie:")
for s_i, s in enumerate(SSN_LEVELS):
    print(f"  SSN={s:3d}: {fof2_data[s_i, 5, 12, la_eu, lo_eu]:.2f} MHz")

# ─── SALVARE ─────────────────────────────────────────────────────────────────
import os
output = "fof2_iri2016.npz"
np.savez_compressed(
    output,
    fof2       = fof2_data,
    m3000      = m3000_data,
    lats       = np.array(LATS,       dtype=np.float32),
    lons       = np.array(LONS,       dtype=np.float32),
    ssn_levels = np.array(SSN_LEVELS, dtype=np.float32),
)
print(f"\nSalvat: {output} ({os.path.getsize(output)//1024} KB)")
print("Copiaza in: app/src/main/python/DVoaData/fof2_iri2016.npz")