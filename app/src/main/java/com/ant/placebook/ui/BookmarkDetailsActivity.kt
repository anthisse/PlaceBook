package com.ant.placebook.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.ant.placebook.R
import com.ant.placebook.databinding.ActivityBookmarkDetailsBinding
import com.ant.placebook.viewmodel.BookmarkDetailsViewModel

// Set the content view with DataBindingUtil
class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionDialogFragment.PhotoOptionDialogListener {
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

                // Replace the image when the image is tapped
                databinding.imageViewPlace.setOnClickListener {
                    replaceImage()
                }
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

    override fun onCaptureClick() {
        Toast.makeText(this, "Camera Capture", Toast.LENGTH_SHORT).show()
    }

    override fun onPickClick() {
        Toast.makeText(this, "Gallery Pick", Toast.LENGTH_SHORT).show()
    }

    // Create the PhotoOptionDialogFragment
    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }
}