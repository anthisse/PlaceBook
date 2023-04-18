package com.ant.placebook.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import com.ant.placebook.R
import com.ant.placebook.databinding.ContentBookmarkInfoBinding
import com.ant.placebook.ui.MapsActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker

class BookmarkInfoWindowAdapter(context: Activity) :
    GoogleMap.InfoWindowAdapter {
    // Call inflate to create a binding class instance
    private val binding =
        ContentBookmarkInfoBinding.inflate(context.layoutInflater)

    // This function is required, but can return null if
    // not replacing the entire info window
    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    // Override getInfoContents() and set its title and phone information
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun getInfoContents(marker: Marker): View {
        binding.title.text = marker.title ?: ""
        binding.phone.text = marker.snippet ?: ""
        val imageView = binding.photo

        // FIXME: The app crashes when clicking on a saved marker,
        //  since no image is saved in Bookmark
        try {
            // Set the image
            imageView.setImageBitmap((marker.tag as MapsActivity.PlaceInfo).image)
            // Catch the exception produced when clicking a saved bookmark
        } catch (ex: ClassCastException) {
            // Just set a default image instead of trying to set the image from the database
            imageView.setImageDrawable(binding.photo.context.getDrawable(R.drawable.default_photo))
        }

        return binding.root
    }
}