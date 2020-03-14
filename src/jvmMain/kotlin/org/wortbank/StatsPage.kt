package org.wortbank

import kotlinx.html.*


fun HTML.statsPage(stats: String) {
    page {
        h1("cover-heading") {
            +"Statistics:"
        }

        code {
            pre {
                +stats
            }
        }

    }
}