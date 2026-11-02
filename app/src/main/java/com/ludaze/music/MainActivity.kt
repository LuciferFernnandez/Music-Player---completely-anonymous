package com.ludaze.music

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ludaze.music.ui.LudazeApp
import com.ludaze.music.ui.LudazeTheme
import com.ludaze.music.ui.Slate

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val perms = buildList {
            add(if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
        }.toTypedArray()
        val audioPerm = perms[0]

        setContent {
            LudazeTheme {
                var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(this, audioPerm) == PackageManager.PERMISSION_GRANTED) }
                val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
                    granted = r[audioPerm] == true
                }
                LaunchedEffect(Unit) { if (!granted) launcher.launch(perms) }
                LaunchedEffect(granted) { if (granted) vm.scan() }

                if (granted) LudazeApp(vm) else Column(
                    Modifier.fillMaxSize().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally
                ) {
                    Text("Ludaze Music needs access to your audio files to build your library.", color = Slate)
                    Spacer(Modifier.height(16.dp))
                    Button({ launcher.launch(perms) }) { Text("Grant access") }
                }
            }
        }
    }
}
