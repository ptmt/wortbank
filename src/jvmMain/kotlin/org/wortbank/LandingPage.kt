package org.wortbank

import kotlinx.html.*

fun HTML.landingPage() {
    page {
        p() {
            +"Enter up to 100 words below:"
        }
        form("/search", method = FormMethod.get) {
            p("lead") {
                textArea(rows = "3", classes = "form-control") {
                    this.name = "bank"
                    +"Kopfhörer, Ohr, Koffer, eigentlich, geschichte, dumm, sich streiten, Wunsch, verlieren, Richtung, ganz, sogenanten, bestehen, entdecken, unbedingt"
                }
            }
            submitInput(classes = "btn btn-lg btn-secondary") {
                this.value = "Search"
            }
        }
    }
}