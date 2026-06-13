package com.tdev.nichess.game

class ChessEngine {

    private var enPassantTarget: Square? = null
    private var castlingRights = CastlingRights()
    private var halfMoveClock = 0
    private var fullMoveNumber = 1

    data class CastlingRights(
        val whiteKingside: Boolean = true,
        val whiteQueenside: Boolean = true,
        val blackKingside: Boolean = true,
        val blackQueenside: Boolean = true
    )

    fun getLegalMoves(state: GameState): List<Move> {
        val pseudoLegal = getPseudoLegalMoves(state)
        return pseudoLegal.filter { move ->
            val next = applyMoveInternal(state, move)
            !isInCheck(next, state.currentTurn)
        }
    }

    fun applyMove(state: GameState, move: Move): GameState {
        val next = applyMoveInternal(state, move)
        val result = computeResult(next)
        val newHistory = state.moveHistory + move
        val captured = getCapturedPiece(state, move)
        val capturedByWhite = if (state.currentTurn == PieceColor.WHITE && captured != null)
            state.capturedByWhite + captured else state.capturedByWhite
        val capturedByBlack = if (state.currentTurn == PieceColor.BLACK && captured != null)
            state.capturedByBlack + captured else state.capturedByBlack
        return next.copy(
            result = result,
            moveHistory = newHistory,
            capturedByWhite = capturedByWhite,
            capturedByBlack = capturedByBlack
        )
    }

    private fun getCapturedPiece(state: GameState, move: Move): Piece? {
        return state.board[move.to.row][move.to.col]
    }

    private fun applyMoveInternal(state: GameState, move: Move): GameState {
        val board = state.board.map { it.copyOf() }.toTypedArray()
        val piece = board[move.from.row][move.from.col] ?: return state

        // en passant capture
        if (piece.type == PieceType.PAWN && move.to == enPassantTarget) {
            val captureRow = if (piece.color == PieceColor.WHITE) move.to.row - 1 else move.to.row + 1
            board[captureRow][move.to.col] = null
        }

        // castling
        if (piece.type == PieceType.KING) {
            val diff = move.to.col - move.from.col
            if (diff == 2) {
                board[move.from.row][5] = board[move.from.row][7]
                board[move.from.row][7] = null
            } else if (diff == -2) {
                board[move.from.row][3] = board[move.from.row][0]
                board[move.from.row][0] = null
            }
        }

        val finalPiece = if (piece.type == PieceType.PAWN && (move.to.row == 0 || move.to.row == 7)) {
            Piece(move.promotion ?: PieceType.QUEEN, piece.color)
        } else piece

        board[move.to.row][move.to.col] = finalPiece
        board[move.from.row][move.from.col] = null

        enPassantTarget = if (piece.type == PieceType.PAWN && Math.abs(move.to.row - move.from.row) == 2) {
            Square(move.from.col, (move.from.row + move.to.row) / 2)
        } else null

        updateCastlingRights(piece, move)

        return state.copy(
            board = board,
            currentTurn = if (state.currentTurn == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        )
    }

    private fun updateCastlingRights(piece: Piece, move: Move) {
        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) {
                castlingRights = castlingRights.copy(whiteKingside = false, whiteQueenside = false)
            } else {
                castlingRights = castlingRights.copy(blackKingside = false, blackQueenside = false)
            }
        }
        if (piece.type == PieceType.ROOK) {
            when {
                move.from == Square(0, 0) -> castlingRights = castlingRights.copy(whiteQueenside = false)
                move.from == Square(7, 0) -> castlingRights = castlingRights.copy(whiteKingside = false)
                move.from == Square(0, 7) -> castlingRights = castlingRights.copy(blackQueenside = false)
                move.from == Square(7, 7) -> castlingRights = castlingRights.copy(blackKingside = false)
            }
        }
    }

    private fun getPseudoLegalMoves(state: GameState): List<Move> {
        val moves = mutableListOf<Move>()
        for (row in 0..7) {
            for (col in 0..7) {
                val piece = state.board[row][col] ?: continue
                if (piece.color != state.currentTurn) continue
                moves += getMovesForPiece(state, Square(col, row), piece)
            }
        }
        return moves
    }

    private fun getMovesForPiece(state: GameState, sq: Square, piece: Piece): List<Move> {
        return when (piece.type) {
            PieceType.PAWN -> getPawnMoves(state, sq, piece)
            PieceType.KNIGHT -> getKnightMoves(state, sq, piece)
            PieceType.BISHOP -> getSlidingMoves(state, sq, piece, listOf(1 to 1, 1 to -1, -1 to 1, -1 to -1))
            PieceType.ROOK -> getSlidingMoves(state, sq, piece, listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0))
            PieceType.QUEEN -> getSlidingMoves(state, sq, piece, listOf(0 to 1, 0 to -1, 1 to 0, -1 to 0, 1 to 1, 1 to -1, -1 to 1, -1 to -1))
            PieceType.KING -> getKingMoves(state, sq, piece)
        }
    }

    private fun getPawnMoves(state: GameState, sq: Square, piece: Piece): List<Move> {
        val moves = mutableListOf<Move>()
        val dir = if (piece.color == PieceColor.WHITE) 1 else -1
        val startRow = if (piece.color == PieceColor.WHITE) 1 else 6
        val promoRow = if (piece.color == PieceColor.WHITE) 7 else 0

        val oneForward = Square(sq.col, sq.row + dir)
        if (oneForward.row in 0..7 && state.board[oneForward.row][oneForward.col] == null) {
            if (oneForward.row == promoRow) {
                moves += listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
                    .map { Move(sq, oneForward, it) }
            } else {
                moves += Move(sq, oneForward)
                val twoForward = Square(sq.col, sq.row + dir * 2)
                if (sq.row == startRow && state.board[twoForward.row][twoForward.col] == null) {
                    moves += Move(sq, twoForward)
                }
            }
        }

        for (dc in listOf(-1, 1)) {
            val capSq = Square(sq.col + dc, sq.row + dir)
            if (capSq.col !in 0..7 || capSq.row !in 0..7) continue
            val target = state.board[capSq.row][capSq.col]
            if ((target != null && target.color != piece.color) || capSq == enPassantTarget) {
                if (capSq.row == promoRow) {
                    moves += listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
                        .map { Move(sq, capSq, it) }
                } else {
                    moves += Move(sq, capSq)
                }
            }
        }

        return moves
    }

    private fun getKnightMoves(state: GameState, sq: Square, piece: Piece): List<Move> {
        val deltas = listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2, 1 to -2, 1 to 2, 2 to -1, 2 to 1)
        return deltas.mapNotNull { (dc, dr) ->
            val to = Square(sq.col + dc, sq.row + dr)
            if (to.col !in 0..7 || to.row !in 0..7) return@mapNotNull null
            val target = state.board[to.row][to.col]
            if (target?.color == piece.color) return@mapNotNull null
            Move(sq, to)
        }
    }

    private fun getSlidingMoves(state: GameState, sq: Square, piece: Piece, dirs: List<Pair<Int, Int>>): List<Move> {
        val moves = mutableListOf<Move>()
        for ((dc, dr) in dirs) {
            var col = sq.col + dc
            var row = sq.row + dr
            while (col in 0..7 && row in 0..7) {
                val target = state.board[row][col]
                if (target == null) {
                    moves += Move(sq, Square(col, row))
                } else {
                    if (target.color != piece.color) moves += Move(sq, Square(col, row))
                    break
                }
                col += dc
                row += dr
            }
        }
        return moves
    }

    private fun getKingMoves(state: GameState, sq: Square, piece: Piece): List<Move> {
        val moves = mutableListOf<Move>()
        val deltas = listOf(-1 to -1, -1 to 0, -1 to 1, 0 to -1, 0 to 1, 1 to -1, 1 to 0, 1 to 1)
        for ((dc, dr) in deltas) {
            val to = Square(sq.col + dc, sq.row + dr)
            if (to.col !in 0..7 || to.row !in 0..7) continue
            val target = state.board[to.row][to.col]
            if (target?.color == piece.color) continue
            moves += Move(sq, to)
        }

        // castling
        val row = if (piece.color == PieceColor.WHITE) 0 else 7
        if (sq.row == row && sq.col == 4 && !isInCheck(state, piece.color)) {
            if ((piece.color == PieceColor.WHITE && castlingRights.whiteKingside ||
                    piece.color == PieceColor.BLACK && castlingRights.blackKingside) &&
                state.board[row][5] == null && state.board[row][6] == null &&
                state.board[row][7]?.type == PieceType.ROOK) {
                val pass = state.copy()
                val passBoard = state.board.map { it.copyOf() }.toTypedArray()
                passBoard[row][5] = piece
                passBoard[row][4] = null
                if (!isInCheck(state.copy(board = passBoard), piece.color)) {
                    moves += Move(sq, Square(6, row))
                }
            }
            if ((piece.color == PieceColor.WHITE && castlingRights.whiteQueenside ||
                    piece.color == PieceColor.BLACK && castlingRights.blackQueenside) &&
                state.board[row][1] == null && state.board[row][2] == null && state.board[row][3] == null &&
                state.board[row][0]?.type == PieceType.ROOK) {
                val passBoard = state.board.map { it.copyOf() }.toTypedArray()
                passBoard[row][3] = piece
                passBoard[row][4] = null
                if (!isInCheck(state.copy(board = passBoard), piece.color)) {
                    moves += Move(sq, Square(2, row))
                }
            }
        }

        return moves
    }

    fun isInCheck(state: GameState, color: PieceColor): Boolean {
        val kingSquare = findKing(state, color) ?: return false
        val opponent = if (color == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
        val attackState = state.copy(currentTurn = opponent)
        return getPseudoLegalMoves(attackState).any { it.to == kingSquare }
    }

    private fun findKing(state: GameState, color: PieceColor): Square? {
        for (row in 0..7) {
            for (col in 0..7) {
                val p = state.board[row][col]
                if (p?.type == PieceType.KING && p.color == color) return Square(col, row)
            }
        }
        return null
    }

    fun computeResult(state: GameState): GameResult {
        val legal = getLegalMoves(state)
        if (legal.isNotEmpty()) return GameResult.ONGOING
        return if (isInCheck(state, state.currentTurn)) {
            if (state.currentTurn == PieceColor.WHITE) GameResult.BLACK_WINS else GameResult.WHITE_WINS
        } else {
            GameResult.DRAW
        }
    }

    fun toFen(state: GameState): String {
        val sb = StringBuilder()
        for (row in 7 downTo 0) {
            var empty = 0
            for (col in 0..7) {
                val piece = state.board[row][col]
                if (piece == null) {
                    empty++
                } else {
                    if (empty > 0) { sb.append(empty); empty = 0 }
                    val ch = when (piece.type) {
                        PieceType.KING -> 'k'; PieceType.QUEEN -> 'q'; PieceType.ROOK -> 'r'
                        PieceType.BISHOP -> 'b'; PieceType.KNIGHT -> 'n'; PieceType.PAWN -> 'p'
                    }
                    sb.append(if (piece.color == PieceColor.WHITE) ch.uppercaseChar() else ch)
                }
            }
            if (empty > 0) sb.append(empty)
            if (row > 0) sb.append('/')
        }
        sb.append(' ')
        sb.append(if (state.currentTurn == PieceColor.WHITE) 'w' else 'b')
        sb.append(' ')
        val cr = StringBuilder()
        if (castlingRights.whiteKingside) cr.append('K')
        if (castlingRights.whiteQueenside) cr.append('Q')
        if (castlingRights.blackKingside) cr.append('k')
        if (castlingRights.blackQueenside) cr.append('q')
        sb.append(if (cr.isEmpty()) "-" else cr)
        sb.append(' ')
        sb.append(enPassantTarget?.toAlgebraic() ?: "-")
        sb.append(" $halfMoveClock $fullMoveNumber")
        return sb.toString()
    }
}
