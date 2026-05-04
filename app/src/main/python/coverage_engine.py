import math

def calculate_destination(lat, lon, bearing, distance_km):
    """Calculates coordinates of a point at a given distance and bearing."""
    R = 6371.0
    brng = math.radians(bearing)
    phi1 = math.radians(lat)
    lam1 = math.radians(lon)

    phi2 = math.asin(math.sin(phi1) * math.cos(distance_km/R) +
                     math.cos(phi1) * math.sin(distance_km/R) * math.cos(brng))
    lam2 = lam1 + math.atan2(math.sin(brng) * math.sin(distance_km/R) * math.cos(phi1),
                             math.cos(distance_km/R) - math.sin(phi1) * math.sin(phi2))

    return [math.degrees(phi2), math.degrees(lam2)]

def get_area_coverage(tx_lat, tx_lon, ssn, power, mode):
    # Band-specific physics parameters to prevent "All Green"
    # freq: MHz, color: Hex, base_reach: km, sensitivity: how much SSN affects it
    bands = [
        {"name": "80m", "freq": 3.5,  "color": "#B71C1C", "reach": 600,  "ssn_mult": 0.5},
        {"name": "40m", "freq": 7.0,  "color": "#E64A19", "reach": 1200, "ssn_mult": 1.2},
        {"name": "30m", "freq": 10.1, "color": "#FBC02D", "reach": 1800, "ssn_mult": 2.0},
        {"name": "20m", "freq": 14.0, "color": "#43A047", "reach": 3500, "ssn_mult": 5.0},
        {"name": "17m", "freq": 18.1, "color": "#1E88E5", "reach": 4000, "ssn_mult": 8.0},
        {"name": "15m", "freq": 21.0, "color": "#3949AB", "reach": 4500, "ssn_mult": 12.0},
        {"name": "12m", "freq": 24.9, "color": "#8E24AA", "reach": 5000, "ssn_mult": 15.0},
        {"name": "10m", "freq": 28.0, "color": "#00ACC1", "reach": 6000, "ssn_mult": 20.0}
    ]

    # Mode multiplier: FT8 expands the footprint more than SSB
    mode_mult = {"FT8": 1.4, "CW": 1.2, "SSB": 1.0}.get(mode, 1.0)

    # Power factor: Logarithmic expansion
    p_factor = math.log10(max(1, power)) / 2.0

    coverage_layers = []

    for b in bands:
        f = b["freq"]
        polygon_points = []

        # 1. Calculate MUF (Maximum Usable Frequency)
        # If the band freq is higher than MUF, the 'reach' is 0 (it won't reflect)
        current_muf = 10 + (ssn / 10.0) # Simple MUF estimate

        # 2. Determine the reach of this specific band
        if f > current_muf + 5:
            # Band is closed
            final_reach = 150 # Local groundwave only
        else:
            # Band is open: Calculate reach based on physics
            # Low bands are limited by D-Layer absorption (reach decreases with solar activity)
            # High bands are limited by MUF (reach increases with solar activity)
            if f < 10:
                # 80m/40m: Reach actually shrinks slightly as SSN increases due to absorption
                final_reach = b["reach"] * (1 - (ssn/500)) * mode_mult * p_factor
            else:
                # 20m-10m: Reach expands as SSN increases
                final_reach = (b["reach"] + (ssn * b["ssn_mult"])) * mode_mult * p_factor

        # 3. Sample 24 points for a smooth polygon (matching the JS reference style)
        for bearing in range(0, 360, 15):
            # Add some 'jitter' to make it look like real ionospheric data
            jitter = 1.0 + (math.sin(bearing * f) * 0.05)
            point = calculate_destination(tx_lat, tx_lon, bearing, final_reach * jitter)
            polygon_points.append(point)

        coverage_layers.append({
            "band": b["name"],
            "color": b["color"],
            "points": polygon_points
        })

    return coverage_layers