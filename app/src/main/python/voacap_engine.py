import math

def calculate_distance(lat1, lon1, lat2, lon2):
    """Calculates Great Circle Distance. Returns 1.0 km minimum."""
    R = 6371.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2)**2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2)**2
    return max(1.0, 2 * R * math.atan2(math.sqrt(a), math.sqrt(1 - a)))

def calculate_propagation(tx_lat, tx_lon, rx_lat, rx_lon, ssn, power, mode):
    dist_km = calculate_distance(tx_lat, tx_lon, rx_lat, rx_lon)

    # Power conversion
    p_dbw = 10 * math.log10(max(0.1, float(power)))

    # Mode Advantage (VOACAP ignores mode, but we add 'Digital Gain' relative to SSB)
    mode_map = {"FT8": 22, "CW": 12, "SSB": 0, "AM": -10}
    m_boost = mode_map.get(mode, 0)

    bands = [
         {"name": "80m", "freq": 3.5},
         {"name": "60m", "freq": 5.3},
         {"name": "40m", "freq": 7.0},
         {"name": "30m", "freq": 10.1},
         {"name": "20m", "freq": 14.0},
         {"name": "17m", "freq": 18.1},
         {"name": "15m", "freq": 21.0},
         {"name": "12m", "freq": 24.9},
         {"name": "10m", "freq": 28.0}
    ]

    results = []
    for b in bands:
        f = b["freq"]
        hourly = []
        for hr in range(24):
            # 1. SOLAR FACTOR (Diurnal Cycle)
            # Peaks at local noon (~14:00 UTC)
            solar_factor = max(0.0, math.cos((hr - 14) * math.pi / 12))

            # 2. MUF (Maximum Usable Frequency)
            # VOACAP style: SSN influences how high the frequencies go.
            muf = (8.0 + ssn/20) + (solar_factor * (12 + ssn/10))

            # 3. NOISE FLOOR (The 'All Green' Fix)
            # ITU-R P.372: Lower bands have massive atmospheric noise.
            # 80m noise floor is ~ -90dBm, 10m is ~ -115dBm.
            noise_dbm = -115 + (28 * math.log10(30/f))

            # 4. LOSSES
            # Free Space Path Loss
            fspl = 20 * math.log10(dist_km) + 20 * math.log10(f) + 32.4

            # Hop Loss: Every 3000km signal reflects. Each reflection = 10dB loss.
            hops = max(1, math.ceil(dist_km / 3200))
            ion_loss = hops * 12

            # Absorption (D-Layer): Kills low bands during the day.
            absorption = (solar_factor * 180) / (f ** 1.9)

            # 5. SNR & RELIABILITY
            if f <= muf:
                # Signal at receiver (dBm)
                signal_dbm = (p_dbw + 30) - (fspl + ion_loss + absorption)
                snr = signal_dbm - noise_dbm + m_boost

                # VOACAP Reliability Sigmoid
                # This makes 100% (Green) very hard to get.
                # Requires a +20dB SNR for a 'solid' link.
                rel = 100 / (1 + math.exp(-0.18 * (snr - 18)))
            else:
                # Above MUF: Signal escapes to space.
                rel = max(0, 15 - (f - muf) * 20)

            # Clamp and convert
            final_val = int(min(98, max(0, rel)))
            hourly.append(final_val)

        results.append({"band": b["name"], "probs": hourly})

    return results