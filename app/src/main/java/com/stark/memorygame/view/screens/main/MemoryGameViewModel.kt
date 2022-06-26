package com.stark.memorygame.view.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stark.memorygame.data.UserDataSource
import com.stark.memorygame.model.BoardSize
import com.stark.memorygame.model.MemoryCard
import com.stark.memorygame.utils.Constants
import com.stark.memorygame.view.intent.MemoryCardGameIntent
import com.stark.memorygame.view.state.GameStatus
import com.stark.memorygame.view.state.MemoryCardGameState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MemoryGameViewModel @Inject constructor(
    private val userDataSource: UserDataSource
) : ViewModel() {

    val userIntent = Channel<MemoryCardGameIntent>(Channel.UNLIMITED)

    var userName: String? = null
        private set

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

    private var customImages: List<String>? = null

    init {
        viewModelScope.launch {
            initCards()
            userName = getUser()
            handleIntent()
        }
    }

    private suspend fun getUser(): String? {
        return withContext(Dispatchers.IO) {
            return@withContext try {
                userDataSource.getUserName().firstOrNull()
            } catch (e: Exception) {
                // TODO: let user know why u failed to fetch username
                null
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

    private suspend fun handleIntent() {
        userIntent.consumeAsFlow().collect {
            when (it) {
                is MemoryCardGameIntent.OnGameCardClick -> {
                    withContext(Dispatchers.IO) {
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

    fun setBoardSize(size: BoardSize) {
        boardSize = size
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
        lastPickedCard = null
        pairsMatched = 0
    }

    fun isGameWon(): Boolean {
        return boardSize.getTotalPairs() == pairsMatched
    }

    fun canPlay() = getTotalMoves() > 0 && !isGameWon()

    private fun getTotalMoves(): Int {
        return moves / 2
    }

    fun isCardAlreadyFaceUp(position: Int): Boolean = cards[position].isFaceUp

    fun checkIfAccountCreated(): Boolean = userName != null

}