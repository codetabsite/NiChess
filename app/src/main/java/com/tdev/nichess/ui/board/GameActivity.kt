package com.tdev.nichess.ui.board

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.tdev.nichess.databinding.ActivityGameBinding
import com.tdev.nichess.game.GameResult
import com.tdev.nichess.game.PieceColor
import com.tdev.nichess.game.PieceType
import com.tdev.nichess.ui.theme.Themes
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class GameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameBinding
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mode = GameMode.valueOf(intent.getStringExtra("mode") ?: "VS_AI")
        val playerColor = PieceColor.valueOf(intent.getStringExtra("color") ?: "WHITE")
        val elo = intent.getIntExtra("elo", 1200)
        val themeIndex = intent.getIntExtra("theme", 0)
        val theme = Themes.all.getOrElse(themeIndex) { Themes.CLASSIC }

        vm.startGame(mode, playerColor, elo, theme)

        binding.boardView.onSquareTapped = { sq -> vm.onSquareTapped(sq) }
        binding.boardView.isFlipped = playerColor == PieceColor.BLACK

        binding.btnResign.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Teslim ol")
                .setMessage("Teslim olmak istiyor musun?")
                .setPositiveButton("Evet") { _, _ -> vm.resign() }
                .setNegativeButton("Hayır", null)
                .show()
        }

        lifecycleScope.launch {
            vm.ui.collect { state ->
                binding.boardView.gameState = state.gameState
                binding.boardView.selectedSquare = state.selectedSquare
                binding.boardView.legalMoves = state.legalMovesForSelected
                binding.boardView.theme = state.theme

                binding.root.setBackgroundColor(state.theme.background)

                binding.tvWhiteTime.text = formatTime(state.whiteTimeMs)
                binding.tvBlackTime.text = formatTime(state.blackTimeMs)

                binding.tvWhiteElo.text = "ELO ${state.gameState.whiteElo}"
                binding.tvBlackElo.text = "ELO ${state.gameState.blackElo}"

                binding.tvAiThinking.visibility = if (state.isAiThinking) View.VISIBLE else View.GONE

                if (state.isSecretMode) {
                    binding.tvSecretLabel.visibility = View.VISIBLE
                }

                if (state.promotionPending != null) {
                    showPromotionDialog(state.gameState.currentTurn)
                }

                if (state.gameState.result != GameResult.ONGOING) {
                    showResultDialog(state.gameState.result)
                }
            }
        }
    }

    private fun showPromotionDialog(color: PieceColor) {
        val options = arrayOf("Vezir", "Kale", "Fil", "At")
        val types = arrayOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
        AlertDialog.Builder(this)
            .setTitle("Piyon Terfi")
            .setItems(options) { _, i -> vm.onPromotionChosen(types[i]) }
            .setCancelable(false)
            .show()
    }

    private fun showResultDialog(result: GameResult) {
        val msg = when (result) {
            GameResult.WHITE_WINS -> "Beyaz kazandı"
            GameResult.BLACK_WINS -> "Siyah kazandı"
            GameResult.DRAW -> "Beraberlik"
            GameResult.ONGOING -> return
        }
        AlertDialog.Builder(this)
            .setTitle("Oyun bitti")
            .setMessage(msg)
            .setPositiveButton("Ana menü") { _, _ -> finish() }
            .setNegativeButton("Tekrar") { _, _ ->
                recreate()
            }
            .setCancelable(false)
            .show()
    }

    private fun formatTime(ms: Long): String {
        val m = TimeUnit.MILLISECONDS.toMinutes(ms)
        val s = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
        return "%02d:%02d".format(m, s)
    }
}
