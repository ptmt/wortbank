package org.wortbank.indexer

import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import org.jetbrains.exposed.sql.Database
import org.wortbank.indexer.Indexer.readFieldIfPresent
import java.io.File
import java.io.FileReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.stax.StAXSource

class Page(val title: String, val text: String)

object Indexer {
    suspend fun perform(db: Database): StringBuilder {
        return StringBuilder().apply {
            appendln("Hello world!")
            val stream = openStream("./dewiki.xml")
            stream.readPages()
        }
    }

    private fun openStream(name: String): XMLStreamReader {
        val xif = XMLInputFactory.newInstance()
        if (!File(name).exists()) {
            error("$name is not exists")
        }
        val xsr = xif.createXMLStreamReader(FileReader(name))
        xsr.nextTag() // Advance to statements element
        return xsr
    }

    private fun XMLStreamReader.readFieldIfPresent(name: String): String? {
        if (this.hasName() && this.localName == name) {
            next()
            if (!hasText()) {
                error("invalid state, element should have text content")
            }
            val str = StringBuilder()
            while (hasText()) {
                str.append(this.text)
                next()
            }
            return str.toString()
        }
        return null
    }

    private fun XMLStreamReader.readPage(): Page {
        var parsedTitle: String? = null
        var parsedText: String? = null
        while (hasNext() && !(this.eventType == XMLStreamReader.END_ELEMENT && this.localName == "page")) {
            readFieldIfPresent("title")?.let { parsedTitle = it }
            readFieldIfPresent("text")?.let { parsedText = it }
            next()
        }
        return Page(parsedTitle ?: error("title has not been found"), parsedText ?: error("text has not been found"))
    }

    private suspend fun XMLStreamReader.readPages() {
        var i = 0
        while (hasNext() && i < 10) {
            if (this.eventType == XMLStreamReader.START_ELEMENT && this.localName == "page") {
                val page = readPage()
                val parsedPage = parseWikiText(page.text)
                println("parsed title <${page.title}> text length ${page.text.length}, total tokens ${parsedPage.tokens.size}")
                println(parsedPage.tokens.take(30))
                yield()
                i++
            }
            next()
        }
    }
}