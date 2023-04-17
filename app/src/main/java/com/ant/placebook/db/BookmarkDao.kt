package com.ant.placebook.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.OnConflictStrategy.REPLACE
import androidx.room.Query
import androidx.room.Update
import com.ant.placebook.model.Bookmark

// Data access object BookmarkDao
@Dao
interface BookmarkDao {

    // Read all bookmarks and return them as a List
    @Query("SELECT * FROM Bookmark")
    fun loadAll(): LiveData<List<Bookmark>>

    // Return a single Bookmark matching an id
    @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
    fun loadBookmark(bookmarkId: Long): Bookmark

    // Return a single Bookmark matching an id, wrapped with LiveData
    @Query("SELECT * FROM Bookmark WHERE id = :bookmarkId")
    fun loadLiveBookmark(bookmarkId: Long): LiveData<Bookmark>

    // Insert a bookmark
    // Since auto-generated keys are being used, there's no concern for a conflict in id
    @Insert(onConflict = IGNORE)
    fun insertBookmark(bookmark: Bookmark): Long

    // Update a bookmark
    @Update(onConflict = REPLACE)
    fun updateBookmark(bookmark: Bookmark)

    // Delete a bookmark
    @Delete
    fun deleteBookmark(bookmark: Bookmark)
}