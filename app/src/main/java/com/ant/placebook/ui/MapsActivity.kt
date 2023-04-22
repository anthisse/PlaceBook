package com.ant.placebook.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.ant.placebook.R
import com.ant.placebook.adapter.BookmarkInfoWindowAdapter
import com.ant.placebook.viewmodel.MapsViewModel
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private val mapsViewModel by viewModels<MapsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up APIs
        setupLocationClient()
        setupPlacesClient()
    }

    // Tasks for when the map is ready
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setupMapListeners()
        getCurrentLocation()
        createBookmarkerMarkerObserver()
    }

    // Set up the Places API
    private fun setupPlacesClient() {
        Places.initialize(applicationContext, getString(R.string.google_maps_key))
        placesClient = Places.createClient(this)
    }

    // Set up the listeners for the map
    private fun setupMapListeners() {
        // Assign the InfoWindowAdapter to map
        map.setInfoWindowAdapter(BookmarkInfoWindowAdapter(this))
        map.setOnPoiClickListener {
            displayPoi(it)
        }
        // Set up a listener for clicking on points of interest
        map.setOnInfoWindowClickListener {
            handleInfoWindowClick(it)
        }
    }

    // Call display a POI
    private fun displayPoi(pointOfInterest: PointOfInterest) {
        displayPoiGetPlaceStep(pointOfInterest)
    }

    // Display information about a POI
    private fun displayPoiGetPlaceStep(pointOfInterest: PointOfInterest) {
        // Get the place's ID
        val placeId = pointOfInterest.placeId

        // Create a field and populate it with some info about the POI
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.PHOTO_METADATAS,
            Place.Field.ADDRESS,
            Place.Field.LAT_LNG
        )

        // Create a fetch request
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

        // Fetch details with placesClient
        // Create a success listener and use it get the POI's details
        placesClient.fetchPlace(request).addOnSuccessListener { response ->
            val place = response.place
            displayPoiGetPhotoStep(place)
        }
            // Add a failure listener in case the request fails
            .addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    // Log the failure
                    Log.e(
                        TAG, "Place not found: " + exception.message + ", " +
                                "statusCode: " + statusCode
                    )
                }
            }
    }

    // Get a photo for a POI
    private fun displayPoiGetPhotoStep(place: Place) {
        // Get the first PhotoMetaData object from the metadata array of a place
        val photoMetadata = place.photoMetadatas?.get(0)

        // If there's no photo, just skip to the next step
        if (photoMetadata == null) {
            displayPoiDisplayStep(place, null)
            return
        }

        // Create a fetch request, and pass in the photoMetaData, and maximum dimensions
        val photoRequest = FetchPhotoRequest
            .builder(photoMetadata)
            .setMaxHeight(
                resources.getDimensionPixelSize(
                    R.dimen.default_image_height
                )
            )
            .setMaxWidth(
                resources.getDimensionPixelSize(
                    R.dimen.default_image_width
                )
            )
            .build()

        // Call fetchPhoto and pass it photoRequest
        // Create a success listener
        placesClient.fetchPhoto(photoRequest)
            .addOnSuccessListener { fetchPhotoResponse ->
                val bitmap = fetchPhotoResponse.bitmap

                displayPoiDisplayStep(place, bitmap)

            }
            // Log if a failure occurred
            .addOnFailureListener { exception ->
                if (exception is ApiException) {
                    val statusCode = exception.statusCode
                    Log.e(
                        TAG, "Place not found: " + exception.message +
                                ", " + "statusCode: " + statusCode
                    )
                }
            }
    }

    // Display the POI's marker
    private fun displayPoiDisplayStep(place: Place, photo: Bitmap?) {
        // Add a marker with some info about the POI
        val marker = map.addMarker(
            MarkerOptions()
                .position(place.latLng as LatLng)
                .title(place.name)
                .snippet(place.phoneNumber)
        )
        marker?.tag = PlaceInfo(place, photo)

        // Display the Info window for a marker
        marker?.showInfoWindow()
    }

    // Set up the location services API
    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    // Handle a tap on a place info window
    @OptIn(DelicateCoroutinesApi::class)
    private fun handleInfoWindowClick(marker: Marker) {
        val placeInfo = (marker.tag as PlaceInfo)
        if (placeInfo.place != null) {

            // Use the launch coroutine to launch a coroutine in GlobalScope
            GlobalScope.launch {
                mapsViewModel.addBookmarkFromPlace(placeInfo.place, placeInfo.image)
            }
        }
        marker.remove()
    }

    private fun addPlaceMarker(
        bookmark: MapsViewModel.BookMarkerView
    ): Marker? {
        val marker = map.addMarker(
            MarkerOptions()
                .position(bookmark.location)
                .title(bookmark.name)
                .snippet(bookmark.phone)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .alpha(0.8f)
        )

        // Replaced with safe call
        marker?.tag = bookmark
        return marker
    }

    private fun displayAllBookmarks(
        bookmarks: List<MapsViewModel.BookMarkerView>
    ) {
        bookmarks.forEach { addPlaceMarker(it) }
    }

    private fun createBookmarkerMarkerObserver() {
        // Get a LiveData object and react to its changes with the observe method
        mapsViewModel.getBookMarkMarkerViews()?.observe(this) {
            // Clear the map's markers
            map.clear()
            it?.let {
                displayAllBookmarks(it)
            }
        }
    }

    // Request fine location permission
    private fun requestLocationPermissions() {
        // Pass current activity as context, an array of permissions, and a requestCode
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION
        )
    }

    // Get the current geographic location
    private fun getCurrentLocation() {
        // If we don't have location permissions, request them
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermissions()

        } else {
            // Otherwise display the user's location
            map.isMyLocationEnabled = true
            // Call a listener on lastLocation
            fusedLocationClient.lastLocation.addOnCompleteListener {
                val location = it.result
                // If our found location is not null
                if (location != null) {
                    // Create a latLng object at this location
                    val latLng = LatLng(location.latitude, location.longitude)
                    // Pan the camera to this location at a street-level view
                    val update = CameraUpdateFactory.newLatLngZoom(latLng, 16.0f)
                    map.moveCamera(update)
                } else {
                    // Otherwise, no location was found
                    Log.e(TAG, "No location found")
                }
            }
        }
    }

    // Get the permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        // This super call is not in the textbook's example
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // If the request is valid
        if (requestCode == REQUEST_LOCATION) {
            // If the item in index 0 of grantResults is PERMISSION_GRANTED
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Get the current location
                getCurrentLocation()
            } else {
                // Otherwise, we didn't get location permissions
                Log.e(TAG, "Location permissions denied")
            }
        }
    }

    companion object {
        private const val REQUEST_LOCATION = 1
        private const val TAG = "MapsActivity" // For debugging
    }

    // Define a class to hold a Place and a Bitmap
    class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)
}
