package com.ant.placebook.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {
    private val TAG = "ImageUtils"

    // Save the BitMap to a file instead of storing it directly in the database
    fun saveBitmapToFile(context: Context, bitmap: Bitmap, filename: String) {

        // Hold the image data
        val stream = ByteArrayOutputStream()

        // Write the bitmap to the stream object in PNG format
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)

        // Convert the string to an array of bytes
        val bytes = stream.toByteArray()

        // Write the array to a file
        saveBytesToFile(context, bytes, filename)
    }

    // Save a ByteArray to a file
    private fun saveBytesToFile(context: Context, bytes: ByteArray, filename: String) {

        // Prevent a crash with a try/catch
        try {
            // Open a file in the private area
            val outputStream: FileOutputStream =
                context.openFileOutput(filename, Context.MODE_PRIVATE)

            // Write the ByteArray bytes to the file and close it
            outputStream.write(bytes)
            outputStream.close()

        } catch (e: Exception) {
            // Catch any exception, log the error, and print the stack trace
            Log.e(TAG, "Exception when opening or writing file.")
            e.printStackTrace()
        }
    }

    // Load a Bitmap
    fun loadBitmapFromFile(context: Context, filename: String): Bitmap? {

        // Get the absolute file path
        val filePath = File(context.filesDir, filename).absolutePath

        // Load the image from the file and return the image
        return BitmapFactory.decodeFile(filePath)
    }
}