package com.ianhanniballake.contractiontimer.extensions

import android.content.BroadcastReceiver
import android.os.Build
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Run work asynchronously from a [BroadcastReceiver].
 */
fun BroadcastReceiver.goAsync(block: suspend () -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        val pendingResult = goAsync()
        GlobalScope.launch {
            block()
            pendingResult.finish()
        }
    } else {
        GlobalScope.launch {
            block()
        }
    }
}