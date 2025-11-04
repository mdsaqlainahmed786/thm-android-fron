package com.thehotelmedia.android.extensions

class AQICalculator {

    // Function to calculate AQI for a given pollutant concentration
    private fun calculateAqiValue(concentrationLow: Double, concentrationHigh: Double, aqiLow: Int, aqiHigh: Int, concentration: Double): Double {
        return (aqiLow + (aqiHigh - aqiLow) * (concentration - concentrationLow) / (concentrationHigh - concentrationLow))
    }

    // Function to calculate AQI for PM2.5
    private fun calculateAQIForPM25(pm25: Double): Int {
        val breakpoints = arrayOf(0.0, 12.0, 35.4, 55.4, 150.4, 250.4, 350.4, 500.4)
        val aqiBreakpoints = arrayOf(0, 50, 100, 150, 200, 300, 400, 500)

        for (i in 0 until breakpoints.size - 1) {
            if (pm25 in breakpoints[i]..breakpoints[i + 1]) {
                println("PM2.5: $pm25 in range ${breakpoints[i]}..${breakpoints[i + 1]}")
                return calculateAqiValue(breakpoints[i], breakpoints[i + 1], aqiBreakpoints[i], aqiBreakpoints[i + 1], pm25).toInt()
            }
        }
        // For values above the highest breakpoint
        println("PM2.5: $pm25 exceeds highest breakpoint")
        return calculateAqiValue(breakpoints[breakpoints.size - 2], breakpoints[breakpoints.size - 1], aqiBreakpoints[aqiBreakpoints.size - 2], aqiBreakpoints[aqiBreakpoints.size - 1], pm25).toInt()
    }

    // Function to calculate AQI for PM10
    private fun calculateAQIForPM10(pm10: Double): Int {
        val breakpoints = arrayOf(0.0, 54.0, 154.0, 254.0, 354.0, 424.0, 504.0, 604.0)
        val aqiBreakpoints = arrayOf(0, 50, 100, 150, 200, 300, 400, 500)

        for (i in 0 until breakpoints.size - 1) {
            if (pm10 in breakpoints[i]..breakpoints[i + 1]) {
                println("PM10: $pm10 in range ${breakpoints[i]}..${breakpoints[i + 1]}")
                return calculateAqiValue(breakpoints[i], breakpoints[i + 1], aqiBreakpoints[i], aqiBreakpoints[i + 1], pm10).toInt()
            }
        }
        println("PM10: $pm10 exceeds highest breakpoint")
        return calculateAqiValue(breakpoints[breakpoints.size - 2], breakpoints[breakpoints.size - 1], aqiBreakpoints[aqiBreakpoints.size - 2], aqiBreakpoints[aqiBreakpoints.size - 1], pm10).toInt()
    }

    // Function to calculate AQI for CO (Carbon Monoxide)
    private fun calculateAQIForCO(co: Double): Int {
        val breakpoints = arrayOf(0.0, 4.4, 9.4, 12.4, 15.4)
        val aqiBreakpoints = arrayOf(0, 50, 100, 150, 200)

        for (i in 0 until breakpoints.size - 1) {
            if (co in breakpoints[i]..breakpoints[i + 1]) {
                println("CO: $co in range ${breakpoints[i]}..${breakpoints[i + 1]}")
                return calculateAqiValue(breakpoints[i], breakpoints[i + 1], aqiBreakpoints[i], aqiBreakpoints[i + 1], co).toInt()
            }
        }
        println("CO: $co exceeds highest breakpoint")
        return calculateAqiValue(breakpoints[breakpoints.size - 2], breakpoints[breakpoints.size - 1], aqiBreakpoints[aqiBreakpoints.size - 2], aqiBreakpoints[aqiBreakpoints.size - 1], co).toInt()
    }

    // Function to calculate AQI for NO (Nitric Oxide)
    private fun calculateAQIForNO(no: Double): Int {
        val breakpoints = arrayOf(0.0, 50.0, 100.0, 150.0, 200.0)
        val aqiBreakpoints = arrayOf(0, 50, 100, 150, 200)

        for (i in 0 until breakpoints.size - 1) {
            if (no in breakpoints[i]..breakpoints[i + 1]) {
                println("NO: $no in range ${breakpoints[i]}..${breakpoints[i + 1]}")
                return calculateAqiValue(breakpoints[i], breakpoints[i + 1], aqiBreakpoints[i], aqiBreakpoints[i + 1], no).toInt()
            }
        }
        println("NO: $no exceeds highest breakpoint")
        return calculateAqiValue(breakpoints[breakpoints.size - 2], breakpoints[breakpoints.size - 1], aqiBreakpoints[aqiBreakpoints.size - 2], aqiBreakpoints[aqiBreakpoints.size - 1], no).toInt()
    }

    // Function to calculate AQI for NO2 (Nitrogen Dioxide)
    private fun calculateAQIForNO2(no2: Double): Int {
        val breakpoints = arrayOf(0.0, 40.0, 100.0, 150.0, 200.0)
        val aqiBreakpoints = arrayOf(0, 50, 100, 150, 200)

        for (i in 0 until breakpoints.size - 1) {
            if (no2 in breakpoints[i]..breakpoints[i + 1]) {
                println("NO2: $no2 in range ${breakpoints[i]}..${breakpoints[i + 1]}")
                return calculateAqiValue(breakpoints[i], breakpoints[i + 1], aqiBreakpoints[i], aqiBreakpoints[i + 1], no2).toInt()
            }
        }
        println("NO2: $no2 exceeds highest breakpoint")
        return calculateAqiValue(breakpoints[breakpoints.size - 2], breakpoints[breakpoints.size - 1], aqiBreakpoints[aqiBreakpoints.size - 2], aqiBreakpoints[aqiBreakpoints.size - 1], no2).toInt()
    }

    // Function to calculate AQI for O3 (Ozone)
    private fun calculateAQIForO3(o3: Double): Int {
        val breakpoints = arrayOf(0.0, 0.06, 0.1, 0.2, 0.3)
        val aqiBreakpoints = arrayOf(0, 50, 100, 150, 200)

        for (i in 0 until breakpoints.size - 1) {
            if (o3 in breakpoints[i]..breakpoints[i + 1]) {
                println("O3: $o3 in range ${breakpoints[i]}..${breakpoints[i + 1]}")
                return calculateAqiValue(breakpoints[i], breakpoints[i + 1], aqiBreakpoints[i], aqiBreakpoints[i + 1], o3).toInt()
            }
        }
        println("O3: $o3 exceeds highest breakpoint")
        return calculateAqiValue(breakpoints[breakpoints.size - 2], breakpoints[breakpoints.size - 1], aqiBreakpoints[aqiBreakpoints.size - 2], aqiBreakpoints[aqiBreakpoints.size - 1], o3).toInt()
    }

    // Function to calculate AQI for SO2 (Sulfur Dioxide)
    private fun calculateAQIForSO2(so2: Double): Int {
        val breakpoints = arrayOf(0.0, 75.0, 150.0, 200.0, 250.0)
        val aqiBreakpoints = arrayOf(0, 50, 100, 150, 200)

        for (i in 0 until breakpoints.size - 1) {
            if (so2 in breakpoints[i]..breakpoints[i + 1]) {
                println("SO2: $so2 in range ${breakpoints[i]}..${breakpoints[i + 1]}")
                return calculateAqiValue(breakpoints[i], breakpoints[i + 1], aqiBreakpoints[i], aqiBreakpoints[i + 1], so2).toInt()
            }
        }
        println("SO2: $so2 exceeds highest breakpoint")
        return calculateAqiValue(breakpoints[breakpoints.size - 2], breakpoints[breakpoints.size - 1], aqiBreakpoints[aqiBreakpoints.size - 2], aqiBreakpoints[aqiBreakpoints.size - 1], so2).toInt()
    }

    // Function to calculate AQI for NH3 (Ammonia)
    private fun calculateAQIForNH3(nh3: Double): Int {
        val breakpoints = arrayOf(0.0, 100.0, 200.0, 300.0, 400.0)
        val aqiBreakpoints = arrayOf(0, 50, 100, 150, 200)

        for (i in 0 until breakpoints.size - 1) {
            if (nh3 in breakpoints[i]..breakpoints[i + 1]) {
                println("NH3: $nh3 in range ${breakpoints[i]}..${breakpoints[i + 1]}")
                return calculateAqiValue(breakpoints[i], breakpoints[i + 1], aqiBreakpoints[i], aqiBreakpoints[i + 1], nh3).toInt()
            }
        }
        println("NH3: $nh3 exceeds highest breakpoint")
        return calculateAqiValue(breakpoints[breakpoints.size - 2], breakpoints[breakpoints.size - 1], aqiBreakpoints[aqiBreakpoints.size - 2], aqiBreakpoints[aqiBreakpoints.size - 1], nh3).toInt()
    }

    // Function to calculate the overall AQI by considering all pollutants
    fun calculateOverallAQI(airQualityData: Map<String, Double>): Int {
        val aqiPm25 = calculateAQIForPM25(airQualityData["pm2_5"] ?: 0.0)
        val aqiPm10 = calculateAQIForPM10(airQualityData["pm10"] ?: 0.0)
        val aqiCo = calculateAQIForCO(airQualityData["co"] ?: 0.0)
        val aqiNo = calculateAQIForNO(airQualityData["no"] ?: 0.0)
        val aqiNo2 = calculateAQIForNO2(airQualityData["no2"] ?: 0.0)
        val aqiO3 = calculateAQIForO3(airQualityData["o3"] ?: 0.0)
        val aqiSo2 = calculateAQIForSO2(airQualityData["so2"] ?: 0.0)
        val aqiNh3 = calculateAQIForNH3(airQualityData["nh3"] ?: 0.0)

        // Return the maximum AQI value from all pollutants
        return maxOf(aqiPm25, aqiPm10, aqiCo, aqiNo, aqiNo2, aqiO3, aqiSo2, aqiNh3)
    }
}
