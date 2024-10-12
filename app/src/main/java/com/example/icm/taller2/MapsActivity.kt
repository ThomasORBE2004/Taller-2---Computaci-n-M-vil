package com.example.icm.taller2

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.icm.taller2.databinding.ActivityMapsBinding

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import org.json.JSONArray
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.Writer
import java.util.Date
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val REQUEST_LOCATION_PERMISSION = 1
        const val RADIUS_OF_EARTH_KM = 6.371
        const val MIN_DISTANCE_METERS = 30 // Mínimo desplazamiento en metros
    }

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private lateinit var mLocationRequest: LocationRequest
    private lateinit var mLocationCallback: LocationCallback

    private var lastLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    if (lastLocation == null || distance(
                            lastLocation!!.latitude,
                            lastLocation!!.longitude,
                            location.latitude,
                            location.longitude
                        ) >= MIN_DISTANCE_METERS
                    ) {
                        lastLocation = location
                        Log.i("LOCATION", "Nueva ubicación detectada: $location")
                        writeJSONObject(location)
                    }
                }
            }
        }

        startLocationUpdates()

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                null
            )
        }
        // Add a marker in Sydney and move the camera
        val sydney = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(sydney).title("Marker in Sydney"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
    }

    private fun createLocationRequest(): LocationRequest {
        var mLocationRequest: LocationRequest = LocationRequest.create()
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(MapsActivity.MIN_DISTANCE_METERS.toFloat());
        mLocationRequest.setInterval(60000); // Update location every 1 minute
        return mLocationRequest
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback,
                null
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                MapsActivity.REQUEST_LOCATION_PERMISSION
            )
        }
    }

    // Función para calcular distancia entre dos puntos
    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = (sin(latDistance / 2) * sin(latDistance / 2)
                + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2))
                * sin(lngDistance / 2) * sin(lngDistance / 2))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val result = MapsActivity.RADIUS_OF_EARTH_KM * c
        return (result * 100.0).roundToInt() / 100.0
    }

    // Guardar la ubicación en JSON
    private fun writeJSONObject(location: Location) {
        val localizaciones = readJSONArrayFromFile("locations.json")

        localizaciones.put(
            MyLocation(
                Date(System.currentTimeMillis()),
                location.latitude,
                location.longitude
            ).toJSON()
        )

        val filename = "locations.json"
        try {
            val file = File(baseContext.getExternalFilesDir(null), filename)
            Log.i("LOCATION", "Ubicacion de archivo: $file")
            val output: Writer = BufferedWriter(FileWriter(file))
            output.write(localizaciones.toString())
            output.close()

            Toast.makeText(applicationContext, "Location saved", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("LOCATION", "Error al guardar la ubicación", e)
        }
    }

    // Leer el JSON previamente guardado
    private fun readJSONArrayFromFile(fileName: String): JSONArray {
        val file = File(baseContext.getExternalFilesDir(null), fileName)
        if (!file.exists()) {
            Log.i("LOCATION", "Ubicacion de archivo: $file no encontrado")
            return JSONArray()
        }
        val jsonString = file.readText()
        return JSONArray(jsonString)
    }
}