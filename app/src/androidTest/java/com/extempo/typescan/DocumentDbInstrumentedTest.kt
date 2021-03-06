package com.extempo.typescan

import android.app.Activity
import androidx.test.*
import androidx.test.runner.*
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.extempo.typescan.model.Author
import com.extempo.typescan.model.database.DocumentDatabase
import com.extempo.typescan.model.repository.AuthorRepository

import org.junit.Test

import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DocumentDbInstrumentedTest {
    val context = InstrumentationRegistry.getTargetContext()

    @Test
    fun insertDocumentItemSavesAndRetrievesData() {
        DocumentDatabase.getInstance(context).documentItemDao().deleteAllDocumentItems()
        repeat(10) {
            DocumentDatabase.getInstance(context).documentItemDao()
                .insertDocument(DocumentFactory.makeDocumentItem())
        }
        val docs = DocumentDatabase.getInstance(context).documentItemDao().getAllDocumentItems()
        println(docs)
    }

    @Test
    fun testingAuthorDao() {
        val name = "Aayush Sharma"
        val author = Author(name)
        AuthorRepository(context).insertAuthor(author)
        println("WOW MAN WHAT A COMMENT: " + AuthorRepository(context).getAllAuthors())
    }
}
