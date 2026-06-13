package com.tdev.nichess.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

class BluetoothChessManager(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val SERVICE_NAME = "NichessGame"
    }

    private val adapter = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var socket: BluetoothSocket? = null

    private val _messages = MutableSharedFlow<String>()
    val messages: SharedFlow<String> = _messages

    var isConnected = false
        private set

    var isHost = false
        private set

    suspend fun startServer(): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermissions()) return@withContext false
        try {
            serverSocket = adapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
            isHost = true
            socket = serverSocket?.accept(30000)
            serverSocket?.close()
            isConnected = socket != null
            if (isConnected) startListening()
            isConnected
        } catch (e: IOException) {
            false
        }
    }

    suspend fun connectToDevice(device: BluetoothDevice): Boolean = withContext(Dispatchers.IO) {
        if (!hasPermissions()) return@withContext false
        try {
            socket = device.createRfcommSocketToServiceRecord(SERVICE_UUID)
            socket?.connect()
            isConnected = true
            isHost = false
            startListening()
            true
        } catch (e: IOException) {
            false
        }
    }

    private fun startListening() {
        Thread {
            val buffer = ByteArray(1024)
            val input = socket?.inputStream ?: return@Thread
            while (isConnected) {
                try {
                    val bytes = input.read(buffer)
                    val msg = String(buffer, 0, bytes)
                    kotlinx.coroutines.runBlocking { _messages.emit(msg.trim()) }
                } catch (e: IOException) {
                    isConnected = false
                    break
                }
            }
        }.start()
    }

    suspend fun sendMove(uci: String) = withContext(Dispatchers.IO) {
        try {
            socket?.outputStream?.write("$uci\n".toByteArray())
        } catch (_: IOException) {}
    }

    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasPermissions()) return emptyList()
        return adapter?.bondedDevices?.toList() ?: emptyList()
    }

    fun isBluetoothEnabled() = adapter?.isEnabled == true

    private fun hasPermissions(): Boolean {
        val perms = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
        return perms.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun disconnect() {
        try {
            isConnected = false
            socket?.close()
            serverSocket?.close()
        } catch (_: IOException) {}
    }
}
