package com.ant.placebook.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.ant.placebook.repository.BookmarkRepo
import com.google.android.libraries.places.api.model.Place

// ViewModel MapsViewModel
class MapsViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MapsViewModel"

    // Create BookmarkRepo object and pass it the application's context
    private val bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())

    // Add a bookmark
    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {

        // Create a Bookmark and set some of its attributes
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name.toString()
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber.toString()
        bookmark.address = place.address.toString()

        // Save the Bookmark to the repo and log it
        val newId = bookmarkRepo.addBookmark(bookmark)
        Log.i(TAG, "New bookmark $newId added to the database.")
    }
}