package com.tdev.nichess.game

enum class PieceType { KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN }
enum class PieceColor { WHITE, BLACK }

data class Piece(val type: PieceType, val color: PieceColor)

data class Square(val col: Int, val row: Int) {
    fun toAlgebraic(): String = "${'a' + col}${row + 1}"
    companion object {
        fun from(algebraic: String): Square {
            val col = algebraic[0] - 'a'
            val row = algebraic[1].digitToInt() - 1
            return Square(col, row)
        }
    }
}

data class Move(val from: Square, val to: Square, val promotion: PieceType? = null) {
    fun toUci(): String {
        val promo = when (promotion) {
            PieceType.QUEEN -> "q"
            PieceType.ROOK -> "r"
            PieceType.BISHOP -> "b"
            PieceType.KNIGHT -> "n"
            else -> ""
        }
        return "${from.toAlgebraic()}${to.toAlgebraic()}$promo"
    }
    companion object {
        fun fromUci(uci: String): Move {
            val from = Square.from(uci.substring(0, 2))
            val to = Square.from(uci.substring(2, 4))
            val promo = if (uci.length > 4) when (uci[4]) {
                'q' -> PieceType.QUEEN
                'r' -> PieceType.ROOK
                'b' -> PieceType.BISHOP
                'n' -> PieceType.KNIGHT
                else -> null
            } else null
            return Move(from, to, promo)
        }
    }
}

enum class GameResult { ONGOING, WHITE_WINS, BLACK_WINS, DRAW }

data class GameState(
    val board: Array<Array<Piece?>> = emptyBoard(),
    val currentTurn: PieceColor = PieceColor.WHITE,
    val result: GameResult = GameResult.ONGOING,
    val whiteElo: Int = 1200,
    val blackElo: Int = 1200,
    val moveHistory: List<Move> = emptyList(),
    val capturedByWhite: List<Piece> = emptyList(),
    val capturedByBlack: List<Piece> = emptyList(),
    val isSecretMode: Boolean = false
) {
    companion object {
        fun emptyBoard(): Array<Array<Piece?>> = Array(8) { arrayOfNulls(8) }
        fun initial(): GameState {
            val board = emptyBoard()
            val backRow = listOf(
                PieceType.ROOK, PieceType.KNIGHT, PieceType.BISHOP, PieceType.QUEEN,
                PieceType.KING, PieceType.BISHOP, PieceType.KNIGHT, PieceType.ROOK
            )
            for (col in 0..7) {
                board[0][col] = Piece(backRow[col], PieceColor.WHITE)
                board[1][col] = Piece(PieceType.PAWN, PieceColor.WHITE)
                board[6][col] = Piece(PieceType.PAWN, PieceColor.BLACK)
                board[7][col] = Piece(backRow[col], PieceColor.BLACK)
            }
            return GameState(board = board)
        }
    }

    override fun equals(other: Any?) = other is GameState &&
        board.contentDeepEquals(other.board) &&
        currentTurn == other.currentTurn

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + currentTurn.hashCode()
        return result
    }
}
