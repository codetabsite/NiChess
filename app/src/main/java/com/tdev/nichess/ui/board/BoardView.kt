package com.tdev.nichess.ui.board

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.tdev.nichess.game.GameState
import com.tdev.nichess.game.Move
import com.tdev.nichess.game.Piece
import com.tdev.nichess.game.PieceColor
import com.tdev.nichess.game.PieceType
import com.tdev.nichess.game.Square
import com.tdev.nichess.ui.theme.BoardTheme
import com.tdev.nichess.ui.theme.Themes

class BoardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var theme: BoardTheme = Themes.CLASSIC
        set(v) { field = v; invalidate() }

    var gameState: GameState = GameState.initial()
        set(v) { field = v; invalidate() }

    var selectedSquare: Square? = null
        set(v) { field = v; invalidate() }

    var legalMoves: List<Move> = emptyList()
        set(v) { field = v; invalidate() }

    var isFlipped: Boolean = false
        set(v) { field = v; invalidate() }

    var onSquareTapped: ((Square) -> Unit)? = null

    private val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val darkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val legalMovePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lastMovePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val piecePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val coordPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var squareSize = 0f

    private val pieceSymbols = mapOf(
        PieceType.KING to "\u2654",
        PieceType.QUEEN to "\u2655",
        PieceType.ROOK to "\u2656",
        PieceType.BISHOP to "\u2657",
        PieceType.KNIGHT to "\u2658",
        PieceType.PAWN to "\u2659"
    )

    init {
        coordPaint.textSize = 28f
        coordPaint.typeface = Typeface.MONOSPACE
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        squareSize = w / 8f
        piecePaint.textSize = squareSize * 0.72f
        piecePaint.textAlign = Paint.Align.CENTER
        coordPaint.textSize = squareSize * 0.18f
    }

    override fun onDraw(canvas: Canvas) {
        applyTheme()
        val lastMove = gameState.moveHistory.lastOrNull()

        for (row in 0..7) {
            for (col in 0..7) {
                val drawCol = if (isFlipped) 7 - col else col
                val drawRow = if (isFlipped) row else 7 - row
                val sq = Square(drawCol, drawRow)

                val left = col * squareSize
                val top = row * squareSize
                val rect = RectF(left, top, left + squareSize, top + squareSize)

                val isLight = (drawCol + drawRow) % 2 == 0
                canvas.drawRect(rect, if (isLight) lightPaint else darkPaint)

                if (lastMove != null && (sq == lastMove.from || sq == lastMove.to)) {
                    canvas.drawRect(rect, lastMovePaint)
                }

                if (sq == selectedSquare) {
                    canvas.drawRect(rect, selectedPaint)
                }

                if (legalMoves.any { it.to == sq }) {
                    val cx = left + squareSize / 2
                    val cy = top + squareSize / 2
                    val r = squareSize * 0.16f
                    canvas.drawCircle(cx, cy, r, legalMovePaint)
                }

                val piece = gameState.board[drawRow][drawCol]
                if (piece != null) {
                    drawPiece(canvas, piece, left, top)
                }

                // coordinates
                coordPaint.color = if (isLight) darkPaint.color else lightPaint.color
                coordPaint.alpha = 160
                if (col == 0) {
                    canvas.drawText("${drawRow + 1}", left + 4f, top + coordPaint.textSize + 2f, coordPaint)
                }
                if (row == 7) {
                    val letter = "${'a' + drawCol}"
                    canvas.drawText(letter, left + squareSize - coordPaint.measureText(letter) - 4f, top + squareSize - 4f, coordPaint)
                }
            }
        }
    }

    private fun drawPiece(canvas: Canvas, piece: Piece, left: Float, top: Float) {
        val symbol = pieceSymbols[piece.type] ?: return
        val cx = left + squareSize / 2
        val cy = top + squareSize / 2 - (piecePaint.descent() + piecePaint.ascent()) / 2

        // shadow
        piecePaint.color = 0x44000000
        piecePaint.style = Paint.Style.FILL
        canvas.drawText(symbol, cx + 2f, cy + 3f, piecePaint)

        piecePaint.color = if (piece.color == PieceColor.WHITE) 0xFFFAFAFA.toInt() else 0xFF1A1A1A.toInt()
        canvas.drawText(symbol, cx, cy, piecePaint)

        if (piece.color == PieceColor.WHITE) {
            piecePaint.color = 0xFF888888.toInt()
            piecePaint.style = Paint.Style.STROKE
            piecePaint.strokeWidth = 1.5f
            canvas.drawText(symbol, cx, cy, piecePaint)
            piecePaint.style = Paint.Style.FILL
        }
    }

    private fun applyTheme() {
        lightPaint.color = theme.lightSquare
        darkPaint.color = theme.darkSquare
        selectedPaint.color = theme.selectedSquare
        selectedPaint.alpha = 180
        legalMovePaint.color = theme.legalMove
        legalMovePaint.alpha = 160
        lastMovePaint.color = theme.lastMove
        lastMovePaint.alpha = 120
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val col = (event.x / squareSize).toInt().coerceIn(0, 7)
            val row = (event.y / squareSize).toInt().coerceIn(0, 7)
            val drawCol = if (isFlipped) 7 - col else col
            val drawRow = if (isFlipped) row else 7 - row
            onSquareTapped?.invoke(Square(drawCol, drawRow))
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
