package com.ant.placebook.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.ant.placebook.db.BookmarkDao
import com.ant.placebook.db.PlaceBookDatabase
import com.ant.placebook.model.Bookmark

// BookmarkRepo class. Accepts a Context object
class BookmarkRepo(context: Context) {
    private val db = PlaceBookDatabase.getInstance(context)
    private val bookmarkDao: BookmarkDao = db.bookmarkDao()

    // Allow one Bookmark to be added to the repo. Returns the value of the unique id
    fun addBookmark(bookmark: Bookmark): Long? {
        val newId = bookmarkDao.insertBookmark(bookmark)
        bookmark.id = newId
        return newId
    }

    // Return a freshly initialized Bookmark
    fun createBookmark(): Bookmark {
        return Bookmark()
    }

    // Return a LiveData<List> of all Bookmarks
    val allBookmarks: LiveData<List<Bookmark>>
    get() {
        return bookmarkDao.loadAll()
    }
}