package com.example.lab04

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.lab04.databinding.FragmentMapsBinding

class MapsFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: FragmentMapsBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var sharedViewModel: SharedViewModel
    private var lastKnownLocation: LatLng? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            getLastLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val button: Button = view.findViewById(R.id.button_parked_here)
        button.setOnClickListener {
            if (lastKnownLocation != null) {
                moveCarIcon(lastKnownLocation!!)
            } else {
                getLastLocation()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        requestLocationPermission()

        // Set a map click listener
        mMap.setOnMapClickListener { latLng ->
            moveCarIcon(latLng)
            lastKnownLocation = latLng
        }
    }

    private fun moveCarIcon(latLng: LatLng) {
        // Clear the map of existing markers
        mMap.clear()

        // Convert vector drawable to bitmap
        val carBitmap = BitmapHelper.vectorToBitmap(requireContext(), R.drawable.ic_car)
        val carIcon = carBitmap?.let { BitmapDescriptorFactory.fromBitmap(it) }

        // Add the marker
        mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Parked Location")
                .icon(carIcon)
        )

        // Move the camera to the new location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

        // Update the shared view model with the new location
        sharedViewModel.setParkingLocation("${latLng.latitude}, ${latLng.longitude}")
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        if (hasLocationPermission()) {
            getLastLocation()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return // Permissions not granted, return
        }

        fusedLocationProviderClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLocation = LatLng(it.latitude, it.longitude)
                lastKnownLocation = currentLocation

                // Remove the previous marker if it exists
                mMap.clear()

                // Convert vector drawable to bitmap
                val carBitmap = BitmapHelper.vectorToBitmap(requireContext(), R.drawable.ic_car)
                if (carBitmap != null) {
                    val carIcon = BitmapDescriptorFactory.fromBitmap(carBitmap)
                    mMap.addMarker(
                        MarkerOptions()
                            .position(currentLocation)
                            .title("Current Location")
                            .icon(carIcon)
                    )
                } else {
                    mMap.addMarker(
                        MarkerOptions()
                            .position(currentLocation)
                            .title("Current Location")
                    )
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f))
                sharedViewModel.setParkingLocation("${it.latitude}, ${it.longitude}")
            }
        }
    }
}

object BitmapHelper {

    fun vectorToBitmap(context: Context, drawableId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }
}
