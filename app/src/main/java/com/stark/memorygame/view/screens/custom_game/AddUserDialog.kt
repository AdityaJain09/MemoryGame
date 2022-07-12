package com.stark.memorygame.view.screens.custom_game

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.stark.memorygame.R
import com.stark.memorygame.databinding.DialogAddUserBinding
import com.stark.memorygame.di.scopes.ActivityScope

class AddUserDialog(
    private val savedUsers: Set<String>? = null,
    private val onCheckingInternetAvailability: OnCheckingInternetAvailability,
    private val onSaveItemListener: OnSaveItemListener,
) : DialogFragment(), View.OnClickListener {

    private var _binding: DialogAddUserBinding? = null
    private val binding get() = _binding!!
    private var db: FirebaseFirestore? = null
    private var chip: Chip ? = null
    private val users: MutableSet<String> by lazy { mutableSetOf() }

    override fun getTheme(): Int {
        return R.style.fullscreen_theme
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = DialogAddUserBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        savedUsers?.forEach(this::onSearchCompleted)
        binding.addUserBtn.setOnClickListener {
            val userName = binding.searchSv.text.toString()
            if (userName.isBlank()) {
                binding.searchSv.error = getString(R.string.enter_user_name_error)
                return@setOnClickListener
            }
            binding.searchPb.visibility = View.VISIBLE
            search(userName)
        }

        binding.continueBtn.setOnClickListener {
            onSaveItemListener.onSave(users)
            dismiss()
        }

    }

    private fun search(username: String) {
        db = Firebase.firestore
        if (onCheckingInternetAvailability.isInternetAvailable()) {
            db?.let { firebaseDb ->
                firebaseDb.collection("users").document(username).get()
                    .addOnSuccessListener { doc ->
                        val isUserFound = doc != null && doc.data != null
                        if (isUserFound)
                            onSearchCompleted(username)
                        else
                            onSearchCompleted(null)
                    }.addOnSuccessListener {

                }
            }
        } else {
            binding.searchPb.visibility = View.GONE
            Toast.makeText(view?.context, getString(R.string.network_error), Toast.LENGTH_SHORT).show()
        }
    }

    private fun onSearchCompleted(username: String?) {
        if (users.contains(username)) {
            binding.searchPb.visibility = View.GONE
            binding.searchSv.error = getString(R.string.username_already_added_error)
            return
        }

        if (username == null) {
            binding.searchPb.visibility = View.GONE
            binding.searchSv.error = getString(R.string.username_not_found_error)
            return
        }
        users.add(username)
        chip = Chip(view?.context)
        chip?.apply {
            text = username
            isCloseIconVisible = true
            setTextColor(Color.GRAY)
            setTextAppearance(R.style.ChipTextAppearance)
        }
        binding.searchPb.visibility = View.GONE
        binding.userTagContainer.addView(chip)
        chip?.setOnCloseIconClickListener(this)
    }

    override fun onClick(v: View?) {
        val chip = v as Chip
        users.remove(chip.text)
        binding.userTagContainer.removeView(chip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        db = null
        _binding = null
    }
}


interface OnCheckingInternetAvailability {
    fun isInternetAvailable(): Boolean
}

interface OnSaveItemListener {
    fun onSave(users: Set<String>)
}