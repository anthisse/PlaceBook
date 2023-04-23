package com.ant.placebook.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.ant.placebook.R
import com.ant.placebook.adapter.BookmarkInfoWindowAdapter
import com.ant.placebook.adapter.BookmarkListAdapter
import com.ant.placebook.databinding.ActivityMapsBinding
import com.ant.placebook.viewmodel.MapsViewModel
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
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
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPhotoRequest
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private val mapsViewModel by viewModels<MapsViewModel>()
    private lateinit var databinding: ActivityMapsBinding
    private lateinit var bookmarkListAdapter: BookmarkListAdapter
    private var markers = HashMap<Long, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databinding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(databinding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up the toolbar for the navigation drawer
        setupToolbar()

        // Set up APIs
        setupLocationClient()
        setupPlacesClient()

        // Set up the navigation drawer
        setupNavigationDrawer()
    }

    // Tasks for when the map is ready
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        setupMapListeners()
        getCurrentLocation()
        createBookmarkObserver()
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
        // Set up a listener for clicking on the search button
        databinding.mainMapView.fab.setOnClickListener {
            searchAtCurrentLocation()
        }

        // Set up a long click listener for adding new bookmarks
        map.setOnMapLongClickListener { latLng -> newBookmark(latLng) }
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
            Place.Field.LAT_LNG,
            Place.Field.TYPES
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
        // When marker.tag is a PlaceInfo, save the bookmark to the database
        when (marker.tag) {
            is PlaceInfo -> {
                val placeInfo = (marker.tag as PlaceInfo)
                if (placeInfo.place != null && placeInfo.image != null) {
                    GlobalScope.launch {
                        mapsViewModel.addBookmarkFromPlace(placeInfo.place, placeInfo.image)
                    }
                }
                marker.remove()
            }

            // When it's a BookMarkerView, start BookMarkerView
            is MapsViewModel.BookmarkView -> {
                val bookmarkMarkerView = (marker.tag as MapsViewModel.BookmarkView)
                marker.hideInfoWindow()
                bookmarkMarkerView.id?.let {
                    startBookmarkDetails(it)
                }
            }
        }
    }

    private fun addPlaceMarker(
        bookmark: MapsViewModel.BookmarkView
    ): Marker? {
        val marker = map.addMarker(
            MarkerOptions()
                .position(bookmark.location)
                .title(bookmark.name)
                .snippet(bookmark.phone)
                .icon(bookmark.categoryResourceId?.let {
                    BitmapDescriptorFactory.fromResource(it)
                })
                .alpha(0.8f)
        )

        // Replaced with safe call
        marker?.tag = bookmark
        bookmark.id?.let {
            if (marker != null) {
                markers[it] = marker
            }
        }
        return marker
    }

    // Display all the bookmarks
    private fun displayAllBookmarks(bookmarks: List<MapsViewModel.BookmarkView>) {
        // Loop through the list of bookmarks and add a marker at each one
        bookmarks.forEach { addPlaceMarker(it) }
    }

    // Create an observer for the Bookmarks
    private fun createBookmarkObserver() {
        // Get a LiveData object and react to its changes with the observe method
        mapsViewModel.getBookmarkViews()?.observe(this) {
            // Clear the map's markers
            map.clear()
            markers.clear()
            it?.let {
                displayAllBookmarks(it)
                // Set the data to the RecyclerView adapter
                bookmarkListAdapter.setBookmarkData(it)
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

    // Start BookmarkDetailsActivity
    private fun startBookmarkDetails(bookmarkId: Long) {
        val intent = Intent(this, BookmarkDetailsActivity::class.java)

        // Add the bookmarkId to the Intent
        intent.putExtra(EXTRA_BOOKMARK_ID, bookmarkId)
        startActivity(intent)
    }

    // Set up the toolbar
    private fun setupToolbar() {
        setSupportActionBar(databinding.mainMapView.toolbar)
        // Manage the toolbar's functionality and appearance
        val toggle = ActionBarDrawerToggle(
            this, databinding.drawerLayout, databinding.mainMapView.toolbar,
            R.string.open_drawer, R.string.close_drawer
        )
        // Ensure the toggle icon is displayed
        toggle.syncState()

    }

    // Get a RecyclerView from the Layout and set a LinearLayoutManager for the RecyclerView
    // Create a new BookmarkListAdapter and assign it to the RecyclerView
    private fun setupNavigationDrawer() {
        val layoutManager = LinearLayoutManager(this)
        databinding.drawerViewMaps.bookmarkRecyclerView.layoutManager = layoutManager
        bookmarkListAdapter = BookmarkListAdapter(null, this)
        databinding.drawerViewMaps.bookmarkRecyclerView.adapter = bookmarkListAdapter
    }

    // Pan the map camera to a Location
    private fun updateMapToLocation(location: Location) {
        val latLng = LatLng(location.latitude, location.longitude)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16.0f))
    }

    private fun searchAtCurrentLocation() {

        // Define a list of fields of places
        val placeFields = listOf(
            Place.Field.ID,
            Place.Field.NAME,
            Place.Field.PHONE_NUMBER,
            Place.Field.LAT_LNG,
            Place.Field.ADDRESS,
            Place.Field.TYPES
        )

        // Get the bounds of the current map
        val bounds = RectangularBounds.newInstance(map.projection.visibleRegion.latLngBounds)

        // Build an intent and overlay the search widget with the current activity
        try {
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, placeFields)
                .setLocationBias(bounds)
                .build(this)

            // Start the activity
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)

        } catch (e: GooglePlayServicesRepairableException) {
            // Catch exceptions when searching
            Toast.makeText(this, "Problems Searching", Toast.LENGTH_LONG).show()
            Log.e(TAG, "Problems Searching")

        } catch (e: GooglePlayServicesNotAvailableException) {
            // Catch an exception when Google Play Services are not available
            Toast.makeText(
                this,
                "Problems Searching. Google Play is not available",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Add a new bookmark from a location
    @OptIn(DelicateCoroutinesApi::class)
    private fun newBookmark(latLng: LatLng) {
        GlobalScope.launch {
            val bookmarkId = mapsViewModel.addBookmark(latLng)
            bookmarkId?.let {
                startBookmarkDetails(it)
            }
        }
    }

    // Set a flag to prevent user interaction
    private fun disableInteraction() {
        window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    // Clear FLAG_NOT_TOUCHABLE
    private fun enableInteraction() {
        window.clearFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    // Show the progress of some process
    private fun showProgress() {
        databinding.mainMapView.progressBar.visibility = ProgressBar.VISIBLE
        disableInteraction()
    }

    // Move the map camera to a Bookmark
    fun moveToBookmark(bookmark: MapsViewModel.BookmarkView) {

        // Close the navigation drawer
        databinding.drawerLayout.closeDrawer(databinding.drawerViewMaps.drawerView)

        // Find the marker in the HashMap
        val marker = markers[bookmark.id]

        // Show its Info window
        marker?.showInfoWindow()

        // Zoom the map to the location
        val location = Location("")
        location.latitude = bookmark.location.latitude
        location.longitude = bookmark.location.longitude
        updateMapToLocation(location)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // When the request code is correct
        when (requestCode) {
            // If a place was found and the data is not null
            AUTOCOMPLETE_REQUEST_CODE -> if (resultCode == Activity.RESULT_OK && data != null) {
                // Get the place
                val place = Autocomplete.getPlaceFromIntent(data)

                // Convert latLng to a location
                val location = Location("")
                location.latitude = place. latLng?.latitude ?: 0.0
                location.longitude = place.latLng.longitude ?: 0.0

                // Display the place info window
                displayPoiGetPhotoStep(place)
            }
        }
    }

    companion object {
        const val EXTRA_BOOKMARK_ID = "com.ant.placebook.EXTRA_BOOKMARK_ID"
        private const val REQUEST_LOCATION = 1
        private const val AUTOCOMPLETE_REQUEST_CODE = 2
        private const val TAG = "MapsActivity" // For debugging
    }

    // Define a class to hold a Place and a Bitmap
    class PlaceInfo(val place: Place? = null, val image: Bitmap? = null)
}
