package com.tdev.nichess.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tdev.nichess.databinding.ActivityMainBinding
import com.tdev.nichess.ui.board.GameActivity
import com.tdev.nichess.ui.board.GameMode
import com.tdev.nichess.ui.dialogs.BluetoothActivity
import com.tdev.nichess.ui.dialogs.NewGameDialog

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnVsAi.setOnClickListener {
            NewGameDialog(this) { color, elo, themeIndex ->
                Intent(this, GameActivity::class.java).apply {
                    putExtra("mode", GameMode.VS_AI.name)
                    putExtra("color", color.name)
                    putExtra("elo", elo)
                    putExtra("theme", themeIndex)
                    startActivity(this)
                }
            }.show()
        }

        binding.btnBluetooth.setOnClickListener {
            startActivity(Intent(this, BluetoothActivity::class.java))
        }

        binding.btnLocal.setOnClickListener {
            Intent(this, GameActivity::class.java).apply {
                putExtra("mode", GameMode.LOCAL_TWO_PLAYER.name)
                putExtra("color", "WHITE")
                putExtra("elo", 0)
                putExtra("theme", 0)
                startActivity(this)
            }
        }
    }
}
