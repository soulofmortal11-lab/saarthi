package com.saarthi.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.saarthi.app.ui.HudScreen
import com.saarthi.app.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val vm: SaarthiViewModel by viewModels()

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.onCoreTapped()
        else vm.showNotice("Saarthi needs microphone access to hear you. You can allow it in the app's settings.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var showSettings by rememberSaveable { mutableStateOf(false) }
            BackHandler(enabled = showSettings) { showSettings = false }

            if (showSettings) {
                SettingsScreen(vm = vm, onBack = { showSettings = false })
            } else {
                HudScreen(
                    vm = vm,
                    onOpenSettings = { showSettings = true },
                    onCoreTap = ::onCoreTap,
                )
            }
        }
    }

    private fun onCoreTap() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) vm.onCoreTapped() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }
}
