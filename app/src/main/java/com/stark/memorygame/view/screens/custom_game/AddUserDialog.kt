package com.stark.memorygame.view.screens.custom_game

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.stark.memorygame.R


class AddUserDialog(
    private val ctx: Context,
    private val fullscreenTheme: Int,
    private val savedUsers: Set<String>? = null,
    private var onSearchingUserListener: OnSearchingUserListener? = null,
    private val onAddingAllUsers: (users: Set<String>?) -> Unit
) : DialogFragment(fullscreenTheme), View.OnClickListener {

    private lateinit var searchEt: EditText
    private lateinit var continueBtn: Button
    private lateinit var addBtn: ImageButton
    private lateinit var chipGroup: ChipGroup
    private lateinit var searchProgressBar: ProgressBar
    private val users: MutableSet<String> by lazy { mutableSetOf() }

    companion object {
        private const val MAX_TAG_LIMIT = 15
        private const val TAGS = "selected_users"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, fullscreenTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_user, container, false)
        searchEt = view.findViewById(R.id.search_sv)
        continueBtn = view.findViewById(R.id.continue_btn)
        chipGroup = view.findViewById(R.id.user_tag_container)
        addBtn = view.findViewById(R.id.add_user_btn)
        searchProgressBar = view.findViewById(R.id.search_pb)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedUsers?.forEach(this::onSearchCompleted)
        addBtn.setOnClickListener {
            val userName = searchEt.text.toString()
            if (userName.isBlank()) {
                searchEt.error = getString(R.string.enter_user_name_error)
                return@setOnClickListener
            }
            searchProgressBar.visibility = View.VISIBLE
            if (chipGroup.childCount == MAX_TAG_LIMIT) {
                Snackbar.make(view, "Cannot add more users", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            onSearchingUserListener?.onSearch(userName)
        }


        continueBtn.setOnClickListener {
            onAddingAllUsers(users.toSet())
            users.clear()
            dismiss()
        }
    }

    fun onSearchCompleted(username: String?) {

        if (users.contains(username)) {
            searchProgressBar.visibility = View.GONE
            searchEt.error = getString(R.string.username_already_added_error)
            return
        }

        if (username == null) {
            searchProgressBar.visibility = View.GONE
            searchEt.error = getString(R.string.username_not_found_error)
            return
        }
        users.add(username)
        val chip = Chip(ctx)
        chip.apply {
            text = username
            isCloseIconVisible = true
            setTextColor(Color.GRAY)
            setTextAppearance(R.style.ChipTextAppearance)
        }
        searchProgressBar.visibility = View.GONE
        chipGroup.addView(chip)
        chip.setOnCloseIconClickListener(this)
    }

    override fun onClick(v: View?) {
        val chip = v as Chip
        users.remove(chip.text)
        chipGroup.removeView(chip)
    }

    override fun onDestroy() {
        super.onDestroy()
        onSearchingUserListener = null
    }
}

interface OnSearchingUserListener {
    fun onSearch(username: String)
}