package org.wortbank

import kotlinx.html.*
import org.wortbank.indexer.SearchResult
import org.wortbank.main

fun HTML.searchResultPage(query: String, result: List<SearchResult>) {
    page {
        +query

        h3 { +"Results" }

        result.forEach {
            div {
                a(href = it.document.url) { +it.document.title }
                p {
                    small {
                        +it.lemmas.entries.joinToString(", ") { "${it.key} (${it.value})" }
                    }
                }
            }
        }

    }
}