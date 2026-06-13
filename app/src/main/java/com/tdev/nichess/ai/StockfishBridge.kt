package com.tdev.nichess.ai

import android.content.Context
import com.tdev.nichess.game.ChessEngine
import com.tdev.nichess.game.GameState
import com.tdev.nichess.game.Move
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintWriter

class StockfishBridge(private val context: Context) {

    private var process: Process? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    private val engine = ChessEngine()

    suspend fun init(): Boolean = withContext(Dispatchers.IO) {
        try {
            val binary = extractBinary() ?: return@withContext false
            process = ProcessBuilder(binary.absolutePath)
                .redirectErrorStream(true)
                .start()
            writer = PrintWriter(process!!.outputStream, true)
            reader = BufferedReader(InputStreamReader(process!!.inputStream))
            sendCommand("uci")
            waitFor("uciok")
            sendCommand("isready")
            waitFor("readyok")
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun extractBinary(): File? {
        val abi = android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: return null
        val assetName = when {
            abi.contains("arm64") -> "stockfish_arm64"
            abi.contains("armeabi") -> "stockfish_arm"
            abi.contains("x86_64") -> "stockfish_x86_64"
            else -> "stockfish_arm64"
        }
        val outFile = File(context.filesDir, "stockfish")
        try {
            context.assets.open(assetName).use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            outFile.setExecutable(true)
            return outFile
        } catch (e: Exception) {
            return null
        }
    }

    suspend fun getBestMove(state: GameState, elo: Int): Move? = withContext(Dispatchers.IO) {
        if (process == null || process?.isAlive == false) return@withContext getFallbackMove(state)

        val fen = engine.toFen(state)
        val depth = eloToDepth(elo)

        try {
            sendCommand("setoption name UCI_LimitStrength value ${elo < 3000}")
            sendCommand("setoption name UCI_Elo value ${elo.coerceIn(1320, 3190)}")
            sendCommand("position fen $fen")
            sendCommand("go depth $depth movetime ${eloToTime(elo)}")

            val result = withTimeoutOrNull(10000) {
                var line: String?
                var bestMove: String? = null
                while (reader?.readLine().also { line = it } != null) {
                    if (line!!.startsWith("bestmove")) {
                        bestMove = line!!.split(" ")[1]
                        break
                    }
                }
                bestMove
            }

            result?.let { Move.fromUci(it) }
        } catch (e: Exception) {
            getFallbackMove(state)
        }
    }

    private fun getFallbackMove(state: GameState): Move? {
        return engine.getLegalMoves(state).randomOrNull()
    }

    private fun eloToDepth(elo: Int): Int = when {
        elo >= 10000 -> 20
        elo >= 2500 -> 15
        elo >= 2000 -> 12
        elo >= 1500 -> 9
        elo >= 1200 -> 6
        else -> 4
    }

    private fun eloToTime(elo: Int): Int = when {
        elo >= 10000 -> 5000
        elo >= 2500 -> 2000
        elo >= 2000 -> 1500
        elo >= 1500 -> 1000
        else -> 500
    }

    private fun sendCommand(cmd: String) {
        writer?.println(cmd)
    }

    private fun waitFor(token: String) {
        var line: String?
        while (reader?.readLine().also { line = it } != null) {
            if (line!!.contains(token)) break
        }
    }

    fun stop() {
        try {
            sendCommand("quit")
            process?.destroy()
        } catch (_: Exception) {}
    }
}
