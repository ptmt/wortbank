package org.wortbank.indexer

import kotlinx.coroutines.yield
import java.io.File
import java.io.FileReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader
import org.wortbank.storage.ESource
import org.wortbank.storage.Storage

class WikiIndexer(override val storage: Storage) : Indexer {
    class Page(val title: String, val text: String)

    override val sourceName = "WIKI_DE"
    private val fileSource = "./dewiki.xml"
    private val url = "https://de.wikipedia.org/wiki/"

    override suspend fun perform() {
        val stream = openStream(fileSource)
        val source = storage.getOrPutSource(sourceName, url)
        val channel = stream.readPages(source)
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

    data class TokenizedPage(val title: String, val lemmas: Map<String, Int>)

    private suspend fun XMLStreamReader.readPages(source: ESource) {
        var i = 0

        while (hasNext()) {
            if (this@readPages.eventType == XMLStreamReader.START_ELEMENT && this@readPages.localName == "page") {
                val frequencyMap = mutableMapOf<String, Int>()
                val page = readPage()
                if (!storage.existPage(page.title)) {
                    val parsedPage = parseWikiText(page.title, page.text)
                    println("parsed title <${page.title}> text length ${page.text.length}, total tokens ${parsedPage.lemmas.size}")
                    parsedPage.lemmas.forEach {
                        val prev = frequencyMap.getOrPut(it) { 0 }
                        frequencyMap[it] = prev + 1
                    }
                    // channel.send(TokenizedPage(page.title, frequencyMap))
                    storage.savePage(source, page.title, frequencyMap)
                } else {
                    println("Skip ${page.title}")
                }
                yield()
                i++
            }
            next()
        }


//        val topTokens = frequencyMap.entries.sortedByDescending { it.value }.take(30).joinToString("\n")
//        println(topTokens)
    }
}