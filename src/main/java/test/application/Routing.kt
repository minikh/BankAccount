package test.application

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.DefaultHeaders
import io.ktor.jackson.jackson
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import test.bank.account.MoneyTransaction
import test.bank.account.MoneyTransferService.getAllUsers
import test.bank.account.MoneyTransferService.transfer

fun Application.bankApplicationServer() {
    install(DefaultHeaders)
    install(CallLogging)
    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }
    install(Routing) {
        get("/users") {
            call.respond(getAllUsers())
        }

        post("/transfer") {
            val body = call.receive<MoneyTransaction>()
            call.respond(transfer(body))
        }
    }
}
