package org.wortbank

import kotlinx.html.*

fun HTML.landingPage() {
    coverPage {
        a(href = "/") {
            h1("cover-heading") {
                +"WortBank"
            }
        }
        form("/search", method = FormMethod.get) {
            p("lead") {
                textArea(rows = "3", classes = "form-control") {
                    this.name = "bank"
                    +"die Kopfhörer, das Ohr, Koffer, eigentlich, geschichte"
                }
            }
            submitInput(classes = "btn btn-lg btn-secondary") {
                this.value = "Search"
            }
        }
    }
}