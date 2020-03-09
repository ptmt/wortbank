package org.wortbank

import kotlinx.html.*

fun HTML.landingPage() {
    body("text-center") {
        div("cover-container d-flex w-100 h-100 p-3 mx-auto flex-column") {
            header("masthead mb-auto") {
                div("inner") {
                    h3("masthead-brand") {
                        +"WortBank"
                    }
                    nav("nav nav-masthead justify-content-center") {
                        a(href="#", classes="nav-link active") { +"Home" }
                        a(href="/howitworks", classes = "nav-link") { +"How it works" }
                    }
                }
            }

            main("inner cover") {
                role = "main"
                h1("cover-heading") {
                    +"WortBank"
                }
                p("lead") {
                    textArea(rows = "3", classes = "form-control") {  }
                }
                button(classes = "btn btn-lg btn-secondary") { +"Search" }
            }
            footer("mastfoot mt-auto") {

            }

        }

    }
}