package com.param.currencyconverter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/**
 * Das Web-Pendant zu `MainActivity.onCreate` + `setContent`: [ComposeViewport]
 * hängt ein <canvas> in das übergebene DOM-Element und rendert die
 * Composables dort hinein.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        MaterialTheme {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Hallo vom Wasm-Gerüst")
            }
        }
    }
}
