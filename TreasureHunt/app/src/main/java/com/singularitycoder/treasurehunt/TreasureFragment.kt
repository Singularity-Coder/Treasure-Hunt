package com.singularitycoder.treasurehunt

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.singularitycoder.treasurehunt.databinding.FragmentTreasureBinding
import java.io.File


class TreasureFragment : Fragment() {

    companion object {
        @JvmStatic
        fun newInstance(tab: String) = TreasureFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PARAM_TAB, tab)
            }
        }
    }

    private var shareState: String? = null

    private lateinit var binding: FragmentTreasureBinding

    private val treasuresAdapter = TreasuresAdapter()
    private var duplicateTreasureList = mutableListOf<Treasure>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        shareState = arguments?.getString(ARG_PARAM_TAB, "") ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTreasureBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.setupUI()
        binding.setupUserActionListeners()
    }

    private fun FragmentTreasureBinding.setupUI() {
        rippleView.startRippleAnimation()
        if (shareState == Tab.EXPLORE.value) {
            rippleView.isVisible = true
            cardSearch.isVisible = false
            cardAddTreasureParent.isVisible = false
        } else {
            rippleView.isVisible = false
        }
        rvFlukes.apply {
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            itemAnimator = DefaultItemAnimator()
            adapter = treasuresAdapter
        }
        doAfter(5000L) {
            rippleView.stopRippleAnimation()
            rippleView.isVisible = false
            treasuresAdapter.treasureList = mutableListOf(
                Treasure(
                    "Legendary Pokemon: HakuTakuPaku",
                    "image.png"
                ),
                Treasure(
                    "If 5 is five then what will you get with 5000 - 7000 + 3000 time shuunya.",
                    "video.mp4"
                ),
                Treasure(
                    "I plundered all the world to make a point. And that is what?",
                    "audio.mp3"
                ),
                Treasure(
                    "Secret UFO tech.",
                    "document.pdf"
                ),
                Treasure(
                    "Original Ayurveda Shastra.",
                    "document.djvu"
                ),
                Treasure(
                    "Custom made gambling App that lets you earn a trillion dollars.",
                    "document.app"
                ),
                Treasure(
                    "Death Note. After Light died I found the book. I got scared so I am waiting for a worthy user.",
                    "document.apk"
                ),
                Treasure(
                    "A super malware capable of taking down any stock market!",
                    "document.java"
                ),
                Treasure(
                    "A funny picture of my cat talking in klingon. It said an I...",
                    "document.webp"
                ),
                Treasure(
                    "hello world!",
                    "document.kt"
                )
            )
            treasuresAdapter.notifyDataSetChanged()
        }
    }

    private fun FragmentTreasureBinding.setupUserActionListeners() {
        etSearch.doAfterTextChanged { keyWord: Editable? ->
            ibClearSearch.isVisible = keyWord.isNullOrBlank().not()
            if (keyWord.isNullOrBlank()) {
                treasuresAdapter.treasureList = duplicateTreasureList
            } else {
                treasuresAdapter.treasureList = treasuresAdapter.treasureList.filter { it: Treasure -> it.title.contains(keyWord) }.toMutableList()
            }
            treasuresAdapter.notifyDataSetChanged()
        }
        ibClearSearch.setOnClickListener {
            etSearch.setText("")
        }
        treasuresAdapter.setItemClickListener { treasure, position ->
            showFileInBrowser(treasure)
        }
    }

    // https://stackoverflow.com/questions/38200282/android-os-fileuriexposedexception-file-storage-emulated-0-test-txt-exposed
    private fun showFileInBrowser(treasure: Treasure) {
        // show in chrome browser
        val intent = Intent(Intent.ACTION_VIEW, File(treasure.filePath).toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.android.chrome")
        }
        try {
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            // If Chrome not installed
            intent.setPackage(null)
            startActivity(intent)
        }
    }
}

private const val ARG_PARAM_TAB = "ARG_PARAM_TAB"
