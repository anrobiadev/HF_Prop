import math
from datetime import datetime

def get_muf_luf_data(tx_lat, tx_lon, rx_lat, rx_lon, ssn, sfi, k_index):
    """Calculeaz? curbele MUF, LUF ?i FOT pentru 24 de ore."""
    muf_list = []
    luf_list = []
    fot_list = []

    ssn = float(ssn)
    sfi = float(sfi)
    k_index = float(k_index)
    ion_index = (ssn + (sfi - 60) * 0.8) / 2

    for hr in range(24):
        # Peak solar activity slightly after noon
        solar_factor = max(0.0, math.cos((hr - 13) * math.pi / 12))

        # Logica de MUF
        base_muf = (7.5 + ion_index/15) + (solar_factor * (15 + ion_index/8))
        muf = base_muf - (k_index * 0.5)

        # LUF (cre?te cu fluxul solar ?i K-index)
        luf = 0.8 + (solar_factor * (4.5 + (sfi/100))) + (k_index * 0.3)

        # FOT (standard 85% din MUF)
        fot = muf * 0.85

        muf_list.append(float(round(muf, 2)))
        luf_list.append(float(round(luf, 2)))
        fot_list.append(float(round(fot, 2)))

    return {
        "muf": muf_list,
        "luf": luf_list,
        "fot": fot_list
    }

def calculate_propagation(tx_lat, tx_lon, rx_lat, rx_lon, ssn, sfi, k_index, power, mode):
    try:
        ssn = float(ssn)
        sfi = float(sfi)
        k_index = float(k_index)
        power = float(power)
    except:
        return [{"band": "DATA ERROR", "probs": [0]*24}]

    # Am ad?ugat ?i benzile intermediare pentru consisten?? cu Tab-ul 2
    bands = [
        {"name": "80m", "freq": 3.5},
        {"name": "40m", "freq": 7.0},
        {"name": "30m", "freq": 10.1},
        {"name": "20m", "freq": 14.0},
        {"name": "17m", "freq": 18.1},
        {"name": "15m", "freq": 21.0},
        {"name": "12m", "freq": 24.9},
        {"name": "10m", "freq": 28.0}
    ]

    now = datetime.utcnow()
    day_of_year = now.timetuple().tm_yday
    season_factor = (math.sin(math.radians((day_of_year - 80) * (360/365))) + 1) / 2

    lat1, lon1, lat2, lon2 = map(float, [tx_lat, tx_lon, rx_lat, rx_lon])
    R = 6371.0
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dp, dl = math.radians(lat2-lat1), math.radians(lon2-lon1)
    a = math.sin(dp/2)**2 + math.cos(p1) * math.cos(p2) * math.sin(dl/2)**2
    dist_km = max(1.0, 2 * R * math.atan2(math.sqrt(a), math.sqrt(1 - a)))

    erp_dbw = 10 * math.log10(max(0.1, power) * 20.0)
    mode_gain = {"FT8": 22, "CW": 12, "SSB": 0, "AM": -6}.get(mode, 0)

    results = []
    for b in bands:
        f = b["freq"]
        hourly = []
        for hr in range(24):
            solar_factor = max(0.0, math.cos((hr - 13) * math.pi / 12))
            ion_index = (ssn + (sfi - 60) * 0.9) / 2
            winter_boost = (1 - season_factor) * 4.0

            muf = (7.5 + ion_index/16) + (solar_factor * (16 + ion_index/8 + winter_boost))
            muf -= (k_index * 0.6)

            luf = 0.8 + (solar_factor * (4.5 + season_factor * 1.5)) + (k_index * 0.2)

            # --- CALCUL SNR ---
            fspl = 20 * math.log10(dist_km) + 20 * math.log10(f) + 32.4
            abs_strength = 500 + (sfi * 1.5) + (season_factor * 300)
            absorption = (solar_factor * abs_strength) / (f ** 2.0)

            geo_noise = k_index * 2.5
            noise_dbm = -112 + (25 * math.log10(30/f)) + geo_noise

            signal_dbm = (erp_dbw + 30) - (fspl + absorption + 10)
            snr = signal_dbm - noise_dbm + mode_gain

            # --- RELIABILITY LOGIC (Fix pentru casute negre) ---
            if f > muf:
                # Semnalul "scap?" în spa?iu, dar l?s?m o urm? de 2-5% dac? e aproape de MUF
                rel = max(0, 10 - (f - muf) * 15)
            elif f < luf:
                # Absorb?ie sever?, dar p?str?m un minim de vizibilitate
                rel = 100 / (1 + math.exp(0.3 * (luf - f + 5)))
            else:
                # Cale deschis?: Sigmoid ajustat s? evite pr?bu?irea la 0 prea repede
                # 22 este pragul pentru "Poor", 35 pentru "Fair"
                rel = 100 / (1 + math.exp(-0.12 * (snr - 20)))

            # Hard-clamp pentru 80m ziua (strat D)
            if f < 4.0 and 7 <= hr <= 16:
                rel = min(rel, 8)

            # FLOOR: Ne asigur?m c? dac? exist? m?car un pic de SNR, rel este minim 1
            # Astfel evit?m c?su?ele complet negre unde exist? propagare marginal?
            final_val = int(min(99, max(0, rel)))
            if snr > -10 and final_val == 0 and f < muf + 2:
                final_val = 5

            hourly.append(final_val)

        results.append({"band": b["name"], "probs": hourly})

    return results