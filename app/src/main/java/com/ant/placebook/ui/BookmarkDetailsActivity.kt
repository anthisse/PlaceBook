package com.ant.placebook.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.ant.placebook.R
import com.ant.placebook.databinding.ActivityBookmarkDetailsBinding
import com.ant.placebook.utils.ImageUtils
import com.ant.placebook.viewmodel.BookmarkDetailsViewModel
import java.io.File
import java.net.URLEncoder

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

        // Setup the layout components
        setupToolbar()
        setupFab()

        // Get the intent data
        getIntentData()
    }

    // Set up the toolbar
    private fun setupToolbar() {
        setSupportActionBar(databinding.toolbar)
    }

    // Set up a FAB
    private fun setupFab() {
        databinding.fab.setOnClickListener { sharePlace() }
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

                // Set databinding and fill in the text fields and categories
                databinding.bookmarkDetailsView = it
                populateImageView()
                populateCategoryList()
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
            bookmarkView.category = databinding.spinnerCategory.selectedItem as String
            bookmarkDetailsViewModel.updateBookmark(bookmarkView)
        }
        finish() // Close the activity
    }

    // Update the image
    private fun updateImage(image: Bitmap) {
        // Assign an image to imageViewPlace
        bookmarkDetailsView?.let {
            databinding.imageViewPlace.setImageBitmap(image)
            // Set the image
            it.setImage(this, image)
        }
    }

    // Create the PhotoOptionDialogFragment
    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }

    // Get an image from its path
    private fun getImageWithPath(filePath: String) = ImageUtils.decodeFileToSize(
        filePath,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height)
    )

    // Get an image from its Uri
    private fun getImageWithAuthority(uri: Uri) = ImageUtils.decodeUriStreamToSize(
        uri, resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height), this
    )

    // Populate the category list
    private fun populateCategoryList() {

        // Return immediately if bookmarkDetailsView is null
        val bookmarkView = bookmarkDetailsView ?: return

        // Get the category icon resourceId
        val resourceId = bookmarkDetailsViewModel.getCategoryResourceId(bookmarkView.category)

        // If it's not null, update imageViewCategory to category icon
        resourceId?.let { databinding.imageViewCategory.setImageResource(it) }

        // Get the list of categories and populate a spinner
        val categories = bookmarkDetailsViewModel.getCategories()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        // Assign the Adapter to the spinnerCategory control
        databinding.spinnerCategory.adapter = adapter

        // Update spinnerCategory to reflect the current category selection
        val placeCategory = bookmarkView.category
        databinding.spinnerCategory.setSelection(adapter.getPosition(placeCategory))

        // Avoid the initial call by Android to onItemSelected()
        databinding.spinnerCategory.post {

            // Assign spinnerCategory.onItemSelectedListener
            // to an instance of the onItemSelectedListener class
            databinding.spinnerCategory.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        // Replaced with safe call
                        // Set the category to a string
                        val category = parent?.getItemAtPosition(position) as String

                        // Set resourceId to the category's resource ID and set its image
                        val resourceId = bookmarkDetailsViewModel.getCategoryResourceId(category)
                        resourceId?.let {
                            databinding.imageViewCategory.setImageResource(it)
                        }
                    }

                    // Required abstract function for the object
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                    }
                }
        }
    }

    // Delete a bookmark
    private fun deleteBookmark() {
        val bookmarkView = bookmarkDetailsView ?: return

        // Display an AlertDialog for deleting a bookmark
        AlertDialog.Builder(this)
            .setMessage("Delete?")
            .setPositiveButton("Ok") {
                // If "Ok" is clicked, delete the bookmark
                    _, _ ->
                bookmarkDetailsViewModel.deleteBookmark(bookmarkView)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .create().show()
    }

    // TODO private fun sharePlace()

    // Share a place
    private fun sharePlace() {

        // Define bookmark view as a bookmarkDetailsView. If it's null, return
        val bookmarkView = bookmarkDetailsView ?: return

        // Initialize a String for a mapUrl
        var mapUrl = ""

        // If the place's id is null get a Google Maps URL based on lat and long
        if (bookmarkView.placeId == null) {
            val location = URLEncoder.encode(
                "${bookmarkView.latitude}," +
                        "${bookmarkView.longitude}", "utf-8"
            )
            mapUrl = "https://www.google.com/maps/dir/?api=1&destination=$location"
        } else {
            // Otherwise, the URL comes from the place's ID
            val name = URLEncoder.encode(bookmarkView.name, "utf-8")
            mapUrl = "https://www.google.com/maps/dir/?api=1&destination=" +
                    "$name&destination_place_id=${bookmarkView.placeId}"
        }

        // Create an Intent to share the location
        val sendIntent = Intent()

        // Set the Intent action to send, and add the text and subject as extras
        sendIntent.action = Intent.ACTION_SEND
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out ${bookmarkView.name} at:\n$mapUrl")
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Sharing ${bookmarkView.name}")
        sendIntent.type = "text/plain"

        // Start the activity
        startActivity(sendIntent)
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
            R.id.action_delete -> {
                deleteBookmark()
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

    // Override onActivityResult() to get a photo
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Make sure the result code is correct
        if (resultCode == android.app.Activity.RESULT_OK) {

            // When the requestCode is REQUEST_CAPTURE_IMAGE
            when (requestCode) {
                REQUEST_CAPTURE_IMAGE -> {
                    // Get the photoFile
                    val photoFile = photoFile ?: return
                    // Revoke write permissions
                    val uri = FileProvider.getUriForFile(
                        this,
                        "com.ant.placebook.fileprovider", photoFile
                    )
                    revokeUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    // Get the image from the photo's path
                    val image = getImageWithPath(photoFile.absolutePath)
                    // Rotate the image if required
                    val bitmap = ImageUtils.rotateImageIfRequired(this, image, uri)
                    // Update the bookmark image
                    updateImage(bitmap)
                }
                REQUEST_GALLERY_IMAGE -> if (data != null && data.data != null) {
                    val imageUri = data.data as Uri

                    // Load the selected image
                    val image = getImageWithAuthority(imageUri)
                    image?.let {
                        val bitmap = ImageUtils.rotateImageIfRequired(this, it, imageUri)
                        updateImage(bitmap)
                    }
                }
            }
        }
    }

    override fun onPickClick() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)
    }

    companion object {
        // Request code for processing camera capture
        private const val REQUEST_CAPTURE_IMAGE = 1

        // Request code for processing gallery image
        private const val REQUEST_GALLERY_IMAGE = 2
    }
}