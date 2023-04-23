package com.ant.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.ant.placebook.model.Bookmark
import com.ant.placebook.repository.BookmarkRepo
import com.ant.placebook.utils.ImageUtils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BookmarkDetailsViewModel(application: Application) :
    AndroidViewModel(application) {
    private val bookmarkRepo = BookmarkRepo(getApplication())
    private var bookmarkDetailsView: LiveData<BookmarkDetailsView>? = null

    // Convert a Bookmark model to BookmarkDetailsView model
    private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkDetailsView {
        return BookmarkDetailsView(
            bookmark.id,
            bookmark.name,
            bookmark.phone,
            bookmark.address,
            bookmark.notes,
            bookmark.category,
            bookmark.longitude,
            bookmark.latitude,
            bookmark.placeId
        )
    }

    // Transform a live Bookmark to a live BookmarkDetailsView
    private fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
        bookmarkDetailsView = Transformations.map(bookmark)
        { repoBookmark ->
            repoBookmark?.let { repoBookmark ->
                bookmarkToBookmarkView(repoBookmark)
            }
        }
    }

    // Update the bookmark view model class
    private fun bookmarkViewToBookmark(bookmarkView: BookmarkDetailsView): Bookmark? {
        val bookmark = bookmarkView.id?.let {
            bookmarkRepo.getBookmark(it)
        }

        // Populate the bookmarkView's parameters
        // TODO It appears that the textbook calls bookmarkDetailsView.attribute rather than
        //  bookmarkView.attribute
        if (bookmark != null) {
            bookmark.id = bookmarkView.id
            bookmark.name = bookmarkView.name
            bookmark.phone = bookmarkView.phone
            bookmark.address = bookmarkView.address
            bookmark.notes = bookmarkView.notes
            bookmark.category = bookmarkView.category
        }

        // Return the altered bookmark
        return bookmark
    }


    // Get a BookmarkDetailsView object
    fun getBookmark(bookmarkId: Long): LiveData<BookmarkDetailsView>? {
        // If bookmarkDetailsView is null, then create a bookmarkDetailsView
        if (bookmarkDetailsView == null) {
            mapBookmarkToBookmarkView(bookmarkId)
        }

        // Return the BookmarkDetailsView
        return bookmarkDetailsView
    }

    // Delete a bookmark
    @OptIn(DelicateCoroutinesApi::class)
    fun deleteBookmark(bookmarkDetailsView: BookmarkDetailsView) {

        // Load a bookmark
        GlobalScope.launch {
            val bookmark = bookmarkDetailsView.id?.let {
                bookmarkRepo.getBookmark(it)
            }

            // If a bookmark is found, delete it
            bookmark?.let {
                bookmarkRepo.deleteBookmark(it)
            }
        }
    }

    // Hold the info required by View
    data class BookmarkDetailsView(
        var id: Long? = null,
        var name: String = "",
        var phone: String = "",
        var address: String = "",
        var notes: String = "",
        var category: String = "",
        var longitude: Double = 0.0,
        var latitude: Double = 0.0,
        var placeId: String? = null
    ) {
        // Get the image
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(it))
        }

        // Set the image
        fun setImage(context: Context, image: Bitmap) {
            id?.let {
                ImageUtils.saveBitmapToFile(
                    context,
                    image,
                    Bookmark.generateImageFilename(it)
                )
            }
        }
    }

    // Update a bookmark
    @OptIn(DelicateCoroutinesApi::class)
    fun updateBookmark(bookmarkView: BookmarkDetailsView) {
        // Run the method in the background
        GlobalScope.launch {
            // Convert the BookmarkDetailsView to a Bookmark
            val bookmark = bookmarkViewToBookmark(bookmarkView)
            bookmark?.let { bookmarkRepo.updateBookmark(it) }
        }
    }

    // Get the categories of the bookmarks
    fun getCategories(): List<String> {
        return bookmarkRepo.categories
    }

    // Get the resource ID of a Category
    fun getCategoryResourceId(category: String): Int? {
        return bookmarkRepo.getCategoryResourceId(category)
    }
}