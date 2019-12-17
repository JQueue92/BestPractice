package com.jqueue

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


fun LifecycleOwner.lifecycleIOScope(lifecycleOwner: LifecycleOwner, run: suspend () -> Unit) {
    lifecycleOwner.lifecycleScope.launch {
        withContext(Dispatchers.IO + SupervisorJob()) {
            run()
        }
    }
}

fun main() {
    println()
}