package com.extempo.typescan.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextServicesManager
import android.widget.*
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.extempo.typescan.R
import com.extempo.typescan.adapter.AuthorSpinnerAdapter
import com.extempo.typescan.databinding.ActivityTextEditorBinding
import com.extempo.typescan.model.Author
import com.extempo.typescan.model.AuthorResult
import com.extempo.typescan.model.DocumentItem
import com.extempo.typescan.utilities.InjectorUtils
import com.extempo.typescan.viewmodel.TextEditorActivityViewModel
import kotlinx.android.synthetic.main.activity_text_editor.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.ref.WeakReference
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class TextEditorActivity : AppCompatActivity(), SpellCheckerSession.SpellCheckerSessionListener {
    override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
        results.let { resultSuggestions ->
            for(i in 0 until resultSuggestions!!.size) {
                for(j in 0 until resultSuggestions[i].suggestionsCount) {
                    for(k in 0 until resultSuggestions[i].getSuggestionsInfoAt(j).suggestionsCount) {
                        println("suggestions: " + resultSuggestions[i].getSuggestionsInfoAt(j).getSuggestionAt(k))
                        println("suggestions: " + ".")
                    }
                    println("suggestions: " + "-")
                }
                println("suggestions: " + "*")
            }
        }
    }

    override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {
    }

    var binding: ActivityTextEditorBinding? = null
    var viewModel: TextEditorActivityViewModel? = null
    private var isNew: Boolean? = null
    private var session: SpellCheckerSession? = null
    private var authorList: ArrayList<AuthorResult> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_editor)
        initializeSpellChecker()
    }

    private fun initializeUI() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_text_editor)
        val factory = InjectorUtils.provideTextEditorActivityViewModelFactory(this)
        viewModel = ViewModelProviders.of(this, factory).get(TextEditorActivityViewModel::class.java)

        isNew = intent.getBooleanExtra(HomeActivity.TEXT_EDITOR_NEW, false)

        if (isNew!!) {
            val result = intent.getParcelableExtra<Uri>(HomeActivity.TEXT_EDITOR_DATA)
            Glide.with(this)
                .asBitmap()
                .load(result)
                .into(object : CustomTarget<Bitmap>(){
                    @SuppressLint("RestrictedApi")
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        this@TextEditorActivity.session?.let { session ->
                            val data = viewModel?.runInference(resource, this@TextEditorActivity, WeakReference(this@TextEditorActivity.session!!))
                            data?.observe(this@TextEditorActivity, Observer { dataList ->
                                var dataString = ""
                                dataList.forEach { dataString += "$it\n" }
                                binding?.documentData = dataString
                            })
                            viewModel?.documentItem = DocumentItem("", "")
                            binding?.documentItem = viewModel?.documentItem
                            var max = 0.0
                            var author = Author("")
                            viewModel?.charImageArray?.forEach { author.charactermap[it.character]?.add(it.mat) }
                            viewModel?.getAllAuthors()?.observe(this@TextEditorActivity, Observer {
                                it.forEach{ a->
                                    println("log_tag: author: $a")
                                    val temp = a.compare(author.charactermap)
                                    println("log_tag temp: $temp")
                                    authorList.add(AuthorResult(a, temp))
                                    if (temp > max) {
                                        max = temp
                                        author.name = a.name
                                    }
                                }
                                println("log_tag: ${author.name}, val: $max")
                                println("log_tag: array size: " + authorList.size)
                                val spinner: Spinner = findViewById(R.id.text_editor_author_list_spinner)
                                val spinnerAdapter = AuthorSpinnerAdapter(this@TextEditorActivity, authorList)
                                spinner.adapter = spinnerAdapter
                            })
                        }
                    }
                    override fun onLoadCleared(placeholder: Drawable?) {

                    }
                })
        } else {
            var dataString = ""
            val documentItem = intent.getSerializableExtra(HomeActivity.TEXT_EDITOR_DOCUMENT_ITEM) as DocumentItem
            viewModel?.documentItem = documentItem
            binding?.documentItem = viewModel?.documentItem
            if (this.filesDir.exists()) {
                try {
                    val file = File(this.filesDir, documentItem.filename + ".txt")
                    val fileInputStream = FileInputStream(file)
                    val inputStreamReader = InputStreamReader(fileInputStream)

                    val dataList = inputStreamReader.readLines()

                    dataList.forEach {data->
                        dataString += data
                    }
                    binding?.documentData = dataString
                    viewModel?.textList  = dataList as ArrayList<String>
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            } else {
                Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show()
            }
        }
        initializeListeners()
    }

    private fun initializeListeners() {
        text_editor_save_button.setOnClickListener {
            val dataList = ArrayList<String>()
            val tokens = StringTokenizer(text_editor_content_text.text.toString(), "\n\r")
            while(tokens.hasMoreTokens()) {
                dataList.add(tokens.nextToken())
            }
            viewModel?.textList = dataList
            viewModel?.documentItem?.let {docItem->
                if (text_editor_author.text.toString().isNotBlank() && text_editor_title.text.toString().isNotBlank()) {
                    viewModel?.textList?.let {textList->
                        viewModel?.author = Author(text_editor_author.text.toString())
                        docItem.author = text_editor_author.text.toString()
                        docItem.title = text_editor_title.text.toString()
                        docItem.generateFilename()
                        binding?.documentItem = docItem
                        if (isNew!!) {
                            viewModel?.insertDocumentItem(docItem, textList).also {
                                viewModel?.insertAuthor()
                                finish()
                            }
                        } else {
                            viewModel?.updateDocumentItem(docItem, textList).also {
                                finish()
                            }
                        }
                    }
                }
            }
        }

        text_editor_author_list_spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                println("log_tag: viewmodel: " + viewModel)
                println("log_tag: docitem: " + viewModel?.documentItem)
                viewModel?.documentItem?.let { documentItem ->
                    println("Selected: " + authorList[position].author.name)
                    documentItem.author = authorList[position].author.name
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                return
            }
        }

        text_editor_cancel_button.setOnClickListener {
            finish()
        }
    }

    private fun initializeSpellChecker() {
        val tsm: TextServicesManager = getSystemService(Context.TEXT_SERVICES_MANAGER_SERVICE) as TextServicesManager
        this.session = tsm.newSpellCheckerSession(null, Locale.ENGLISH, this, false).also {
            initializeUI()
        }
    }

    companion object {
        @JvmStatic
        @BindingAdapter("bind:editableText")
        fun loadDocumentContent(et: EditText, data: String?) {
            data?.let {
                et.setText(it.toLowerCase(Locale.ENGLISH), TextView.BufferType.EDITABLE)
            }
        }

        @JvmStatic
        @BindingAdapter("bind:author")
        fun loadAuthor(et: EditText, data: DocumentItem?) {
            data?.let {
                et.setText(it.author, TextView.BufferType.EDITABLE)
            }
        }

        @JvmStatic
        @BindingAdapter("bind:title")
        fun loadTitle(et: EditText, data: DocumentItem?) {
            data?.let {
                et.setText(it.title, TextView.BufferType.EDITABLE)
            }
        }
    }
}
