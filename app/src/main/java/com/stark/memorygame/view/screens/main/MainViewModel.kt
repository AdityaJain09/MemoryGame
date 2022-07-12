package com.stark.memorygame.view.screens.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stark.memorygame.data.UserDataSource
import com.stark.memorygame.model.BoardSize
import com.stark.memorygame.model.MemoryCard
import com.stark.memorygame.utils.Constants
import com.stark.memorygame.view.custom_views.GameType
import com.stark.memorygame.view.intent.MemoryCardGameIntent
import com.stark.memorygame.view.state.GameStatus
import com.stark.memorygame.view.state.MemoryCardGameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val userDataSource: UserDataSource
) : ViewModel() {

    val userIntent = Channel<MemoryCardGameIntent>(Channel.UNLIMITED)

    var userName: String? = null
        private set

    var movesState: GameTypeMoveWinState? = null
        private set

    @Volatile
    var gameNames: MutableSet<String> = mutableSetOf()
        private set

    var myGames: MutableSet<String> = mutableSetOf()
        private set

    private var isTimeLeft: Boolean = true

    private val _timerState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val timerState: StateFlow<Boolean> = _timerState.asStateFlow()

    fun addMyGames(id: String) {
        myGames.add(id)
    }

    private val _state: MutableStateFlow<MemoryCardGameState> =
        MutableStateFlow(MemoryCardGameState.Idle)
    val state: StateFlow<MemoryCardGameState> = _state.asStateFlow()

    private val _gameStatusState: MutableStateFlow<GameStatus> = MutableStateFlow(GameStatus.Idle)
    val gameStatusState: StateFlow<GameStatus> = _gameStatusState.asStateFlow()

    var cards: List<MemoryCard> = emptyList()
        private set

    private var lastPickedCard: Int? = null

    private var pairsMatched: Int = 0
    private var moves: Int = 0

    var boardSize: BoardSize = BoardSize.EASY
        private set

    var gameType: GameType = GameType.Normal
        private set

    private var customImages: List<String>? = null

    init {
        viewModelScope.launch {
            initCards()
            handleIntent()
        }
    }

    fun addGameNames(name: String) {
        gameNames.add(name)
    }

    fun fetchUserDetails() {
        viewModelScope.launch {
            setUser()
        }
    }

    private suspend fun setUser() {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                userName = userDataSource.getUserName().firstOrNull()
                _state.emit(MemoryCardGameState.OnFetchingUsername)
            } catch (e: Exception) {
                Log.i( "MainViewModel","fetching username from datastore failed ${e.message}")
                _state.emit(MemoryCardGameState.Error("Failed to fetch Username"))
            }
        }
    }

    fun setCustomCards(images: List<String>?) {
        customImages = images
    }

    private fun initCards() {
        if (customImages == null) {
            cards = emptyList()
            val distinctImages =
                Constants.DEFAULT_ICONS.shuffled().take(boardSize.getTotalPairs())
            val randomDuplicateImages = distinctImages.plus(distinctImages).shuffled()
            cards = randomDuplicateImages.map { MemoryCard(identifier = it) }
        } else {
            customImages?.let { images ->
                val randomDuplicateImages = images.plus(images).shuffled()
                cards = randomDuplicateImages.map {
                    MemoryCard(
                        identifier = it.hashCode(),
                        imageUrl = it
                    )
                }
            }
        }

    }

    fun setTimeLeft(isLeft: Boolean) {
        isTimeLeft = isLeft
    }

    private suspend fun handleIntent() {
        userIntent.consumeAsFlow().collect {
            when (it) {
                is MemoryCardGameIntent.OnGameCardClick -> {
                    withContext(Dispatchers.IO) {

                        if (gameType == GameType.TimeLimit && !isTimeLeft) {
                            return@withContext
                        }

                        if (gameType == GameType.TimeLimit && !timerState.value) {
                            _timerState.emit(true)
                        }
                        updateCurrentCard(it.position)
                        _state.emit(MemoryCardGameState.CardStateChange(it.position))
                    }
                }

                is MemoryCardGameIntent.OnRefresh -> {
                    withContext(Dispatchers.IO) {
                        reset()
                        _state.emit(MemoryCardGameState.OnGameReset(cards))
                    }
                }
                else -> {
                    _state.emit(MemoryCardGameState.Error("Can't Perform this action"))
                }
            }
        }
    }

    private suspend fun updateCurrentCard(position: Int) {
        flipCard(position)
        _gameStatusState.emit(GameStatus.MovesAndPairs(getTotalMoves(), pairsMatched))
    }

    fun setSetting(size: BoardSize, type: GameType) {
        boardSize = size
        gameType = type

        viewModelScope.launch {
            reset()
            _state.emit(MemoryCardGameState.OnGameReset(cards))
        }
    }

    private fun flipCard(position: Int): Boolean {
        val card = cards[position]
        ++moves
        var isMatched = false
        if (lastPickedCard == null) {
            restoreCards()
            lastPickedCard = position
        } else {
            isMatched = checkIfCardsMatched(lastPickedCard!!, position)
            lastPickedCard = null
        }
        card.isFaceUp = !card.isFaceUp
        return isMatched
    }

    private fun checkIfCardsMatched(position1: Int, position2: Int): Boolean {
        if (cards[position1].identifier != cards[position2].identifier) return false
        cards[position1].isMatched = true
        cards[position2].isMatched = true
        ++pairsMatched
        return true
    }

    private fun restoreCards() {
        for (card in cards) {
            if (!card.isMatched) {
                card.isFaceUp = false
            }
        }
    }

    private fun reset() {
        initCards()
        moves = 0
        isTimeLeft = true
        _timerState.value = false
        lastPickedCard = null
        pairsMatched = 0
    }

    fun isGameOver(): Boolean {
        return when (gameType) {
            GameType.Normal -> {
                isAllPairsMatched()
            }
            GameType.Moves -> {
                getMovesLimitScore()
                isAllPairsMatched()
            }
            GameType.TimeLimit -> {
                if (isAllPairsMatched()) {
                    setTimerOff()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun setTimerOff() {
        viewModelScope.launch {
            _timerState.emit(false)
        }
    }

    private fun isAllPairsMatched(): Boolean {
        return boardSize.getTotalPairs() == pairsMatched
    }

    private fun getMovesLimitScore() {
        movesState = when {
            (getWinnerMaxMoves().first >= getTotalMoves()) -> GameTypeMoveWinState.WINNER
            ((getWinnerMaxMoves().first < getTotalMoves()) && (getWinnerMaxMoves().second >= getTotalMoves())) ->
                GameTypeMoveWinState.TRY_BETTER
            else -> GameTypeMoveWinState.LOSER
        }
    }

    private fun getWinnerMaxMoves(): Pair<Int, Int> {
        return when(boardSize.getTotalPairs()) {
            4 -> 6 to 8
            9 -> 16 to 20
            12 -> 24 to 28
            14 -> 30 to 35
            else -> 150 to 150
        }
    }

    fun canPlay() = getTotalMoves() > 0 && !isAllPairsMatched()

    private fun getTotalMoves(): Int {
        return moves / 2
    }

    fun getTimer(): Long {
        return when(boardSize) {
            BoardSize.EASY -> boardSize.getTimerInMillis()
            BoardSize.MEDIUM -> boardSize.getTimerInMillis()
            BoardSize.HARD -> boardSize.getTimerInMillis()
            BoardSize.VERY_HARD -> boardSize.getTimerInMillis()
        }
    }

    fun isCardAlreadyFaceUp(position: Int): Boolean = cards[position].isFaceUp

    fun checkIfAccountCreated(): Boolean = userName != null
}

enum class GameTypeMoveWinState {
    WINNER, TRY_BETTER, LOSER
}