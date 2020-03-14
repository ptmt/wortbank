package org.wortbank

import kotlinx.html.*

fun BODY.menu() {
    header("masthead mb-auto") {
        div("inner") {
            h3("masthead-brand") {
                +"WortBank"
            }
            nav("nav nav-masthead justify-content-center") {
                a(href = "#", classes = "nav-link active") { +"Home" }
                a(href = "/howitworks", classes = "nav-link") { +"How it works" }
                a(href = "/stats", classes = "nav-link") { +"Stats" }
            }
        }
    }
}

fun HTML.coverPage(inner: MAIN.() -> Unit) {
    body("text-center") {
        div("cover-container d-flex w-100 h-100 p-3 mx-auto flex-column") {
            this@body.menu()
            main("inner cover") {
                role = "main"
                inner()
            }
            footer("mastfoot mt-auto") {

            }
        }
    }
}

fun HTML.page(inner: MAIN.() -> Unit) {
    body() {
        div("d-flex w-100 p-3 mx-auto flex-column") {
            this@body.menu()
            main("container") {
                role = "main"
                inner()
            }
        }
    }
}