package com.extempo.typescan.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.extempo.typescan.model.repository.AuthorRepository
import com.extempo.typescan.model.repository.DocumentRepository

@Suppress("UNCHECKED_CAST")
class TextEditorActivityViewModelFactory(private val documentRepository: DocumentRepository, private val authorRepository: AuthorRepository): ViewModelProvider.NewInstanceFactory() {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TextEditorActivityViewModel(documentRepository, authorRepository) as T
    }
}