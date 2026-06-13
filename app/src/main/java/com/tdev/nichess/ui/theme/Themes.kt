package com.tdev.nichess.ui.theme

import androidx.annotation.ColorInt

data class BoardTheme(
    val name: String,
    @ColorInt val lightSquare: Int,
    @ColorInt val darkSquare: Int,
    @ColorInt val selectedSquare: Int,
    @ColorInt val legalMove: Int,
    @ColorInt val lastMove: Int,
    @ColorInt val background: Int,
    @ColorInt val pieceColor: Int
)

object Themes {

    val CLASSIC = BoardTheme(
        name = "Klasik",
        lightSquare = 0xFFF0D9B5.toInt(),
        darkSquare = 0xFFB58863.toInt(),
        selectedSquare = 0xFF7FC97F.toInt(),
        legalMove = 0x557FC97F,
        lastMove = 0xAAF6F669.toInt(),
        background = 0xFF1A1A1A.toInt(),
        pieceColor = 0xFFFFFFFF.toInt()
    )

    val MIDNIGHT = BoardTheme(
        name = "Gece",
        lightSquare = 0xFF3D5A80.toInt(),
        darkSquare = 0xFF1B2A3B.toInt(),
        selectedSquare = 0xFF00B4D8.toInt(),
        legalMove = 0x5500B4D8,
        lastMove = 0xAA90E0E0.toInt(),
        background = 0xFF0A0F1C.toInt(),
        pieceColor = 0xFFE0E0E0.toInt()
    )

    val FOREST = BoardTheme(
        name = "Orman",
        lightSquare = 0xFF9CAF88.toInt(),
        darkSquare = 0xFF4A6741.toInt(),
        selectedSquare = 0xFFFFC857.toInt(),
        legalMove = 0x55FFC857,
        lastMove = 0xAAFFE082.toInt(),
        background = 0xFF1E2B1A.toInt(),
        pieceColor = 0xFFFFFFFF.toInt()
    )

    val STONE = BoardTheme(
        name = "Taş",
        lightSquare = 0xFFD4C5B0.toInt(),
        darkSquare = 0xFF7B6F5E.toInt(),
        selectedSquare = 0xFFE07B39.toInt(),
        legalMove = 0x55E07B39,
        lastMove = 0xAAF4A261.toInt(),
        background = 0xFF2C2416.toInt(),
        pieceColor = 0xFFFFFFFF.toInt()
    )

    val SECRET = BoardTheme(
        name = "???",
        lightSquare = 0xFF3A0000.toInt(),
        darkSquare = 0xFF1A0000.toInt(),
        selectedSquare = 0xFFFF0000.toInt(),
        legalMove = 0x55FF0000,
        lastMove = 0xAAFF4444.toInt(),
        background = 0xFF0A0000.toInt(),
        pieceColor = 0xFFFF2020.toInt()
    )

    val all = listOf(CLASSIC, MIDNIGHT, FOREST, STONE)
}
