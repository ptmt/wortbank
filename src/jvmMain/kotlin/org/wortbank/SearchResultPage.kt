package org.wortbank

import kotlinx.html.*
import org.wortbank.storage.SearchResult
import kotlin.math.roundToInt

fun HTML.searchResultPage(query: List<String>, result: List<SearchResult>) {
    page {

        h3 { +"Results: ${result.size}" }

        code {
            +query.joinToString(", ")
        }

        div()

        result.forEach {
            div(classes = "card w-75") {
                div(classes = "card-body") {

                    h5(classes = "card-title") {
                        +it.document.title
                    }

                    h6(classes = "card-subtitle mb-2 text-muted") {
                        +"Found: ${it.lemmas.size} (${((it.lemmas.size.toDouble() / query.size.toDouble()) * 100.0).roundToInt()}%)"
                    }

                    p(classes = "card-text") {
                        small {
                            +it.lemmas.entries.joinToString(", ") { "${it.key} (${it.value})" }
                        }
                    }
                    a(classes = "card-link", href = it.document.url) { +it.document.url }
                }
            }
        }

    }
}