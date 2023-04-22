package com.ant.placebook.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.view.View
import com.ant.placebook.databinding.ContentBookmarkInfoBinding
import com.ant.placebook.ui.MapsActivity
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.ant.placebook.viewmodel.MapsViewModel

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

        when (marker.tag) {

            // When marker.tag is a PlaceInfo then set the bitmap from PlaceInfo.image
            is MapsActivity.PlaceInfo -> {
                imageView.setImageBitmap(
                    (marker.tag as MapsActivity.PlaceInfo).image)
            }

            // When marker.tag is a BookmarkMarkerView, set the bitmap from BookmarkMarkerView
            is MapsViewModel.BookMarkerView -> {
                val bookmarkView = marker.tag as
                        MapsViewModel.BookMarkerView
                //TODO set the bitmap
            }

        }

        return binding.root
    }
}