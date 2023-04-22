package com.ant.placebook.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.ant.placebook.R
import com.ant.placebook.databinding.ActivityBookmarkDetailsBinding
import com.ant.placebook.utils.ImageUtils
import com.ant.placebook.viewmodel.BookmarkDetailsViewModel
import java.io.File

// Set the content view with DataBindingUtil
class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionDialogFragment.PhotoOptionDialogListener {
    private lateinit var databinding: ActivityBookmarkDetailsBinding
    private val TAG = "BookmarkDetailsActivity"
    private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsView: BookmarkDetailsViewModel.BookmarkDetailsView? = null
    private var photoFile: File? = null
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
        // Clear any previous photoFile
        photoFile = null
        try {
            // Call createUniqueImageFile and assign it to photoFile
            photoFile = ImageUtils.createUniqueImageFile(this)
        } catch (ex: java.io.IOException) {
            // Catch IO exceptions and return without doing anything
            Log.e(TAG, "IO Error!")
            return
        }

        // If the photoFile is not null
        photoFile?.let { photoFile ->

            // Get a Uri for the temporary photo file
            val photoUri = FileProvider.getUriForFile(
                this,
                "com.ant.placebook.fileprovider", photoFile
            )

            // Create a new Intent to display the camera viewfinder
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            // Add photoUri as an extra on the Intent
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)

            // Grant the Intent write permissions
            val intentActivities = packageManager.queryIntentActivities(
                captureIntent, PackageManager.MATCH_DEFAULT_ONLY
            )
            intentActivities.map { it.activityInfo.packageName }.forEach {
                grantUriPermission(it, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }

            // Invoke the Intent and pass the appropriate request code
            startActivityForResult(captureIntent, REQUEST_CAPTURE_IMAGE)
        }
    }

    override fun onPickClick() {
        Toast.makeText(this, "Gallery Pick", Toast.LENGTH_SHORT).show()
    }

    // Create the PhotoOptionDialogFragment
    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }

    companion object {
        // Request code for processing camera capture Intent
        private const val REQUEST_CAPTURE_IMAGE = 1
    }
}