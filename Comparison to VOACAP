# HF PROP vs VOACAP — Method Comparison

## Similarities

Both applications use the same theoretical foundation: CCIR/ITU-R ionospheric coefficients, MUF derived from foF2 × M(3000)F2, D-layer absorption based on the George & Bradley model, and ITU-R P.533 for link budget estimation. The underlying physics is identical.

---

## Key Differences

| Aspect | VOACAP | HF PROP |
|---|---|---|
| **Ionospheric model** | CCIR coefficients (R12-based) | IRI-2016 table + CCIR fallback |
| **foF2 source** | Modelled from SSN | Modelled + anchored to live GIRO ionosonde data |
| **MUF calculation** | Full ray-tracing geometry | Simplified M(3000)F2 formula with multi-hop absorption factor |
| **Antenna model** | Extensive library (Yagi, rhombic, etc.) | Dipole approximation |
| **Noise model** | ITU-R P.372, environment-selectable | ITU-R P.372, quiet rural fixed |
| **Platform** | Web / desktop | Android (on-device, offline-capable) |
| **Calibration** | Validated against decades of measurement data | Calibrated against VOACAP Online on 4 path scenarios |

---

## Accuracy

MUF predictions were compared against VOACAP Online across four scenarios (short path NVIS 392 km, medium path 2000 km, long path 3500 km, winter conditions). Mean error was below 1.5 MHz for distances under 2000 km, rising to 2–3 MHz at 3500 km. The largest discrepancy occurs on long multi-hop paths where the simplified hop model cannot fully replicate VOACAP's ray-tracing results.

---

## When to use each

**VOACAP** is the reference tool for precise circuit planning, antenna selection, and statistical reliability studies.

**HF PROP** is intended for field use — fast on-device predictions with real-time ionospheric corrections, no internet required for calculation, optimised for portable and emergency communication scenarios.
