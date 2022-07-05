package com.stark.memorygame.view.screens.main

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.github.jinatonic.confetti.CommonConfetti
import com.github.jinatonic.confetti.ConfettiManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.CollectionReference
import com.squareup.picasso.Picasso
import com.stark.memorygame.R
import com.stark.memorygame.model.BoardSize
import com.stark.memorygame.model.BoardSize.Companion.getBoardSizeValue
import com.stark.memorygame.model.UserCustomGameImages
import com.stark.memorygame.databinding.ActivityMainBinding
import com.stark.memorygame.utils.FirebaseConstants.DOWNLOAD_CUSTOM_GAME_ERROR
import com.stark.memorygame.utils.FirebaseConstants.DOWNLOAD_CUSTOM_GAME_SUCCESS
import com.stark.memorygame.utils.FirebaseConstants.GAME_NAME
import com.stark.memorygame.view.adapter.MemoryCardClickListener
import com.stark.memorygame.view.adapter.MemoryGameAdapter
import com.stark.memorygame.view.common.DialogHelper
import com.stark.memorygame.view.custom_views.BoardSelectorDialog
import com.stark.memorygame.view.custom_views.OnBoardSizeSelectListener
import com.stark.memorygame.view.custom_views.OnDownloadGameListener
import com.stark.memorygame.view.extensions.createToast
import com.stark.memorygame.view.extensions.launchWithLifecycle
import com.stark.memorygame.view.extensions.showSnackBar
import com.stark.memorygame.view.intent.MemoryCardGameIntent
import com.stark.memorygame.view.screens.base.BaseActivity
import com.stark.memorygame.view.screens.custom_game.CustomGameActivity
import com.stark.memorygame.view.screens.custom_game.CustomGameActivity.Companion.CUSTOM_GAME_EXTRAS
import com.stark.memorygame.view.screens.custom_game.CustomGameActivity.Companion.GAME_NAME_EXTRA
import com.stark.memorygame.view.screens.custom_game.GameSharingState
import com.stark.memorygame.view.screens.registration.RegistrationActivity
import com.stark.memorygame.view.state.GameStatus
import com.stark.memorygame.view.state.MemoryCardGameState
import com.stark.memorygame.view.viewmodel.ViewModelFactory
import kotlinx.coroutines.*
import javax.inject.Inject

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var gameAdapter: MemoryGameAdapter
    private lateinit var vm: MainViewModel
    private var boardSelectorDialog: BoardSelectorDialog? = null
    private var menuSelectState: MenuSelectState = MenuSelectState.BOARD_SIZE
    private lateinit var confettiManager: ConfettiManager
    private val job: Job = SupervisorJob()

    companion object {
        private const val TAG = "MainActivity"
    }

    @Inject
    lateinit var viewModelFactory: ViewModelFactory

    private val customGameContract =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val gameName = result.data?.getStringExtra(GAME_NAME_EXTRA)
                if (gameName == null) {
                    createToast(getString(R.string.game_load_error), Toast.LENGTH_SHORT).show()
                    return@registerForActivityResult
                }
                downloadGame(gameName)
            }
        }


    private val downloadGameListener = object : OnDownloadGameListener {
        override fun onDownloadGame(gameName: String) {
            if (!isNetworkAvailable) {
                createToast(getString(R.string.network_error)).show()
                return
            }
            if (gameName.isBlank()) {
                createToast(getString(R.string.no_name_error), Toast.LENGTH_SHORT).show()
                return
            }
            try {
                downloadGame(gameName.lowercase())
            } catch (e: Exception) {
                firebaseAnalytics.logEvent(DOWNLOAD_CUSTOM_GAME_ERROR) {
                    param(GAME_NAME, gameName)
                }
                createToast(getString(R.string.download_game_error)).show()
            }
        }
    }

    private val boardSizeSelectListener by lazy {
        object : OnBoardSizeSelectListener {
            override fun onSelect(size: BoardSize) {
                gameName = null
                vm.setCustomCards(null)
                Log.i(TAG, "onSelect: boardsize = $size")
                if (menuSelectState != MenuSelectState.CUSTOM_NEW_GAME) {
                    vm.setBoardSize(size)
                    return
                }
                val intent = Intent(this@MainActivity, CustomGameActivity::class.java)
                intent.putExtra(CUSTOM_GAME_EXTRAS, size)
                customGameContract.launch(intent)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent.activityComponent().create().inject(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        vm = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
        initViews()
        updateGamePairs()
        updatedGameMoves()
        observers()
    }

    private fun downloadGameNames() {
        vm.userName?.let { userName ->
            val collection = db.collection(IMAGES_COLLECTION)
            downloadMyGamesOnly(collection, userName)
            collection.whereEqualTo("shareType", GameSharingState.EVERYONE.name).get()
                .addOnSuccessListener { doc ->
                    doc.forEachIndexed { index, doc ->
                        Log.i(TAG, "downloadGameNames: everyone = $doc.id")
                        vm.addGameNames(doc.id)
                    }
                }.addOnFailureListener {
                    Log.e(TAG, "Failed to download game list of share type everyone due to ", it )
                }

            collection.whereArrayContains("taggedUsers", userName)
                .get().addOnSuccessListener { doc ->
                    doc.forEachIndexed { _, doc ->
                        Log.i(TAG, "downloadGameNames: tagged users = $doc.id")
                        vm.addGameNames(doc.id)
                    }

                }.addOnFailureListener {
                    Log.e(TAG, "Failed to download game list of tagged users due to ", it )
                }

        }
    }

    private var gameName: String? = null
    private fun downloadGame(customGameName: String) {
        downloadGameNames()
        db.collection(IMAGES_COLLECTION).document(customGameName).get()
            .addOnSuccessListener { document ->
                val downloadedCustomImages = document.toObject(UserCustomGameImages::class.java)
                val images = downloadedCustomImages?.images
                Log.i(TAG, "downloadGame: $downloadedCustomImages")
                if (images == null) {
                    Log.e(TAG, "Failed to fetch $customGameName from firestore")
                    createToast(
                        String.format(
                            getString(
                                R.string.game_not_found_error,
                                customGameName
                            )
                        ), Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }
                val totalCards = images.size * 2
                vm.setCustomCards(images)
                cacheImages(images)
                gameName = customGameName
                firebaseAnalytics.logEvent(DOWNLOAD_CUSTOM_GAME_SUCCESS) {
                    param(GAME_NAME, customGameName)
                }
                vm.setBoardSize(getBoardSizeValue(totalCards))
            }.addOnFailureListener {
                Log.i(TAG, "downloadGame: failure")
                firebaseAnalytics.logEvent(DOWNLOAD_CUSTOM_GAME_ERROR) {
                    param(GAME_NAME, customGameName)
                }
                Log.e(TAG, "downloadGame failed ", it)
            }
    }

    private fun cacheImages(images: List<String>) {
        for (image in images) {
            Picasso.get().load(image).fetch()
        }
        binding.root.showSnackBar(getString(R.string.view_shrink_error), Snackbar.LENGTH_LONG)
            .show()
    }

    private fun updateGamePairs(pairs: Int = 0) {
        binding.tvMatchPairs.text = getString(
            R.string.total_pairs_text,
            pairs,
            vm.boardSize.getTotalPairs()
        )
    }

    private fun updatedGameMoves(moves: Int = 0) {
        binding.tvMoves.text = getString(R.string.moves_text, moves)
    }

    private fun initViews() {
        supportActionBar?.title =
            (gameName ?: getString(R.string.app_name)).replaceFirstChar { it.uppercaseChar() }
        gameAdapter = MemoryGameAdapter(this@MainActivity,
            vm.boardSize,
            vm.cards,
            object : MemoryCardClickListener {
                override fun onCardClick(position: Int) {
                    if (vm.isGameWon()) {
                        binding.root.showSnackBar(getString(R.string.game_already_won)).show()
                        return
                    }
                    if (vm.isCardAlreadyFaceUp(position)) {
                        binding.root.showSnackBar(getString(R.string.invalid_move)).show()
                        return
                    }
                    lifecycleScope.launch {
                        vm.userIntent.send(MemoryCardGameIntent.OnGameCardClick(position))
                    }
                }
            }
        )
        binding.rvBoard.apply {
            adapter = gameAdapter
            layoutManager = GridLayoutManager(this@MainActivity, vm.boardSize.getWidth())
            setHasFixedSize(true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                if (::confettiManager.isInitialized) confettiManager.terminate()
                if (vm.canPlay())
                    alertDialog()
                else
                    refresh()
            }

            R.id.mi_new_size -> {
                menuSelectState = MenuSelectState.BOARD_SIZE
                boardSelectorDialog = BoardSelectorDialog(
                    this,
                    onBoardSizeSelectListener = boardSizeSelectListener,
                    currentBoardSize = vm.boardSize,
                    title = getString(R.string.change_board_size_text),
                    resourceId = R.layout.dialog_board_size
                )
                return true
            }

            R.id.mi_custom -> {
                if (!vm.checkIfAccountCreated()) {
                    registerAccount()
                    return false
                }
                boardSelectorDialog = BoardSelectorDialog(
                    this,
                    onBoardSizeSelectListener = boardSizeSelectListener,
                    currentBoardSize = vm.boardSize,
                    title = getString(R.string.create_custom_game_title),
                    resourceId = R.layout.dialog_board_size
                )
                menuSelectState = MenuSelectState.CUSTOM_NEW_GAME

                return true
            }
            R.id.mi_download -> initDownload(vm.gameNames.toList())
            R.id.mi_list -> initDownload(vm.myGames.toList(), false)
            R.id.mi_about -> {
                firebaseAnalytics.logEvent("open_about", null)
                val aboutLink = remoteConfig.getString("about_link")
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(aboutLink)))
            }

            else -> super.onOptionsItemSelected(item)

        }
        return true
    }

    private fun initDownload(gameList: List<String>, isDownloadable: Boolean = true): Boolean {
        if (!vm.checkIfAccountCreated()) {
            registerAccount()
            return false
        }
        menuSelectState = MenuSelectState.DOWNLOAD_CUSTOM_GAME
        boardSelectorDialog = BoardSelectorDialog(
            this,
            onDownloadGameListener = downloadGameListener,
            title = getString(R.string.download_game),
            isGameDownloadable = isDownloadable,
            resourceId = R.layout.dialog_download_board,
            downloadGameList = gameList
        )
        return true
    }

    private fun registerAccount() {
        startActivity(
            Intent(this, RegistrationActivity::class.java)
        )
        finish()
    }

    private fun refresh() {
        lifecycleScope.launch {
            vm.userIntent.send(MemoryCardGameIntent.OnRefresh)
        }
    }

    private fun alertDialog() {
        val dialogHelper = DialogHelper.Dialog(
            this,
            title = getString(R.string.alert_title),
            message = getString(R.string.game_restart_message),
            negativeBtnText = getString(R.string.no),
            positionBtnText = getString(R.string.yes),
            onPositiveClick = {
                refresh()
            }
        )
        DialogHelper.AlertDialogBuilder(dialogHelper).build()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observers() {
        launchWithLifecycle {
            launch {
                vm.state.collect {
                    when (it) {
                        is MemoryCardGameState.Idle -> {
                        }
                        is MemoryCardGameState.Loading -> {}
                        is MemoryCardGameState.CardStateChange -> {
                            gameAdapter.notifyDataSetChanged()
                            if (vm.isGameWon()) {
                                confettiManager = CommonConfetti.rainingConfetti(
                                    binding.clRoot,
                                    intArrayOf(Color.YELLOW, Color.BLUE, Color.MAGENTA)
                                ).oneShot()
                                binding.root.showSnackBar(getString(R.string.game_won_message))
                                    .show()
                            }
                        }

                        is MemoryCardGameState.OnDownloadGameNames -> {
                            downloadGameNames()
                        }

                        is MemoryCardGameState.OnGameReset -> {
                            initViews()
                            updateGamePairs()
                            updatedGameMoves()
                            gameAdapter.notifyDataSetChanged()
                        }

                        is MemoryCardGameState.Error -> {
                            binding.root.showSnackBar(it.error ?: "Unknown Error").show()
                        }
                    }
                }
            }

            launch {
                vm.gameStatusState.collect {
                    when (it) {
                        is GameStatus.Idle -> {}
                        is GameStatus.MovesAndPairs -> {
                            updatedGameMoves(it.moves)
                            updateGamePairs(it.pairs)
                        }
                    }
                }
            }
        }
    }

    private fun downloadMyGamesOnly(collection: CollectionReference, userName: String) {
        collection.whereEqualTo("creator", userName)
            .get()
            .addOnSuccessListener {
                it.documents.forEachIndexed { _, doc ->
                    Log.i(TAG, "downloadGameNames: creator = $doc.id")
                    vm.addGameNames(doc.id)
                    vm.mygames(doc.id)
                }
            }.addOnFailureListener {
                Log.e(TAG, "Failed to download game list of creators due to ", it )
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        boardSelectorDialog = null
    }

    override fun onBackPressed() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.alert_title))
            .setMessage(getString(R.string.quite_game_message))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                finishAfterTransition()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.cancel()
            }.show()
    }
}

enum class MenuSelectState {
    BOARD_SIZE, CUSTOM_NEW_GAME, DOWNLOAD_CUSTOM_GAME, CREATE_ACCOUNT
}