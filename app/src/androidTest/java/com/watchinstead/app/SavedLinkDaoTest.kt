package com.watchinstead.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.watchinstead.app.data.local.WatchInsteadDatabase
import com.watchinstead.app.data.local.SavedLinkDao
import com.watchinstead.app.data.local.SavedLinkEntity
import com.watchinstead.app.domain.Category
import com.watchinstead.app.domain.Platform
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedLinkDaoTest {
    private lateinit var database: WatchInsteadDatabase
    private lateinit var dao: SavedLinkDao

    private fun entity(url: String) = SavedLinkEntity(
        url = url, platform = Platform.OTHER, category = Category.OTHER, note = "", title = null,
        thumbnailUrl = null, createdAt = 1, watchedAt = null, lastShownAt = null, metadataAttemptedAt = null,
    )

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WatchInsteadDatabase::class.java).build()
        dao = database.savedLinkDao()
    }

    @After fun tearDown() = database.close()

    @Test fun duplicateUrlIsIgnored() = runTest {
        dao.insert(entity("a"))
        assertEquals(-1L, dao.insert(entity("a")))
    }

    @Test fun watchedLinksLeaveTheQueueAndCanBeCleared() = runTest {
        val id = dao.insert(entity("a"))
        dao.insert(entity("b"))
        dao.setWatchedAt(id, 5)
        assertEquals(listOf("b"), dao.unwatched().map { it.url })
        dao.deleteWatched()
        assertEquals(1, dao.unwatched().size)
    }
}
