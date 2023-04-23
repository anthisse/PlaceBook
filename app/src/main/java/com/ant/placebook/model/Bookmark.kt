package com.ant.placebook.model

import android.content.Context
import android.graphics.Bitmap
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ant.placebook.utils.FileUtils
import com.ant.placebook.utils.ImageUtils

// Database entity class Bookmark
@Entity
data class Bookmark(
    // Automatically generate ids
    @PrimaryKey(autoGenerate = true) var id: Long? = null,

    // Define the rest of the data with default values
    var placeId: String? = null,
    var name: String = "",
    var address: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var phone: String = "",
    var notes: String = "",
    var category: String = ""
) {

    // Save an image
    fun setImage(image: Bitmap, context: Context) {
        // If the bookmark has an id, then save the image to a file
        id?.let {
            ImageUtils.saveBitmapToFile(context, image, generateImageFilename(it))
        }
    }

    // Delete an image
    fun deleteImage(context: Context) {
        id?.let {
            FileUtils.deleteFile(context, generateImageFilename(it))
        }
    }

    // Put generateImageFilename() in a companion object so it's available at class level,
    // so that if an object needs an image it doesn't need to load the bookmark the database
    companion object {
        fun generateImageFilename(id: Long): String {
            // Return a file named "bookmark" and append its id to the file name
            return "bookmark$id.png"
        }
    }
}