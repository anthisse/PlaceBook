package com.ant.placebook.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ant.placebook.model.Bookmark

// Database PlaceBookDatabase
@Database(entities = arrayOf(Bookmark::class), version = 2)
abstract class PlaceBookDatabase : RoomDatabase() {

    // Return an instance of a DAO interface
    abstract fun bookmarkDao(): BookmarkDao

    // Companion object for PlaceBookDatabase
    companion object {
        private var instance: PlaceBookDatabase? = null

        // Function getInstance(). Takes a Context and returns a PlaceBookDatabase
        fun getInstance(context: Context): PlaceBookDatabase {
            // If this is the first call of getInstance
            if (instance == null) {
                // Create a Room Database
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlaceBookDatabase::class.java,
                    "PlaceBook").fallbackToDestructiveMigration().build()
            }
            // Return the PlaceBookDatabase
            return instance as PlaceBookDatabase
        }
    }
}