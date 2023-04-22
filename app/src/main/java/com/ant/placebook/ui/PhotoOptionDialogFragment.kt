package com.ant.placebook.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.DialogFragment

class PhotoOptionDialogFragment : DialogFragment() {

    // Implemented in BookmarkDetailsActivity
    interface PhotoOptionDialogListener {
        fun onCaptureClick()
        fun onPickClick()
    }

    // Hold an instance of PhotoOptionDialogListener
    private lateinit var listener: PhotoOptionDialogListener

    // Override onCreateDialog to
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Set listener to the parent activity
        listener = activity as PhotoOptionDialogListener

        // Set option indices to -1
        var captureSelectIdx = -1
        var pickSelectIdx = -1

        // Hold an options ArrayList to hold AlertDialog options
        val options = ArrayList<String>()

        // Hold the current activity as the context
        val context = activity as Context

        // If the device has a camera
        if (canCapture(context)) {
            // Add a Camera option to the options array
            options.add("Camera")
            captureSelectIdx = 0
        }

        // If the device can pick an image
        if (canPick(context)) {
            // Add a Gallery option to the options array
            options.add("Gallery")
            // If the camera option exists, then options will be at the second position
            pickSelectIdx = if (captureSelectIdx == 0) 1
            // Otherwise, it's at the first position
            else 0
        }

        // Build an AlertDialog
        return AlertDialog.Builder(context)
            .setTitle("Photo Option")
            .setItems(options.toTypedArray<CharSequence>()) { _, which ->
                // If Camera was selected then call onCaptureClick()
                if (which == captureSelectIdx) {
                    listener.onCaptureClick()
                    // If Gallery was selected then call onPickClick()
                } else if (which == pickSelectIdx) {
                    listener.onPickClick()
                }
            }
            // Add a cancel button
            .setNegativeButton("Cancel", null)
            .create()
    }

    companion object {
        // Determine if the device can pick an image from a gallery
        fun canPick(context: Context): Boolean {
            // Create an Intent for picking images and check if it can be resolved
            val pickIntent =
                Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            return (pickIntent.resolveActivity(context.packageManager) != null)
        }

        // Determine if the device can capture images
        fun canCapture(context: Context): Boolean {
            // Create an Intent for capturing images and check if it can be resolved
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            return (captureIntent.resolveActivity(context.packageManager) != null)
        }

        // Helper method to be used by parent activity when creating a PhotoOptionDialogFragment
        fun newInstance(context: Context) =
            // If the device can pick a gallery image or capture an image
            if (canPick(context) || canCapture(context)) {
                // Call and return PhotoOptionDialogFragment
            PhotoOptionDialogFragment()
        } else {
            // Otherwise, just return null
            null
        }
    }
}