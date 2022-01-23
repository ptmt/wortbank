package org.wortbank

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.features.StatusPages
import io.ktor.html.respondHtml
import io.ktor.http.content.resource
import io.ktor.http.content.static
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import kotlinx.html.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.wortbank.indexer.WikiIndexer
import org.wortbank.storage.*
import org.wortbank.wordle.WordleFilter


fun <T> Database.tx(statement: Transaction.() -> T): T {
    return transaction(1, 1, this) {
        // addLogger(StdOutSqlLogger)
        statement()
    }
}

fun <T> WortBankApplication.tx(statement: Transaction.() -> T) = db.tx(statement)

class WortBankApplication {
    val db = Database.connect("jdbc:sqlite:./index.db", "org.sqlite.JDBC").apply {
        tx {
            SchemaUtils.create(
                DBSources,
                DBLemmas,
                DBLemmasInDocuments,
                DBDocuments
            )
        }
    }

    private val storage = Storage(db)

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
                    sharedHead()
                    landingPage()
                }
            }
            get("/search") {
                val bank = call.request.queryParameters["bank"] ?: run {
                    call.respondRedirect("/")
                    return@get
                }
                val words = bank.split(",").map { it.trim() }
                val results = storage.search(words)
                call.respondHtml {
                    sharedHead("WortBank — Search Results")
                    searchResultPage(words, results)
                }
            }
            get("/stats") {
                call.respondHtml {
                    sharedHead("WortBank — Stats")
                    val lemmas = tx {
                        ELemma.all().limit(100).toList()
                    }
                    statsPage(lemmas.joinToString("\n") { it.lemma })
                }
            }
            post("/perform_index") {
                WikiIndexer(storage).perform()
                call.respondHtml {
                    sharedHead()
                    page {
                        ul {
                            li {
                                val sources = tx {
                                    ESource.count()
                                }
                                +"Sources: $sources"
                            }
                            li {
                                val documents = tx {
                                    EDocument.count()
                                }
                                +"Documents: $documents"
                            }
                            li {
                                val lemmas = tx {
                                    ELemma.count()
                                }
                                +"Lemmas: $lemmas"
                            }
                        }
                    }
                }
            }
            get("/prepare_wordle") {
                val (processed, notprocessed) = WordleFilter(storage).filterWords()
                call.respondHtml {
                    sharedHead()
                    page {
                        h2 {
                            +"Processed: $processed"

                        }
                        h2 {
                            +"Not processed: $notprocessed "
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