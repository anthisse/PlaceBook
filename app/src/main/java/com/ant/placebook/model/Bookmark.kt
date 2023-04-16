package com.ant.placebook.model

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    var phone: String = ""
)