package com.ant.placebook.utils

import android.content.Context
import java.io.File

object FileUtils {

    // Delete a file
    fun deleteFile(context: Context, filename: String) {
        val dir = context.filesDir
        val file = File(dir, filename)
        file.delete()
    }

}
