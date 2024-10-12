package com.example.icm.taller2

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.icm.taller2.databinding.ActivityMapsBinding

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
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
import java.util.Locale
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
    private var currentMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mLocationRequest = createLocationRequest()

        mMap.uiSettings.isZoomGesturesEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true

        // Detectar cambios en la ubicación
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
                        updateMarker(location)
                    }
                }
            }
        }

        startLocationUpdates()

        // Obtener el fragmento del mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val editTextAddress = findViewById<EditText>(R.id.texto)
        editTextAddress.setOnEditorActionListener { _, _, _ ->
            val address = editTextAddress.text.toString()
            searchLocationByAddress(address)
            true
        }

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
            mFusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(
                        MarkerOptions().position(userLocation).title("Tu ubicación actual")
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))

                    mFusedLocationClient.requestLocationUpdates(
                        mLocationRequest,
                        mLocationCallback,
                        null
                    )
                }
            }
        }
        // Manejo del evento LongClick para crear un marcador
        mMap.setOnMapLongClickListener { latLng ->
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val addressText = addresses?.get(0)?.getAddressLine(0) ?: "Sin dirección"

            mMap.addMarker(MarkerOptions().position(latLng).title(addressText))

            // Mostrar la distancia entre el usuario y el nuevo marcador
            if (lastLocation != null) {
                val distanceToMarker = distance(
                    lastLocation!!.latitude,
                    lastLocation!!.longitude,
                    latLng.latitude,
                    latLng.longitude
                )
                Toast.makeText(
                    this,
                    "Distancia al marcador: $distanceToMarker metros",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Función para buscar una dirección usando Geocoder
    private fun searchLocationByAddress(address: String) {
        val geocoder = Geocoder(this, Locale.getDefault())
        val addresses = geocoder.getFromLocationName(address, 1)
        if (addresses != null) {
            if (addresses.isNotEmpty()) {
                val location = addresses[0]
                val latLng = LatLng(location.latitude, location.longitude)
                mMap.addMarker(MarkerOptions().position(latLng).title(address))
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateMarker(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        currentMarker?.remove() // Quitar marcador anterior
        currentMarker = mMap.addMarker(MarkerOptions().position(latLng).title("Ubicación actual"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    private fun createLocationRequest(): LocationRequest {
        var mLocationRequest: LocationRequest = LocationRequest.create()
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        mLocationRequest.setSmallestDisplacement(MIN_DISTANCE_METERS.toFloat())
        mLocationRequest.setInterval(60000) // Actualizar ubicación cada minuto
        return mLocationRequest
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
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
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    // Calcular distancia entre dos puntos
    fun distance(lat1: Double, long1: Double, lat2: Double, long2: Double): Double {
        val latDistance = Math.toRadians(lat1 - lat2)
        val lngDistance = Math.toRadians(long1 - long2)
        val a = (sin(latDistance / 2) * sin(latDistance / 2)
                + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2))
                * sin(lngDistance / 2) * sin(lngDistance / 2))
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val result = RADIUS_OF_EARTH_KM * c * 1000 // Convertir a metros
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

            Toast.makeText(applicationContext, "Ubicación guardada", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("LOCATION", "Error al guardar la ubicación", e)
        }
    }

    // Leer el JSON previamente guardado
    private fun readJSONArrayFromFile(fileName: String): JSONArray {
        val file = File(baseContext.getExternalFilesDir(null), fileName)
        if (!file.exists()) {
            Log.i("LOCATION", "Archivo no encontrado: $file")
            return JSONArray()
        }
        val jsonString = file.readText()
        return JSONArray(jsonString)
    }
}
