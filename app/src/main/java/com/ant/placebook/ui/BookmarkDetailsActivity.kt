package com.ant.placebook.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ant.placebook.R
import com.ant.placebook.databinding.ActivityBookmarkDetailsBinding
import com.ant.placebook.viewmodel.BookmarkDetailsViewModel

// Set the content view with DataBindingUtil
class BookmarkDetailsActivity : AppCompatActivity() {
    private lateinit var databinding: ActivityBookmarkDetailsBinding
    private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        databinding = DataBindingUtil.setContentView(this, R.layout.activity_bookmark_details)
        setupToolbar()

        // Get the intent data
        getIntentData()
    }

    private fun setupToolbar() {
        setSupportActionBar(databinding.toolbar)
    }

    // Load the image from bookmarkView and set the imageViewPlace
    private fun populateImageView() {
        bookmarkDetailsView?.let { bookmarkView ->
            val placeImage = bookmarkView.getImage(this)
            placeImage?.let {
                databinding.imageViewPlace.setImageBitmap(placeImage)
            }
        }
    }

    // Get data from an Intent
    private fun getIntentData() {
        // Get the bookmarkId from the Intent
        val bookmarkId = intent.getLongExtra(
            MapsActivity.EXTRA_BOOKMARK_ID, 0
        )

        // Get the BookmarkDetailsView and observe for changes
        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(this) {

            // When the BookmarkDetailsView is changed, repopulate its fields
            it?.let {
                bookmarkDetailsView = it

                // Set databinding and fill in the text fields
                databinding.bookmarkDetailsView = it
                populateImageView()
            }
        }
    }

    private fun saveChanges() {
        val name = databinding.editTextName.text.toString()
        if (name.isEmpty()) {
            return
        }
        // Take the changes from the text fields and update the bookmark
        bookmarkDetailsView?.let { bookmarkView ->
            bookmarkView.name = databinding.editTextName.text.toString()
            bookmarkView.notes = databinding.editTextNotes.text.toString()
            bookmarkView.address = databinding.editTextAddress.text.toString()
            bookmarkView.phone = databinding.editTextPhone.text.toString()
            bookmarkDetailsViewModel.updateBookmark(bookmarkView)
        }
        finish() // Close the activity
    }

    // Override onCreateOptionsMenu and provide Toolbar items
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_bookmark_details, menu)
        return true
    }

    // Override onOptionsItemSelected
    override fun onOptionsItemSelected(item: MenuItem): Boolean =

        // When the itemId matches action_save, save the changes
        when (item.itemId) {
            R.id.action_save -> {
                saveChanges()
                true
            }
            // Otherwise, just super call the onOptionsItemSelected method
            else -> super.onOptionsItemSelected(item)
        }
}