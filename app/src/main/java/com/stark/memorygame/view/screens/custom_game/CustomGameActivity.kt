package com.stark.memorygame.view.screens.custom_game

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputFilter
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.stark.memorygame.R
import com.stark.memorygame.model.BoardSize
import com.stark.memorygame.databinding.ActivityCustomGameBinding
import com.stark.memorygame.utils.FirebaseConstants
import com.stark.memorygame.utils.FirebaseConstants.UPLOAD_CUSTOM_GAME_SUCCESS
import com.stark.memorygame.utils.ImageScaler
import com.stark.memorygame.utils.hasPermission
import com.stark.memorygame.view.adapter.CustomGameAdapter
import com.stark.memorygame.view.adapter.ImageClickListener
import com.stark.memorygame.view.extensions.createToast
import com.stark.memorygame.view.screens.base.BaseActivity
import com.stark.memorygame.view.state.CustomGameState
import com.stark.memorygame.view.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class CustomGameActivity : BaseActivity() {

    private lateinit var boardSize: BoardSize
    private lateinit var binding: ActivityCustomGameBinding
    private lateinit var customGameAdapter: CustomGameAdapter
    private val images: MutableList<Uri> by lazy { mutableListOf() }
    private lateinit var vm: CustomGameViewModel
    private val storage = Firebase.storage

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    @SuppressLint("NotifyDataSetChanged")
    private val startImageChooseIntent =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result == null || result.resultCode != RESULT_OK || result.data == null) {
                createToast(getString(R.string.photo_picking_error), Toast.LENGTH_SHORT).show()
                return@registerForActivityResult
            }
            val clipData = result.data!!.clipData
            Log.i(TAG, "data = $clipData and itemsCount = ${clipData?.itemCount} ")
            val selectedUri = result.data!!.data
            clipData?.run {
                for (i in 0 until itemCount) {
                    val clipItem = getItemAt(i)
                    if (images.size < boardSize.getTotalPairs()) {
                        images.add(clipItem.uri)
                    }
                }
            } ?: selectedUri?.let {
                // In some mobiles selecting multiple imageUrl is not possible, In that case this block will run.
                images.add(it)
            }
            customGameAdapter.notifyDataSetChanged()
            supportActionBar?.title = getString(R.string.photo_choose_title, images.size, boardSize.getTotalPairs())
            binding.btnSave.isEnabled = shouldEnableSaveBtn()

        }

    private fun shouldEnableSaveBtn(): Boolean {
        val gameName = binding.etGameName.text.toString()
        return boardSize.getTotalPairs() == images.size && (gameName.isNotBlank() && gameName.length > MIN_GAME_NAME_LENGTH)
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    { isGranted ->
        if (isGranted)
            launchPhotosChooseIntent()
        else
            createToast(getString(R.string.read_write_permission_error)).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.activityComponent().create().inject(this)
        super.onCreate(savedInstanceState)
        binding = ActivityCustomGameBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this, viewModelFactory)[CustomGameViewModel::class.java]
        setMaxGameLength(remoteConfig.getLong("custom_game_max_length").toInt())
        initViews()
        clickListener()
        observers()
    }

    private fun observers() {
        binding.etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        binding.etGameName.doAfterTextChanged {
            binding.btnSave.isEnabled = shouldEnableSaveBtn()
        }

        lifecycleScope.launch {
            vm.customGameState.collect { state ->
                when (state) {
                    is CustomGameState.SaveGame -> {
                        binding.btnSave.isEnabled = boardSize.getTotalPairs() == images.size
                    }

                    is CustomGameState.Error -> {
                        binding.btnSave.isEnabled = false
                        binding.etGameName.error = state.error ?: getString(R.string.game_creation_error)
                    }
                    CustomGameState.Idle -> {}
                }
            }
        }
    }

    private fun clickListener() {
        binding.btnSave.setOnClickListener {
            if (!isNetworkAvailable) {
                createToast(getString(R.string.network_error)).show()
                return@setOnClickListener
            }
            val gameName = binding.etGameName.text.toString()
            // check if game name already taken
            binding.btnSave.isEnabled = false
            db.collection(DATABASE_NAME).document(gameName).get().addOnSuccessListener { doc ->
                if (doc != null && doc.data != null) {
                    AlertDialog.Builder(this)
                        .setTitle(getString(R.string.duplicate_text))
                        .setMessage(getString(R.string.game_name_conflict, gameName))
                        .setPositiveButton(getString(R.string.ok), null)
                        .show()
                    binding.btnSave.isEnabled = true
                } else {
                    try {
                        handleImageUploading(gameName)
                    } catch (e: Exception) {
                        Log.i(TAG, "clickListener: Failed to upload image due to ${e.message}")
                        createToast("Unknown Error").show()
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch documents", exception)
                createToast(getString(R.string.server_error), Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
            }
        }
    }

    private fun handleImageUploading(gameName: String) {
        binding.pbUploading.visibility = View.VISIBLE
        var uploadingTaskError = false
        val uploadedUrls = mutableListOf<String>()
        for ((index, photoUri) in images.withIndex()) {
            Log.i(TAG, "handleImageUploading: $photoUri")
            val imageByteArray = decodeImageToByteArray(photoUri)
            val filePath = "$ROOT_COLLECTION_NAME/${gameName}/${System.currentTimeMillis()}-${index}.jpg"
            val storageReference = storage.reference.child(filePath)
            storageReference.putBytes(imageByteArray).continueWithTask { task ->
                Log.i(TAG, "Bytes Uploaded = ${task.result.bytesTransferred}")
                if (!task.isSuccessful) {
                    disableScreenElements()
                    task.exception?.let {
                        throw it
                    }
                }
                storageReference.downloadUrl
            }.addOnCompleteListener { downloadTask ->
                if (!downloadTask.isSuccessful) {
                    disableScreenElements()
                    Log.e(
                        TAG,
                        "Exception while uploading imageUrl to firebasce",
                        downloadTask.exception
                    )
                    if (!isNetworkAvailable) {
                        createToast(getString(R.string.network_error)).show()
                        return@addOnCompleteListener
                    }
                    createToast(getString(R.string.image_uploading_error)).show()
                    uploadingTaskError = true
                    binding.btnSave.isEnabled = true
                    return@addOnCompleteListener
                }

                if (uploadingTaskError) {
                    disableScreenElements()
                    return@addOnCompleteListener
                }

                val downloadUrl = downloadTask.result.toString()
                uploadedUrls.add(downloadUrl)
                binding.pbUploading.progress = uploadedUrls.size * 100 / images.size
                Log.i(TAG, "So far uploaded Images = ${uploadedUrls.size}")
                if (uploadedUrls.size == images.size) {
                    manageAllUploadedImages(gameName, uploadedUrls)
                }
            }
        }
    }

    private fun disableScreenElements() {
        binding.btnSave.isEnabled = false
        binding.pbUploading.visibility = View.GONE
    }

    private fun manageAllUploadedImages(gameName: String, uploadedImages: MutableList<String>) {
        db.collection(DATABASE_NAME).document(gameName).set(mapOf(ROOT_COLLECTION_NAME to uploadedImages))
            .addOnCompleteListener { uploadDocumentTask ->
                binding.pbUploading.visibility = View.GONE
                if (!uploadDocumentTask.isSuccessful) {
                    uploadDocumentTask.exception?.let {
                        Log.e(TAG, "Failed to create game", uploadDocumentTask.exception)
                        createToast(getString(R.string.game_creation_error)).show()
                    }
                    binding.btnSave.isEnabled = true
                    return@addOnCompleteListener
                }
                firebaseAnalytics.logEvent(UPLOAD_CUSTOM_GAME_SUCCESS) {
                    param(FirebaseConstants.GAME_NAME, gameName)
                }
                Log.i(TAG, "Successfully uploaded game...")
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.uploaded_message_info, gameName))
                    .setPositiveButton(getString(R.string.ok)) { _, _ ->
                        val resultData = Intent()
                        resultData.putExtra(GAME_NAME_EXTRA, gameName)
                        setResult(RESULT_OK, resultData)
                        finishAfterTransition()
                    }.show()
            }
    }

    private fun decodeImageToByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        val scaledImage = ImageScaler.scaleToFitHeight(originalBitmap, remoteConfig.getLong("scaled_height").toInt())
        val byteArrayOutputStream = ByteArrayOutputStream()
        scaledImage.compress(Bitmap.CompressFormat.JPEG, remoteConfig.getLong("compress_quality").toInt(), byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray()
    }

    private fun initViews() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(CUSTOM_GAME_EXTRAS) as BoardSize
        supportActionBar?.title = getString(R.string.photo_choose_title, 0, boardSize.getTotalPairs())
        customGameAdapter = CustomGameAdapter(
            this,
            images,
            boardSize,
            object : ImageClickListener {
                override fun onClickImagePlaceHolder() {
                    Log.i(TAG, "has Permission: ${Build.VERSION.SDK_INT}")
                    if (hasPermission(this@CustomGameActivity, READ_EXTERNAL_STORAGE)) {
                        launchPhotosChooseIntent()
                    } else {
                        requestPermission()
                    }
                }
            }
        )
        binding.rvImagePicker.apply {
            adapter = customGameAdapter
            layoutManager = GridLayoutManager(this@CustomGameActivity, boardSize.getWidth())
            setHasFixedSize(true)
        }

    }

    private fun requestPermission() {
        requestPermission.launch(READ_EXTERNAL_STORAGE)
    }

    private fun launchPhotosChooseIntent() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startImageChooseIntent.launch(Intent.createChooser(intent, "Choose Pics"))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    val abc = ""

    companion object {
        const val CUSTOM_GAME_EXTRAS = "custom_game_board_size_extras"
        private const val TAG = "CustomGameActivity"
        private const val READ_EXTERNAL_STORAGE = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val MIN_GAME_NAME_LENGTH = 3
        private var MAX_GAME_NAME_LENGTH: Int = 0
        const val GAME_NAME_EXTRA = "custom_game_name"
        
        fun setMaxGameLength(length: Int?) {
            MAX_GAME_NAME_LENGTH = if (length == null || length < 1) 15 else length
        }
    }
}