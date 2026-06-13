package com.tdev.nichess.ui.dialogs

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import com.tdev.nichess.databinding.DialogNewGameBinding
import com.tdev.nichess.game.PieceColor

class NewGameDialog(
    private val context: Context,
    private val onStart: (PieceColor, Int, Int) -> Unit
) {
    fun show() {
        val binding = DialogNewGameBinding.inflate(LayoutInflater.from(context))
        val dialog = AlertDialog.Builder(context)
            .setTitle("Yeni Oyun")
            .setView(binding.root)
            .setPositiveButton("Başla") { _, _ ->
                val color = if (binding.rgColor.checkedRadioButtonId == binding.rbWhite.id)
                    PieceColor.WHITE else PieceColor.BLACK
                val elo = binding.sliderElo.value.toInt()
                val theme = binding.spinnerTheme.selectedItemPosition
                onStart(color, elo, theme)
            }
            .setNegativeButton("İptal", null)
            .create()
        dialog.show()
    }
}
