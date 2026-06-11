"""
test_iri2016.py v2
Ruleaza: python test_iri2016.py
"""
import sys
print(f"Python: {sys.version}")
print()

print("1. numpy...")
try:
    import numpy as np; print(f"   OK: {np.__version__}")
except ImportError as e:
    print(f"   LIPSESTE: pip install numpy"); sys.exit(1)

print("2. iri2016 import...")
try:
    import iri2016; print("   OK")
except Exception as e:
    print(f"   EROARE: {e}"); sys.exit(1)

print("3. IRI build...")
try:
    r = iri2016.IRI("2002-06-15T12:00:00", [200,400,100], 50.0, 20.0)
    fof2 = float(r['foF2'].values[0])
    print(f"   OK: foF2={fof2:.2f} MHz")
except Exception as e:
    print(f"   EROARE: {e}"); sys.exit(1)

print("4. Variatie diurna (0N/40E - ecuator, variatie maxima)...")
vals = {}
for h in [0, 6, 12, 18]:
    r = iri2016.IRI(f"2002-06-15T{h:02d}:00:00", [200,400,100], 0.0, 40.0)
    vals[h] = float(r['foF2'].values[0])
    print(f"   HR={h:2d}: foF2={vals[h]:.2f} MHz")

var = max(vals.values()) - min(vals.values())
print(f"   Variatie: {var:.2f} MHz", end="")
if var > 3.0:
    print(" -> OK")
else:
    print(" -> PROBLEMA")
    sys.exit(1)

print()
print("=" * 40)
print("TOTUL OK! Ruleaza: python generate_fof2_table.py")
print("=" * 40)