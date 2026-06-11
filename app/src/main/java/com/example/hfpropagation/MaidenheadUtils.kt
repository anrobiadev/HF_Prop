package com.example.hfpropagation
import kotlin.math.*


object MaidenheadUtils {

    /**
     * Converts a 4 or 6 character Maidenhead grid (e.g., "FN20" or "FN20qo")
     * to a Pair of (Latitude, Longitude).
     */
    fun gridToLatLon(grid: String): Pair<Double, Double>? {
        val g = grid.trim().uppercase()

        // Regex to ensure valid format: 2 letters (A-R), 2 numbers, optional 2 letters (A-X)
        if (!g.matches(Regex("^[A-R]{2}[0-9]{2}([A-X]{2})?$"))) {
            return null
        }

        // Calculate Fields (20° Longitude, 10° Latitude per field)
        var lon = (g[0] - 'A') * 20.0 - 180.0
        var lat = (g[1] - 'A') * 10.0 - 90.0

        // Calculate Squares (2° Longitude, 1° Latitude per square)
        lon += (g[2] - '0') * 2.0
        lat += (g[3] - '0') * 1.0

        // Calculate Subsquares if it's a 6-character grid
        if (g.length == 6) {
            lon += (g[4] - 'A') * (5.0 / 60.0) // 5 minutes
            lat += (g[5] - 'A') * (2.5 / 60.0) // 2.5 minutes

            // Shift to the center of the 6-character subsquare
            lon += (2.5 / 60.0)
            lat += (1.25 / 60.0)
        } else {
            // Shift to the center of the 4-character square
            lon += 1.0
            lat += 0.5
        }

        return Pair(lat, lon)
    }

    /**
     * Converts Latitude and Longitude back into a 6-character Maidenhead grid.
     * Useful when the user gets their location via the phone's GPS.
     */
    fun latLonToGrid(lat: Double, lon: Double): String {
        var adjustedLon = lon + 180.0
        var adjustedLat = lat + 90.0

        val fieldLon = (adjustedLon / 20.0).toInt()
        val fieldLat = (adjustedLat / 10.0).toInt()

        adjustedLon -= fieldLon * 20.0
        adjustedLat -= fieldLat * 10.0

        val squareLon = (adjustedLon / 2.0).toInt()
        val squareLat = (adjustedLat / 1.0).toInt()

        adjustedLon -= squareLon * 2.0
        adjustedLat -= squareLat * 1.0

        val subLon = (adjustedLon / (5.0 / 60.0)).toInt()
        val subLat = (adjustedLat / (2.5 / 60.0)).toInt()

        return buildString {
            append((fieldLon + 'A'.code).toChar())
            append((fieldLat + 'A'.code).toChar())
            append((squareLon + '0'.code).toChar())
            append((squareLat + '0'.code).toChar())
            append((subLon + 'a'.code).toChar())
            append((subLat + 'a'.code).toChar())
        }
    }
    object PathMath {
        private const val EARTH_RADIUS_KM = 6371.0

        /**
         * Calculates the Great Circle distance between two points in kilometers.
         */
        fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return EARTH_RADIUS_KM * c
        }

        /**
         * Calculates the initial bearing (azimuth) from point 1 to point 2 in degrees.
         */
        fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val y = sin(Math.toRadians(lon2 - lon1)) * cos(Math.toRadians(lat2))
            val x = cos(Math.toRadians(lat1)) * sin(Math.toRadians(lat2)) -
                    sin(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * cos(Math.toRadians(lon2 - lon1))
            val bearing = Math.toDegrees(atan2(y, x))
            return (bearing + 360) % 360 // Normalize to 0-360
        }
    }

}