package org.wortbank

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.html.respondHtml
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import kotlinx.html.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.sqlite.SQLiteConfig
import org.wortbank.indexer.Indexer

fun HTML.sharedHeader(title: String) {
    head {
        title(title)
        styleLink("https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css")
        styleLink("/static/wortbank.css")
    }
}

fun <T> Database.tx(statement: Transaction.() -> T): T {
    return transaction(1, 1, this) {
        addLogger(StdOutSqlLogger)
        statement()
    }
}

fun <T> WortBankApplication.tx(statement: Transaction.() -> T) = db.tx(statement)

class WortBankApplication {
    val db = Database.connect("jdbc:sqlite:./index.db", "org.sqlite.JDBC").apply {
        tx {
            SchemaUtils.create(DBSources, DBWords)
        }
    }


    fun Application.main() {
        /**
         * First we install the features we need. They are bound to the whole application.
         * Since this method has an implicit [Application] receiver that supports the [install] method.
         */
        // This adds automatically Date and Server headers to each response, and would allow you to configure
        // additional headers served to each response.
        install(DefaultHeaders)
        // This uses use the logger to log every call (request/response)
        install(CallLogging)

        install(StatusPages) {
            exception<Throwable> { cause ->
                environment.log.error(cause.message)
                cause.printStackTrace()
                // val error = HttpBinError(code = HttpStatusCode.InternalServerError, request = call.request.local.uri, message = cause.toString(), cause = cause)
                call.respond(cause)
            }
        }

        routing {
            get("/") {
                call.respondHtml {
                    sharedHeader("WortBank — search specific texts based on given set of words")
                    landingPage()
                }
            }
            get("/perform_index") {
                val result = Indexer.perform(db)
                call.respondHtml {
                    sharedHeader("WortBank — search specific texts based on given set of words")
                    body {
                        div {
                            val sources = tx {
                                ESource.count()
                            }
                            +"Sources: $sources"
                        }
                        div {
                            +result.toString()
                        }
                    }
                }
            }
            static("/static") {
                resource("wortbank.css")
                resource("wortbank.js")
            }
        }
    }
}