package org.wortbank

import kotlinx.html.*
import org.wortbank.main

fun HTML.searchResultPage(query: String, result: String) {
    page {
        +query

        h3 { +"Results" }


        code {
            pre {
                +result
            }
        }
    }
}