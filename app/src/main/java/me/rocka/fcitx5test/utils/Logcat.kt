package me.rocka.fcitx5test.utils

import android.os.Process
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@Suppress("BlockingMethodInNonBlockingContext")
class Logcat : CoroutineScope by CoroutineScope(Dispatchers.IO) {

    private var process: java.lang.Process? = null
    private var emittingJob: Job? = null

    private val flow: MutableSharedFlow<String> = MutableSharedFlow()

    /**
     * Subscribe to this flow to receive log in app
     * Nothing would be emitted until [initLogFlow] was called
     */
    val logFlow: SharedFlow<String> by lazy { flow.asSharedFlow() }

    /**
     * Get a snapshot of logcat
     */
    fun getLogAsync(): Deferred<List<String>> = async {
        Runtime.getRuntime()
            .exec(arrayOf("logcat", "--pid=${Process.myPid()}", "-d"))
            .inputStream
            .bufferedReader()
            .readLines()
    }

    /**
     * Clear logcat
     */
    fun clearLog(): Job =
        launch {
            Runtime.getRuntime().exec(arrayOf("logcat", "-c"))
        }

    /**
     * Create a process reading logcat, sending lines to [logFlow]
     */
    fun initLogFlow() =
        if (process != null)
            throw IllegalStateException("Logcat process already created!")
        else launch {
            Runtime
                .getRuntime()
                .exec(arrayOf("logcat", "--pid=${Process.myPid()}"))
                .also { process = it }
                .inputStream
                .bufferedReader()
                .lineSequence()
                .asFlow()
                .flowOn(Dispatchers.IO)
                .cancellable()
                .collect { flow.emit(it) }
        }.also { emittingJob = it }

    /**
     * Destroy the reading process
     */
    fun shutdownLogFlow() {
        process?.destroy()
        emittingJob?.cancel()
    }
}
