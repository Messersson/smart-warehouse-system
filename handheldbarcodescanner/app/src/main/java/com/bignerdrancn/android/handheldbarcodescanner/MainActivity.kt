package com.bignerdrancn.android.handheldbarcodescanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bignerdrancn.android.handheldbarcodescanner.ui.WarehouseScannerApp
import com.bignerdrancn.android.handheldbarcodescanner.ui.theme.HandheldBarcodeScannerTheme

class MainActivity : ComponentActivity() {
    private var scanCallback: ((String) -> Unit)? = null
    private var captureHardwareScanner = false
    private val keyBuffer = StringBuilder()
    private var lastKeyAt = 0L

    private val scannerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            extractScanCode(intent)?.let(::emitScan)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        registerScannerReceiver()

        setContent {
            var scannedCode by remember { mutableStateOf<String?>(null) }

            DisposableEffect(Unit) {
                scanCallback = { scannedCode = it }
                onDispose { scanCallback = null }
            }

            HandheldBarcodeScannerTheme {
                WarehouseScannerApp(
                    scannedCode = scannedCode,
                    onScannedCodeConsumed = { scannedCode = null },
                    onScannerCaptureChanged = { captureHardwareScanner = it }
                )
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(scannerReceiver)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        extractScanCode(intent)?.let(::emitScan)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (!captureHardwareScanner || event.action != KeyEvent.ACTION_DOWN) {
            return super.dispatchKeyEvent(event)
        }

        if (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_TAB) {
            val code = keyBuffer.toString().trim()
            keyBuffer.clear()
            if (code.isNotBlank()) {
                emitScan(code)
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        val now = SystemClock.elapsedRealtime()
        if (now - lastKeyAt > KEY_BUFFER_TIMEOUT_MS) {
            keyBuffer.clear()
        }
        lastKeyAt = now

        val unicode = event.unicodeChar
        if (unicode > 0 && !event.isAltPressed && !event.isCtrlPressed && !event.isMetaPressed) {
            keyBuffer.append(unicode.toChar())
        }

        return super.dispatchKeyEvent(event)
    }

    private fun registerScannerReceiver() {
        val filter = IntentFilter().apply {
            SCANNER_ACTIONS.forEach(::addAction)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scannerReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(scannerReceiver, filter)
        }
    }

    private fun emitScan(code: String) {
        val trimmed = code.trim()
        if (trimmed.isNotBlank()) {
            scanCallback?.invoke(trimmed)
        }
    }

    private fun extractScanCode(intent: Intent?): String? {
        if (intent == null) {
            return null
        }

        for (key in SCANNER_EXTRA_KEYS) {
            intent.getStringExtra(key)?.trim()?.takeIf { it.isNotBlank() }?.let {
                return it
            }
            intent.getByteArrayExtra(key)?.toString(Charsets.UTF_8)?.trim()?.takeIf { it.isNotBlank() }?.let {
                return it
            }
        }

        return intent.dataString?.trim()?.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val KEY_BUFFER_TIMEOUT_MS = 700L

        private val SCANNER_ACTIONS = listOf(
            "com.wms.SCAN",
            "android.intent.ACTION_DECODE_DATA",
            "android.intent.action.SCANRESULT",
            "nlscan.action.SCANNER_RESULT",
            "com.honeywell.decode.intent.action.SCAN_RESULT",
            "com.honeywell.scan.intent.SCAN",
            "com.android.server.scannerservice.broadcast",
            "com.symbol.datawedge.api.RESULT_ACTION",
            "com.symbol.datawedge.SCAN",
            "com.seuic.scan",
            "scannerdata"
        )

        private val SCANNER_EXTRA_KEYS = listOf(
            "com.symbol.datawedge.data_string",
            "data_string",
            "barcode",
            "barocode",
            "barcode_string",
            "SCAN_BARCODE1",
            "scan_result",
            "SCAN_RESULT",
            "value",
            "code",
            "data",
            "scannerdata",
            "EXTRA_SCAN_DATA"
        )
    }
}
