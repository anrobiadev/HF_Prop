# HF PROP – A Field Companion

**An Android HF radio propagation prediction app for amateur radio operators.**

[![Version](https://img.shields.io/badge/version-3.0.0-blue)](https://github.com/anrobiadev/HF_Prop/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-green)](https://github.com/anrobiadev/HF_Prop)

---

## What it does

HF PROP calculates HF radio propagation conditions between two locations and presents the results in a way that is immediately useful in the field. Enter your TX and RX grid squares, tap Analyze, and the app returns:

- **MUF / FOT / LUF curves** for a full 24-hour period with an interactive tap-to-read chart
- **24-hour band reliability heatmap** across all amateur HF bands (160m → 10m)
- **Signal Footprint Map** showing coverage area, skip zones, and NVIS regions for each band
- **Real-time solar and ionospheric data** from NOAA, SIDC SILSO, DRAO Penticton, and the GIRO ionosonde network

The app is designed for portable and emergency communication use — it works fully offline after first launch (no internet required for propagation calculations) and produces results in seconds.

---

## Features

### Propagation Engine
- **IRI-2016 ionospheric model** — pre-computed lookup table (3 SSN levels × 12 months × 24 hours × 17 latitudes × 18 longitudes) for accurate foF2 interpolation
- **CCIR/ITU methodology** — George & Bradley D-layer absorption, ITU-R P.533 link budget, ITU-R P.372 atmospheric noise
- **Multi-hop geometry** — correct hop count and D-layer path calculation for all distances
- **NVIS detection** — automatic near-vertical incidence mode for frequencies below foF2
- **Calibrated against VOACAP Online** — validated on multiple real path scenarios
- **Real-time GIRO anchoring** — when ionosonde data is available, foF2 values are anchored to real measured data while preserving the modelled diurnal shape
- **K-Index geomagnetic storm correction**
- Supports **SSB, CW, FT8, AM** modes and any power level

### Real-Time Solar Data
- **SSN** — SIDC SILSO (Kalman-filtered sunspot number)
- **SFI** — NRC DRAO Penticton (10.7 cm solar flux)
- **K-Index** — NOAA SWPC
- **foF2 / MUF(D)** — GIRO DIDBase via FastChar API, from the nearest ionosonde stations to your path midpoint
- All data is **persisted locally** and survives app restarts

### Signal Footprint Map
- Per-band coverage polygons with physics-based skip distances
- **Skip zone** rendered as a diagonal-hatched overlay (dead zone between TX and skip distance)
- **NVIS bands** shown as filled circles (no skip zone)
- **Layers FAB button** — toggle individual bands, skip zones, and view the inline legend
- Built on OSMdroid with offline tile support

### User Interface
- Material 3 / Jetpack Compose
- Dark and light themes
- **Three languages** — English, Romanian, Hungarian
- Maidenhead grid square input with map-based TX/RX selection
- Interactive chart with tap-to-read exact MUF/FOT/LUF values per hour

---

## Technical Stack

| Layer | Technology |
|---|---|
| Language | Kotlin + Jetpack Compose |
| Propagation engine | Python 3.11 via Chaquopy |
| Ionospheric model | IRI-2016 + CCIR coefficient files |
| Map | OSMdroid |
| Charts | MPAndroidChart |
| Data storage | SharedPreferences |
| Solar data | NOAA / SIDC / DRAO / GIRO REST APIs |

The propagation engine runs entirely in Python on-device. No external servers are used for calculations.

---

## Project Structure

```
app/src/main/
├── java/com/example/hfpropagation/
│   ├── MainActivity.kt          — main UI, tab navigation, state
│   ├── SolarUtils.kt            — GIRO + solar data fetch
│   ├── StorageUtils.kt          — local data persistence
│   ├── TranslationProvider.kt   — EN / RO / HU strings
│   ├── SettingsTab.kt           — settings screen
│   └── HelpTab.kt               — help documentation
└── python/
    ├── voacap_engine.py         — propagation calculations
    ├── coverage_engine.py       — signal footprint map
    └── DVoaData/
        ├── FOF2CCIR01.dat       — CCIR foF2 coefficients (monthly)
        ├── Coeff01.dat          — M(3000)F2 coefficients (monthly)
        ├── ...                  — (12 pairs, one per month)
        └── fof2_iri2016.npz     — IRI-2016 pre-computed table
```

---

## Building from Source

### Requirements
- Android Studio Hedgehog or later
- Android SDK 26+
- Chaquopy plugin (configured in `build.gradle.kts`)

### Steps

```bash
git clone https://github.com/anrobiadev/HF_Prop.git
```

Open in Android Studio → **Build → Make Project**.

Chaquopy will automatically configure Python 3.11 and download `numpy` on first build.

### IRI-2016 Table

The file `fof2_iri2016.npz` is not included in the repository due to size. To generate it:

```bash
pip install iri2016 numpy   # also requires cmake + gfortran
python generate_fof2_table.py
```

Copy the result to `app/src/main/python/DVoaData/`. Without this file the app falls back to the CCIR analytic model, which is still functional.

---

## GIRO Data Notes

The GIRO FastChar API requires a residential or mobile IP. Datacenter IPs receive HTTP 403 — no proxy is needed from a phone.

- Separate requests per characteristic (`foF2` and `MUF(D)` fetched individually)
- Minimum 2-second delay between requests per station (LGDC policy)

---

## Data Sources

| Data | Source |
|---|---|
| Sunspot number (SSN) | SIDC SILSO |
| Solar flux index (SFI) | NRC DRAO Penticton |
| K-Index | NOAA SWPC |
| foF2 / MUF(D) | GIRO DIDBase / LGDC — FastChar API |
| Ionospheric coefficients | ITU-R / CCIR (embedded) |
| IRI-2016 model | IRI Working Group |

---

## Changelog

See [RELEASE_NOTES_v3.0.0.md](RELEASE_NOTES_v3.0.0.md) for the full list of changes from v2.8.1 to v3.0.0.

---

## Contributing

Contributions are welcome. Please open an issue before submitting a pull request for larger changes.

Areas where help is particularly welcome:
- Additional language translations
- Antenna gain models beyond the dipole approximation
- Ionosonde station selection improvements
- UI/UX refinements

---

## License

MIT — see [LICENSE](LICENSE) for details.

---
A short comparison with VOACAP engine
[Method comparison with VOACAP](VOACAP_COMPARISON.md)

---


## Acknowledgements

- **LGDC / Ivan Galkin** — GIRO DIDBase and FastChar API support
- **IRI Working Group** — IRI-2016 ionospheric model
- **ITU-R** — P.533 and P.372 propagation recommendations
- **VOACAP Online** — used as calibration reference
- **OSMdroid** — offline map library
- **RVSU** — Radioamatori Voluntari în Situații de Urgență

---

*Developed by YO7ZRO · 73!*
