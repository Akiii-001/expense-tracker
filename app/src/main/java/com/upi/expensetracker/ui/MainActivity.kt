package com.upi.expensetracker.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.upi.expensetracker.ui.theme.AppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result handled implicitly; app still works for manual entry */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()

        val focusTxnId = intent.getLongExtra(EXTRA_TXN_ID, -1L)

        setContent {
            AppTheme {
                Surface(modifier = Modifier, color = MaterialTheme.colorScheme.background) {
                    AppRoot(viewModel = viewModel, focusTransactionId = focusTxnId)
                }
            }
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: ExpenseViewModel, focusTransactionId: Long) {
    var tab by rememberSaveable { mutableStateOf(0) }
    // Consume the notification's "focus this transaction" only once, so it
    // doesn't re-open every time you come back to the Activity tab.
    var focusConsumed by rememberSaveable { mutableStateOf(false) }
    val effectiveFocus = if (focusConsumed) -1L else focusTransactionId

    Scaffold(
        topBar = { AppTopBar() },
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
