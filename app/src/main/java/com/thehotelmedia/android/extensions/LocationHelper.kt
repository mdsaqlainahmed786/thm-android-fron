package com.thehotelmedia.android.extensions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*

class LocationHelper(
    private val context: Context,
    private val permissionLauncher: ActivityResultLauncher<Array<String>>,
    private val locationCallback: (latitude: Double, longitude: Double) -> Unit,
    private val errorCallback: (String) -> Unit
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private val locationRequest: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L)
        .setMinUpdateIntervalMillis(5000L)
        .build()

    /**
     * Check for location permissions and request them if not granted. If granted, fetch location.
     */
    fun checkAndRequestLocation() {
        if (!arePermissionsGranted()) {
            // Request permissions
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // Permissions are granted
            fetchLocation()
        }
    }

    /**
     * Fetch the last known location or request a new location update if needed.
     */
    private fun fetchLocation() {
        if (!arePermissionsGranted()) {
            errorCallback("Location permission not granted.")
            return
        }

        // Explicitly check permissions before accessing location
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {

            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        locationCallback(location.latitude, location.longitude)
                    } else {
                        // Request a fresh location if last location is null
                        requestNewLocation()
                    }
                }
                .addOnFailureListener {
                    errorCallback("Failed to get last known location: ${it.message}")
                }
        } else {
            errorCallback("Location permission not granted.")
        }
    }

    /**
     * Request new location updates.
     */
    private fun requestNewLocation() {
        if (!arePermissionsGranted()) {
            errorCallback("Location permission not granted.")
            return
        }
        // Explicit permission check before calling requestLocationUpdates
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {

            val callback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    fusedLocationClient.removeLocationUpdates(this)
                    val location: Location? = locationResult.lastLocation
                    if (location != null) {
                        locationCallback(location.latitude, location.longitude)
                    } else {
                        errorCallback("Failed to get current location.")
                    }
                }

                override fun onLocationAvailability(availability: LocationAvailability) {
                    if (!availability.isLocationAvailable) {
                        errorCallback("Location services are currently unavailable.")
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
        } else {
            errorCallback("Location permission not granted.")
        }
    }

    /**
     * Check if location permissions are granted.
     */
    private fun arePermissionsGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }
}
