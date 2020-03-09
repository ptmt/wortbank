package org.wortbank

import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.html.*
import org.jetbrains.exposed.sql.Database
import java.io.*

actual class Sample {
    actual fun checkMe() = 42
}

actual object Platform {
    actual val name: String = "JVM"
}

fun main() {
    embeddedServer(Netty,
        watchPaths = listOf("wortbank"),
        port = 8080,
        module = Application::wortbank).start(wait = true)
}

fun Application.wortbank() {
    val currentDir = File(".").absoluteFile
    environment.log.info("Current directory: $currentDir")
    WortBankApplication().apply { main() }
}