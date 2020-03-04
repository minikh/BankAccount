package test

import io.ktor.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import test.CommandLineSupport.createOptions
import test.CommandLineSupport.getRepeatTime
import test.CommandLineSupport.getRepeatTimes
import test.CommandLineSupport.help
import test.application.bankApplicationServer
import test.bank.account.MoneyTransferService
import test.settings.DbSettings

fun main(args: Array<String>) {
    help()
    val parser: CommandLineParser = DefaultParser()
    val configs: CommandLine = parser.parse(createOptions(), args)
    val sleepAfterExceptionMs = getRepeatTime(configs)
    val repeatTimes = getRepeatTimes(configs)
    MoneyTransferService.setUp(sleepAfterExceptionMs, repeatTimes)

    DbSettings.createDb()
    DbSettings.createTestUsers()

    embeddedServer(Netty, 8080, module = Application::bankApplicationServer).start()
}


