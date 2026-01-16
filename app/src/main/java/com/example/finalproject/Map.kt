package com.example.finalproject

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions

private lateinit var map: GoogleMap
private lateinit var fusedLocationClient: FusedLocationProviderClient
class Map : AppCompatActivity(), OnMapReadyCallback {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_map)

        val mapContainer = findViewById<FrameLayout>(R.id.mapView)
        loadMap()

        // Back Button
        val backBtn = findViewById<Button>(R.id.backBtn)
        backBtn.setOnClickListener {
            startActivity(Intent(this, QuestionAndMedia::class.java))
        }
        // Next Button
        val nextBtn = findViewById<Button>(R.id.nextBtn)
        nextBtn.setOnClickListener {
            startActivity(Intent(this, VideoRecording::class.java))
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun loadMap() {
        val mapFragment = SupportMapFragment.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.mapView, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        val stc = LatLng(43.750198, -79.267273)

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true

        map.addMarker(
            MarkerOptions()
                .position(stc)
                .title("Scarborough Town Centre")
        )?.showInfoWindow()
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(stc, 15f))

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1001
            )
            return
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)
                val userMarker = map.addMarker(MarkerOptions().position(userLatLng).title("You are here"))
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f))

                map.setOnMapLoadedCallback {
                    userMarker?.showInfoWindow()
                }

                map.addPolyline(
                    PolylineOptions()
                        .clickable(true)
                        .add(stc, userLatLng)
                ).tag = "A"
            }
        }
    }
}