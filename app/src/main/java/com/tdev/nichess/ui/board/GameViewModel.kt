package com.tdev.nichess.ui.board

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tdev.nichess.ai.StockfishBridge
import com.tdev.nichess.bluetooth.BluetoothChessManager
import com.tdev.nichess.game.ChessEngine
import com.tdev.nichess.game.GameResult
import com.tdev.nichess.game.GameState
import com.tdev.nichess.game.Move
import com.tdev.nichess.game.PieceColor
import com.tdev.nichess.game.PieceType
import com.tdev.nichess.game.Square
import com.tdev.nichess.ui.theme.BoardTheme
import com.tdev.nichess.ui.theme.Themes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class GameMode { VS_AI, BLUETOOTH, LOCAL_TWO_PLAYER }

data class UiState(
    val gameState: GameState = GameState.initial(),
    val selectedSquare: Square? = null,
    val legalMovesForSelected: List<Move> = emptyList(),
    val theme: BoardTheme = Themes.CLASSIC,
    val mode: GameMode = GameMode.VS_AI,
    val playerColor: PieceColor = PieceColor.WHITE,
    val aiElo: Int = 1200,
    val isAiThinking: Boolean = false,
    val secretTapCount: Int = 0,
    val isSecretMode: Boolean = false,
    val whiteTimeMs: Long = 600000L,
    val blackTimeMs: Long = 600000L,
    val isTimerRunning: Boolean = false,
    val promotionPending: Move? = null
)

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val engine = ChessEngine()
    private val stockfish = StockfishBridge(app)
    val bluetooth = BluetoothChessManager(app)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private var timerJob: Job? = null

    init {
        viewModelScope.launch { stockfish.init() }
        viewModelScope.launch {
            bluetooth.messages.collect { uci ->
                val move = try { Move.fromUci(uci) } catch (_: Exception) { return@collect }
                applyMoveFromRemote(move)
            }
        }
    }

    fun startGame(mode: GameMode, playerColor: PieceColor, aiElo: Int, theme: BoardTheme) {
        val state = GameState.initial().copy(whiteElo = if (playerColor == PieceColor.WHITE) 1200 else aiElo,
            blackElo = if (playerColor == PieceColor.BLACK) 1200 else aiElo)
        _ui.value = UiState(
            gameState = state,
            mode = mode,
            playerColor = playerColor,
            aiElo = aiElo,
            theme = theme
        )
        startTimer()
        if (mode == GameMode.VS_AI && playerColor == PieceColor.BLACK) {
            triggerAiMove()
        }
    }

    fun onSquareTapped(sq: Square) {
        val s = _ui.value
        if (s.gameState.result != GameResult.ONGOING) return
        if (s.isAiThinking) return
        if (s.mode == GameMode.VS_AI && s.gameState.currentTurn != s.playerColor) return
        if (s.mode == GameMode.BLUETOOTH) {
            val myColor = if (bluetooth.isHost) PieceColor.WHITE else PieceColor.BLACK
            if (s.gameState.currentTurn != myColor) return
        }

        val piece = s.gameState.board[sq.row][sq.col]

        // Secret mode: count black knight taps
        if (piece?.type == PieceType.KNIGHT && piece.color == PieceColor.BLACK) {
            val newCount = s.secretTapCount + 1
            if (newCount >= 5 && !s.isSecretMode) {
                activateSecretMode()
                return
            }
            _ui.value = s.copy(secretTapCount = newCount)
        }

        if (s.selectedSquare == null) {
            if (piece == null || piece.color != s.gameState.currentTurn) return
            val legal = engine.getLegalMoves(s.gameState).filter { it.from == sq }
            _ui.value = s.copy(selectedSquare = sq, legalMovesForSelected = legal)
        } else {
            val move = s.legalMovesForSelected.find { it.to == sq }
            if (move != null) {
                if (isPromotion(s.gameState, s.selectedSquare!!, sq)) {
                    _ui.value = s.copy(
                        selectedSquare = null,
                        legalMovesForSelected = emptyList(),
                        promotionPending = Move(s.selectedSquare!!, sq)
                    )
                } else {
                    executeMove(move)
                }
            } else if (piece?.color == s.gameState.currentTurn) {
                val legal = engine.getLegalMoves(s.gameState).filter { it.from == sq }
                _ui.value = s.copy(selectedSquare = sq, legalMovesForSelected = legal)
            } else {
                _ui.value = s.copy(selectedSquare = null, legalMovesForSelected = emptyList())
            }
        }
    }

    fun onPromotionChosen(type: PieceType) {
        val pending = _ui.value.promotionPending ?: return
        val move = Move(pending.from, pending.to, type)
        _ui.value = _ui.value.copy(promotionPending = null)
        executeMove(move)
    }

    private fun activateSecretMode() {
        _ui.value = _ui.value.copy(
            isSecretMode = true,
            secretTapCount = 0,
            aiElo = 10000,
            theme = Themes.SECRET,
            selectedSquare = null,
            legalMovesForSelected = emptyList()
        )
    }

    private fun isPromotion(state: GameState, from: Square, to: Square): Boolean {
        val piece = state.board[from.row][from.col] ?: return false
        if (piece.type != PieceType.PAWN) return false
        return (piece.color == PieceColor.WHITE && to.row == 7) ||
                (piece.color == PieceColor.BLACK && to.row == 0)
    }

    private fun executeMove(move: Move) {
        val s = _ui.value
        val newState = engine.applyMove(s.gameState, move)
        _ui.value = s.copy(
            gameState = newState,
            selectedSquare = null,
            legalMovesForSelected = emptyList()
        )
        switchTimer(newState.currentTurn)

        if (s.mode == GameMode.BLUETOOTH) {
            viewModelScope.launch { bluetooth.sendMove(move.toUci()) }
        }

        if (newState.result == GameResult.ONGOING && s.mode == GameMode.VS_AI) {
            triggerAiMove()
        }
    }

    private fun applyMoveFromRemote(move: Move) {
        val s = _ui.value
        val legal = engine.getLegalMoves(s.gameState)
        if (legal.none { it.toUci() == move.toUci() }) return
        val newState = engine.applyMove(s.gameState, move)
        _ui.value = s.copy(
            gameState = newState,
            selectedSquare = null,
            legalMovesForSelected = emptyList()
        )
        switchTimer(newState.currentTurn)
    }

    private fun triggerAiMove() {
        val s = _ui.value
        if (s.gameState.result != GameResult.ONGOING) return
        _ui.value = s.copy(isAiThinking = true)
        viewModelScope.launch {
            val elo = if (s.isSecretMode) 10000 else s.aiElo
            val move = stockfish.getBestMove(s.gameState, elo)
                ?: engine.getLegalMoves(s.gameState).randomOrNull()
            if (move != null) {
                val newState = engine.applyMove(_ui.value.gameState, move)
                _ui.value = _ui.value.copy(
                    gameState = newState,
                    isAiThinking = false,
                    selectedSquare = null,
                    legalMovesForSelected = emptyList()
                )
                switchTimer(newState.currentTurn)
            } else {
                _ui.value = _ui.value.copy(isAiThinking = false)
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _ui.value
                if (!s.isTimerRunning || s.gameState.result != GameResult.ONGOING) continue
                if (s.gameState.currentTurn == PieceColor.WHITE) {
                    val newTime = s.whiteTimeMs - 1000
                    if (newTime <= 0) {
                        _ui.value = s.copy(
                            whiteTimeMs = 0,
                            gameState = s.gameState.copy(result = GameResult.BLACK_WINS)
                        )
                    } else {
                        _ui.value = s.copy(whiteTimeMs = newTime)
                    }
                } else {
                    val newTime = s.blackTimeMs - 1000
                    if (newTime <= 0) {
                        _ui.value = s.copy(
                            blackTimeMs = 0,
                            gameState = s.gameState.copy(result = GameResult.WHITE_WINS)
                        )
                    } else {
                        _ui.value = s.copy(blackTimeMs = newTime)
                    }
                }
            }
        }
        _ui.value = _ui.value.copy(isTimerRunning = true)
    }

    private fun switchTimer(nextTurn: PieceColor) {
        // Timer continues, just tracks which side's clock runs
    }

    fun resign() {
        val s = _ui.value
        val result = if (s.gameState.currentTurn == PieceColor.WHITE) GameResult.BLACK_WINS else GameResult.WHITE_WINS
        _ui.value = s.copy(gameState = s.gameState.copy(result = result))
        timerJob?.cancel()
    }

    fun selectTheme(theme: BoardTheme) {
        _ui.value = _ui.value.copy(theme = theme)
    }

    override fun onCleared() {
        stockfish.stop()
        bluetooth.disconnect()
        timerJob?.cancel()
    }
}
