package com.upi.expensetracker.ui

import android.Manifest
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.upi.expensetracker.ui.theme.AppTheme

class MainActivity : FragmentActivity() {

    private val viewModel: ExpenseViewModel by viewModels()
    private lateinit var prefs: SharedPreferences

    private var lockEnabled by mutableStateOf(false)
    private var unlocked by mutableStateOf(true)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result handled implicitly; app still works for manual entry */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()

        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        lockEnabled = prefs.getBoolean(KEY_LOCK, false)
        unlocked = !lockEnabled

        val focusTxnId = intent.getLongExtra(EXTRA_TXN_ID, -1L)

        setContent {
            AppTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    if (lockEnabled && !unlocked) {
                        LockScreen(onUnlock = { promptAuth() })
                        LaunchedEffect(Unit) { promptAuth() }
                    } else {
                        AppRoot(
                            viewModel = viewModel,
                            focusTransactionId = focusTxnId,
                            lockEnabled = lockEnabled,
                            onToggleLock = { setLock(it) }
                        )
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Re-lock when the app goes to the background.
        if (lockEnabled) unlocked = false
    }

    private fun setLock(enable: Boolean) {
        lockEnabled = enable
        prefs.edit().putBoolean(KEY_LOCK, enable).apply()
        unlocked = true // we're already inside the app when toggling
    }

    private fun promptAuth() {
        val bm = BiometricManager.from(this)
        if (bm.canAuthenticate(AUTHENTICATORS) != BiometricManager.BIOMETRIC_SUCCESS) {
            // No fingerprint/PIN set up (or unsupported combo); don't lock the user out.
            unlocked = true
            return
        }
        try {
            val prompt = BiometricPrompt(
                this,
                ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        unlocked = true
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock UPI Expense Tracker")
                .setSubtitle("Use your fingerprint or device PIN/pattern")
                .setAllowedAuthenticators(AUTHENTICATORS)
                .build()
            prompt.authenticate(info)
        } catch (e: Exception) {
            // If the device can't show the prompt for any reason, fail open.
            unlocked = true
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    companion object {
        const val EXTRA_TXN_ID = "extra_txn_id"
        private const val KEY_LOCK = "app_lock"
        private val AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text("Locked", style = MaterialTheme.typography.titleLarge)
        Text(
            "Unlock to view your money",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
        )
        Button(onClick = onUnlock) { Text("Unlock") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(
    viewModel: ExpenseViewModel,
    focusTransactionId: Long,
    lockEnabled: Boolean,
    onToggleLock: (Boolean) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var focusConsumed by rememberSaveable { mutableStateOf(false) }
    val effectiveFocus = if (focusConsumed) -1L else focusTransactionId
    val categoryStyles by viewModel.categoryStyles.collectAsState()

    CompositionLocalProvider(LocalCategoryStyles provides categoryStyles) {
        if (showSettings) {
            SettingsScreen(
                viewModel = viewModel,
                lockEnabled = lockEnabled,
                onToggleLock = onToggleLock,
                onBack = { showSettings = false }
            )
        } else {
            Scaffold(
                topBar = { AppTopBar(onSettings = { showSettings = true }) },
                bottomBar = { AppBottomBar(selected = tab, onSelect = { tab = it }) }
            ) { padding ->
                when (tab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        focusTransactionId = effectiveFocus,
                        onFocusConsumed = { focusConsumed = true },
                        modifier = Modifier.padding(padding)
                    )
                    1 -> ReportsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(padding)
                    )
                    else -> BudgetsScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}
