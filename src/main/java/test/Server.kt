package test

import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import test.application.bankApplicationServer
import test.settings.DbSettings

fun <C : Any> C.getLogger(): Logger = LoggerFactory.getLogger(this::class.java.name.substringBefore("\$Companion"))

fun main() {
    DbSettings.createDb()
    DbSettings.createTestUsers()

    embeddedServer(Netty, 8080, watchPaths = listOf("Money transfer app"), module = Application::bankApplicationServer).start()
}


