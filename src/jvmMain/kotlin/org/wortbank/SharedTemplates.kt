package org.wortbank

import kotlinx.html.*

fun HTML.sharedHead(title: String = "WortBank — search specific texts based on given set of words") {
    this@sharedHead.head {
        title(title)
        styleLink("https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/css/bootstrap.min.css")
        styleLink("/static/wortbank.css")
        script(src = "https://code.jquery.com/jquery-3.4.1.slim.min.js") {}
        script(src = "https://cdn.jsdelivr.net/npm/popper.js@1.16.0/dist/umd/popper.min.js") {}
        script(src = "https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js") {}
    }
}

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