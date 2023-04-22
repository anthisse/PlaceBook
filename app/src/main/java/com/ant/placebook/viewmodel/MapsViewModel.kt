package com.ant.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.ant.placebook.model.Bookmark
import com.ant.placebook.repository.BookmarkRepo
import com.ant.placebook.utils.ImageUtils
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place

// ViewModel MapsViewModel
class MapsViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "MapsViewModel"
    private var bookmarks: LiveData<List<BookmarkView>>? = null

    // Create BookmarkRepo object and pass it the application's context
    private val bookmarkRepo: BookmarkRepo = BookmarkRepo(getApplication())

    // Add a bookmark
    fun addBookmarkFromPlace(place: Place, image: Bitmap?) {
        // Create a Bookmark and set some of its attributes
        // toString() is not required for the string values (textbook invoked toString())
        val bookmark = bookmarkRepo.createBookmark()
        bookmark.placeId = place.id
        bookmark.name = place.name ?: ""
        bookmark.longitude = place.latLng?.longitude ?: 0.0
        bookmark.latitude = place.latLng?.latitude ?: 0.0
        bookmark.phone = place.phoneNumber ?: ""
        bookmark.address = place.address ?: ""

        // Save the Bookmark to the repo and log it
        val newId = bookmarkRepo.addBookmark(bookmark)
        Log.i(TAG, "New bookmark $newId added to the database.")

        // Set the image if the image isn't null
        image?.let { bookmark.setImage(it, getApplication()) }
    }

    // Convert a Bookmark to a BookmarkMarkerView
    private fun bookmarkToBookmarkView(bookmark: Bookmark) =
        BookmarkView(
            bookmark.id,
            LatLng(bookmark.latitude, bookmark.longitude),
            bookmark.name,
            bookmark.phone
        )


    // Map BookmarkRepo to objects that can be used by MapsActivity
    private fun mapBookmarksToBookmarkView() {
        // Map Bookmarks to BookmarkMarkerViews
        bookmarks = Transformations.map(bookmarkRepo.allBookmarks) { repoBookmarks ->
            // Store the return value of Transformations.map to bookmarks
            repoBookmarks.map { bookmark ->
                bookmarkToBookmarkView(bookmark)
            }
        }
    }

    fun getBookmarkViews():
            LiveData<List<BookmarkView>>? {
        if (bookmarks == null) {
            mapBookmarksToBookmarkView()
        }
        return bookmarks
    }

    // Hold info to plot a marker
    data class BookmarkView(
        var id: Long? = null,
        var location: LatLng = LatLng(0.0, 0.0),
        var name: String = "",
        var phone: String = ""
    ) {

        // Get an image for the marker
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(it))
        }
    }
}