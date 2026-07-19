package com.example.demodata.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.demodata.data.remote.NetworkConstants
import com.example.demodata.data.remote.RetrofitClient
import com.example.demodata.data.remote.model.*
import com.google.firebase.messaging.FirebaseMessaging
import com.example.demodata.data.remote.model.LoginRequest
import com.example.demodata.data.remote.model.RefreshTokenRequest
import com.example.demodata.data.remote.model.RegisterRequest
import com.example.demodata.data.session.SessionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SessionViewModel(
    private val sessionManager: SessionManager
) : ViewModel() {

    val isLoggedIn = sessionManager.isLoggedIn.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = false
    )

    val username = sessionManager.currentUsername.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = null
    )

    val userId = sessionManager.userId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )
    val notificationsEnabled = sessionManager.notificationsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true
    )

    val isDarkMode = sessionManager.isDarkMode.stateIn(
        scope        = viewModelScope,
        started      = SharingStarted.Eagerly,
        initialValue = null
    )

    fun login(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.login(
                    projectSlug = NetworkConstants.PROJECT_SLUG,
                    request     = LoginRequest(
                        email = email,
                        password = password,
                        deviceId = sessionManager.getDeviceId()
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!

                    var finalUserId: String? = null

                    val meResponse = RetrofitClient.apiService.me(
                        NetworkConstants.PROJECT_SLUG,
                        "Bearer ${body.accessToken}"
                    )

                    if (meResponse.isSuccessful) {
                        finalUserId = meResponse.body()?.user?.userId
                    }

                    sessionManager.login(email, body.accessToken, body.refreshToken, finalUserId)
                    fetchAndSyncToken()
                    onResult(true)
                }
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    private fun fetchAndSyncToken() {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                syncFcmToken(token)
            } catch (e: Exception) {
                // Error al obtener token de Firebase
            }
        }
    }
    fun syncFcmToken(fcmToken: String) {
        viewModelScope.launch {
            try {
                val token = sessionManager.accessToken.firstOrNull()
                val uId = sessionManager.userId.firstOrNull()
                val uName = sessionManager.currentUsername.firstOrNull()

                if (token != null) {
                    RetrofitClient.apiService.updateFcmToken(
                        projectSlug = NetworkConstants.PROJECT_SLUG,
                        token = "Bearer $token",
                        request = DeviceTokenRequest(
                            userId = uId,
                            userName = uName,
                            fcmToken = fcmToken,
                            deviceId = sessionManager.getDeviceId()
                        )
                    )
                }
            } catch (e: Exception) {
                // Manejar error de red
            }
        }
    }

    fun register(email: String, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.register(
                    projectSlug = NetworkConstants.PROJECT_SLUG,
                    request     = RegisterRequest(email, password)
                )
                onResult(response.isSuccessful)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun loginWithGoogle(googleToken: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.loginWithGoogle(
                    projectSlug = NetworkConstants.PROJECT_SLUG,
                    request = GoogleLoginRequest(
                        token = googleToken,
                        deviceId = sessionManager.getDeviceId()
                    )
                )

                if (response.isSuccessful && response.body() != null) {

                    val body = response.body()!!

                    var finalUserId: String? = null

                    val meResponse = RetrofitClient.apiService.me(
                        NetworkConstants.PROJECT_SLUG,
                        "Bearer ${body.accessToken}"
                    )

                    if (meResponse.isSuccessful) {
                        finalUserId = meResponse.body()?.user?.userId
                    }

                    sessionManager.login(
                        username = "Google User",
                        access = body.accessToken,
                        refresh = body.refreshToken,
                        userId = finalUserId
                    )
                    fetchAndSyncToken()
                    onResult(true)

                } else {
                    onResult(false)
                }

            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    fun refreshSession(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val currentRefresh = sessionManager.refreshToken.firstOrNull()
                if (currentRefresh != null) {
                    val response = RetrofitClient.apiService.refreshToken(
                        projectSlug = NetworkConstants.PROJECT_SLUG,
                        request     = RefreshTokenRequest(
                            refreshToken = currentRefresh,
                            deviceId = sessionManager.getDeviceId()
                        )
                    )
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        sessionManager.updateTokens(body.accessToken, body.refreshToken)
                        onResult(true)
                        return@launch
                    }
                }
                onResult(false)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            sessionManager.setNotificationsEnabled(enabled)
            if (enabled) {
                FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users")
            }
        }
    }
    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch { sessionManager.setDarkMode(enabled) }
    }

    fun logout() {
        viewModelScope.launch { sessionManager.logout() }
    }

    class Factory(private val sessionManager: SessionManager) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionViewModel(sessionManager) as T
    }


}
