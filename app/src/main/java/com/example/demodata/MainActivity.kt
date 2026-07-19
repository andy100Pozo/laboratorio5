package com.example.demodata
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.demodata.ui.Navigation
import com.example.demodata.ui.theme.AppTheme
import com.example.demodata.ui.viewmodel.SessionViewModel
import com.google.firebase.messaging.FirebaseMessaging
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = applicationContext as DemoDataApp
            val sessionVm: SessionViewModel = viewModel(
                factory = SessionViewModel.Factory(app.sessionManager)
            )

            val isDarkModePref by sessionVm.isDarkMode.collectAsState()
            val darkTheme      = isDarkModePref ?: isSystemInDarkTheme()


            // Launcher para solicitar el permiso de notificaciones de forma moderna en Compose
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                // Aquí podrías manejar el resultado si fuera necesario
            }

            // Solicitamos el permiso al iniciar la app (solo en Android 13+)
            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                // Nos suscribimos al tema global para recibir notificaciones masivas
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            }

            AppTheme(darkTheme = darkTheme) {
                Navigation()
            }
        }
    }
}