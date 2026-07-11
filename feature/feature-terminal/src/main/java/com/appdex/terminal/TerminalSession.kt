package com.appdex.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStream

/**
 * A non-interactive terminal session that executes commands via ProcessBuilder.
 * Supports command execution with output streaming and ANSI color parsing.
 */
class TerminalSession(
    private val workingDir: String,
    private val environment: Map<String, String> = emptyMap()
) {
    private val _output = MutableSharedFlow<TerminalOutput>(extraBufferCapacity = 64)
    val output: SharedFlow<TerminalOutput> = _output.asSharedFlow()

    private var currentProcess: Process? = null
    private var sessionJob: Job? = null

    val isAlive: Boolean get() = currentProcess?.isAlive == true

    fun execute(command: String, scope: kotlinx.coroutines.CoroutineScope) {
        sessionJob = scope.launch(Dispatchers.IO) {
            try {
                _output.emit(TerminalOutput.Prompt(workingDir))

                val processBuilder = ProcessBuilder("sh", "-c", command)
                    .directory(java.io.File(workingDir))
                    .redirectErrorStream(true)

                // Set environment
                processBuilder.environment().putAll(environment)
                processBuilder.environment()["TERM"] = "xterm-256color"
                processBuilder.environment()["PS1"] = "appdex@device:\$ "

                val process = processBuilder.start()
                currentProcess = process

                // Read output
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (isActive) {
                    line = reader.readLine()
                    if (line == null) break
                    _output.emit(TerminalOutput.Data(line))
                }

                process.waitFor()
                val exitCode = process.exitValue()
                _output.emit(TerminalOutput.Exit(exitCode))
                _output.emit(TerminalOutput.Prompt(workingDir))

            } catch (e: IOException) {
                _output.emit(TerminalOutput.Error(e.message ?: "Unknown error"))
                _output.emit(TerminalOutput.Prompt(workingDir))
            } catch (e: Exception) {
                _output.emit(TerminalOutput.Error(e.message ?: "Execution failed"))
                _output.emit(TerminalOutput.Prompt(workingDir))
            } finally {
                currentProcess = null
            }
        }
    }

    fun kill() {
        currentProcess?.destroyForcibly()
        sessionJob?.cancel()
    }
}

sealed interface TerminalOutput {
    data class Prompt(val workingDir: String) : TerminalOutput
    data class Data(val text: String) : TerminalOutput
    data class Exit(val code: Int) : TerminalOutput
    data class Error(val message: String) : TerminalOutput
}
