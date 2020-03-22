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
//    <nav class="navbar navbar-expand-md navbar-dark bg-dark mb-4">
//    <a class="navbar-brand" href="#">Top navbar</a>
//    <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
//    <span class="navbar-toggler-icon"></span>
//    </button>
//    <div class="collapse navbar-collapse" id="navbarCollapse">
//    <ul class="navbar-nav mr-auto">
//    <li class="nav-item active">
//    <a class="nav-link" href="#">Home <span class="sr-only">(current)</span></a>
//    </li>
//    <li class="nav-item">
//    <a class="nav-link" href="#">Link</a>
//    </li>
//    <li class="nav-item">
//    <a class="nav-link disabled" href="#" tabindex="-1" aria-disabled="true">Disabled</a>
//    </li>
//    </ul>
//    <form class="form-inline mt-2 mt-md-0">
//    <input class="form-control mr-sm-2" type="text" placeholder="Search" aria-label="Search">
//    <button class="btn btn-outline-success my-2 my-sm-0" type="submit">Search</button>
//    </form>
//    </div>
//    </nav>
    nav("navbar navbar-expand-md navbar-dark bg-dark mb-4") {
        a(href = "/", classes = "navbar-brand") { +"Wortbank" }
        button(classes = "navbar-toggler", type = ButtonType.button) {
            // data-toggle="collapse" data-target="#navbarCollapse" aria-controls="navbarCollapse" aria-expanded="false" aria-label="Toggle navigation">
            span(classes = "navbar-toggler-icon") {}
        }
        div(classes = "collapse navbar-collapse") {
            this.id = "navbarCollapse"
            ul(classes = "navbar-nav mr-auto") {
                li(classes = "nav-item") {
                    a(classes = "nav-link", href = "/") { +"Home" }
                }
                li(classes = "nav-item") {
                    a(classes = "nav-link", href = "/howitworks") { +"How it works" }
                }
                li(classes = "nav-item") {
                    a(classes = "nav-link", href = "htpts://github.com/ptmt/wortbank") { +"GitHub" }
                }
            }
        }
    }
}


fun HTML.page(inner: MAIN.() -> Unit) {
    body {
        this@body.menu()
        main("container") {
            role = "main"
            inner()
        }
    }
}