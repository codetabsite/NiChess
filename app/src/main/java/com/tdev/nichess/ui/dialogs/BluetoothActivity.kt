package com.tdev.nichess.ui.dialogs

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tdev.nichess.bluetooth.BluetoothChessManager
import com.tdev.nichess.databinding.ActivityBluetoothBinding
import com.tdev.nichess.ui.board.GameActivity
import com.tdev.nichess.ui.board.GameMode
import kotlinx.coroutines.launch

class BluetoothActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBluetoothBinding
    private lateinit var btManager: BluetoothChessManager
    private val devices = mutableListOf<BluetoothDevice>()

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        if (results.values.all { it }) loadDevices()
        else Toast.makeText(this, "Bluetooth izni gerekli", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothBinding.inflate(layoutInflater)
        setContentView(binding.root)
        btManager = BluetoothChessManager(this)

        checkPermissions()

        binding.btnHost.setOnClickListener {
            binding.tvStatus.text = "Bağlantı bekleniyor..."
            lifecycleScope.launch {
                val ok = btManager.startServer()
                if (ok) launchGame()
                else binding.tvStatus.text = "Bağlantı başarısız"
            }
        }

        binding.listDevices.setOnItemClickListener { _, _, pos, _ ->
            val device = devices[pos]
            binding.tvStatus.text = "Bağlanıyor..."
            lifecycleScope.launch {
                val ok = btManager.connectToDevice(device)
                if (ok) launchGame()
                else binding.tvStatus.text = "Bağlantı başarısız"
            }
        }
    }

    private fun checkPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) loadDevices()
        else permLauncher.launch(missing.toTypedArray())
    }

    private fun loadDevices() {
        devices.clear()
        devices.addAll(btManager.getPairedDevices())
        val names = devices.map { it.name ?: it.address }
        binding.listDevices.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
    }

    private fun launchGame() {
        Intent(this, GameActivity::class.java).apply {
            putExtra("mode", GameMode.BLUETOOTH.name)
            putExtra("color", if (btManager.isHost) "WHITE" else "BLACK")
            putExtra("elo", 0)
            putExtra("theme", 0)
            startActivity(this)
        }
        finish()
    }
}
