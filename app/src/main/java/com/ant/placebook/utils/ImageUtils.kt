package com.ant.placebook.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

object ImageUtils {
    private const val TAG = "ImageUtils"

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
            Log.e(TAG, "IO Exception!")
            e.printStackTrace()
        }
    }

    // Calculate the optimal inSampleSize to resize an image to a requested width and height
    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // Rotate a bitmap
    private fun rotateImage(img: Bitmap, degree: Float): Bitmap? {
        val matrix = Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    // Load a Bitmap
    fun loadBitmapFromFile(context: Context, filename: String): Bitmap? {
        // Get the absolute file path
        val filePath = File(context.filesDir, filename).absolutePath
        // Load the image from the file and return the image
        return BitmapFactory.decodeFile(filePath)
    }

    // Create an image file
    @Throws(IOException::class)
    fun createUniqueImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMddHHmmss").format(Date())
        val filename = "PlaceBook_" + timeStamp + "_"
        val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(filename, ".jpg", filesDir)
    }

    // Decode a file to a specified size
    fun decodeFileToSize(filePath: String, width: Int, height: Int): Bitmap {
        // Load the size of the image
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(filePath, options)

        // Calculate the sample size
        options.inSampleSize = calculateInSampleSize(
            options.outWidth, options.outHeight, width, height
        )

        // Load the full image
        options.inJustDecodeBounds = false

        // Return the down-sampled image
        return BitmapFactory.decodeFile(filePath, options)
    }

    // Check an image's rotation and rotate it if required
    @Throws(IOException::class)
    fun rotateImageIfRequired(context: Context, img: Bitmap, selectedImage: Uri): Bitmap {
        val input: InputStream? = context.contentResolver.openInputStream(selectedImage)
        val path = selectedImage.path
        val ei: ExifInterface = when {
            input != null -> ExifInterface(input)
            path != null -> ExifInterface(path)
            else -> null
        } ?: return img
        return when (ei.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )) {
            // Rotate the image to the correct orientation
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90.0f) ?: img
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180.0f) ?: img
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270.0f) ?: img
            else -> img

        }
    }

    // Load a downsampled image
    fun decodeUriStreamToSize(
        uri: Uri, width: Int, height: Int, context: Context
    ): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val options: BitmapFactory.Options
            // Open an inputStream for the Uri
            inputStream = context.contentResolver.openInputStream(uri)
            // If it isn't null
            if (inputStream != null) {
                // Get the image size
                options = BitmapFactory.Options()
                options.inJustDecodeBounds = false
                BitmapFactory.decodeStream(inputStream, null, options)

                // Close the input stream
                inputStream.close()
                // Open an input stream
                inputStream = context.contentResolver.openInputStream(uri)

                // If the stream isn't null
                if (inputStream != null) {
                    // Load the image from the downsampling options
                    options.inSampleSize = calculateInSampleSize(
                        options.outWidth, options.outHeight, width, height
                    )
                    options.inJustDecodeBounds = false
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)

                    // Close the stream and return the bitmap
                    inputStream.close()
                    return bitmap
                }
            }
            // If we got null anywhere for the input stream, just return null
            return null

        } catch (e: Exception) {
            // Catch any exception, log it, and return null
            Log.e(TAG, "IO Exception!")
            return null

        } finally {
            // Always close the file
            inputStream?.close()
        }
    }
}