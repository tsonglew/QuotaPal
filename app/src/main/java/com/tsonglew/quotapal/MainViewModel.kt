package com.tsonglew.quotapal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tsonglew.quotapal.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant

data class LoginState(val challenge: DeviceChallenge? = null, val error: String? = null)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as QuotaPalApplication
    val state = app.repository.state
    val preferences = app.settings.flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferencesState())
    private val mutableLogin = MutableStateFlow<LoginState?>(null)
    val login: StateFlow<LoginState?> = mutableLogin
    private var loginJob: Job? = null

    init { onForeground() }
    fun onForeground() {
        viewModelScope.launch {
            app.repository.initialize()
            if (state.value.connected) { app.repository.refresh(); app.updateWidgets() }
            app.reconcileSync()
        }
    }
    fun refresh() { viewModelScope.launch { app.repository.refresh(true); app.updateWidgets() } }
    fun demo() { viewModelScope.launch { cancelLogin(); app.repository.demo(); app.reconcileSync(); app.updateWidgets() } }
    fun logout() { cancelLogin(); app.scope.launch { app.repository.logout(); app.reconcileSync(); app.updateWidgets() } }
    fun showRemaining(value: Boolean) { viewModelScope.launch { app.settings.showRemaining(value); app.updateWidgets() } }
    fun theme(value: String) { viewModelScope.launch { app.settings.theme(value) } }
    fun refreshMinutes(value: Long) { viewModelScope.launch { app.settings.refreshMinutes(value); app.reconcileSync() } }

    fun startLogin() {
        cancelLogin()
        mutableLogin.value = LoginState()
        loginJob = viewModelScope.launch {
            try {
                val challenge = app.api.challenge()
                mutableLogin.value = LoginState(challenge)
                while (Instant.now().epochSecond < challenge.expiresAt) {
                    delay(challenge.intervalSeconds * 1000)
                    val grant = app.api.poll(challenge) ?: continue
                    val session = app.api.exchange(grant)
                    app.repository.connect(session)
                    app.reconcileSync(); app.updateWidgets()
                    mutableLogin.value = null
                    return@launch
                }
                mutableLogin.update { it?.copy(error = "授权已超时，请重新获取登录码") }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: ApiFailure) { mutableLogin.update { it?.copy(error = failure.kind.userMessage()) } }
            catch (_: Exception) { mutableLogin.update { it?.copy(error = "连接未完成，请重试") } }
        }
    }
    fun cancelLogin() {
        loginJob?.cancel(); loginJob = null
        app.repository.cancelPendingConnection()
        mutableLogin.value = null
    }
}
