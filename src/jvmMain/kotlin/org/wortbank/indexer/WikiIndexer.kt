package org.wortbank.indexer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader
import org.wortbank.storage.ESource
import org.wortbank.storage.Storage
import org.xml.sax.helpers.DefaultHandler
import java.io.FileInputStream
import java.io.InputStream
import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory

class WikiIndexer(override val storage: Storage) : Indexer {
    class Page(val title: String, val text: String)

    override val sourceName = "WIKI_DE"
    private val fileSource = "./dewiki.xml"
    private val url = "https://de.wikipedia.org/wiki/"

    override suspend fun perform() {
        val source = storage.getOrPutSource(sourceName, url)
        val channel = openStream(fileSource)
        // val channel = stream.readPages(source)
        for(page in channel) {
            savePage(source, page)
        }
    }

    private fun openStream(name: String): Channel<Page> {
        val xif = XMLInputFactory.newInstance()
        if (!File(name).exists()) {
            error("$name is not exists")
        }
        val saxParser = SAXParserFactory.newInstance().also {
            it.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, false);
        }.newSAXParser()

        val pages = Channel<Page>(Int.MAX_VALUE)

        val defaultHandler = object : DefaultHandler() {
            var currentValue = ""
            var currentElement = false
            var text: String? = null
            var title: String? = null
            var skip = true

            override fun startElement(uri: String, localName: String, qName: String, attributes: org.xml.sax.Attributes) {
                if (qName in listOf("title", "text")) {
                    currentElement = true
                    currentValue = ""
                }
            }
            //overriding the endElement() method of DefaultHandler
            override fun endElement(uri: String, localName: String, qName: String) {
                currentElement = false
                when (qName) {
                   // "page" -> println(">> page $currentValue")
                    "title" -> title = currentValue
                    "text" -> text = currentValue
                }
                if (title != null && text != null) {
                    if (!skip && title!!.length < 200) {
                        pages.offer(Page(title!!, text!!))
                    } else {
                        println("skip $title")
                    }
                    title = null
                    text = null
                }
            }
            //overriding the characters() method of DefaultHandler
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (currentElement) {
                    if (title == "Verordnung über die Aufstellung von Betriebskosten") {
                        skip = false
                    }
                    currentValue += String(ch, start, length)
                }
            }
        }


        val inputStream = FileInputStream(name)
        GlobalScope.launch(Dispatchers.Default) {
            saxParser.parse(inputStream, defaultHandler)
        }
//        val xsr = xif.createXMLStreamReader(FileReader(name))
//
//        xsr.nextTag() // Advance to statements element
//        return xsr
        return pages
    }

    private fun XMLStreamReader.readFieldIfPresent(name: String): String? {
        if (this.hasName() && this.localName == name) {
            next()
            if (!hasText()) {
                println("$name has no text")
                return null
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

    private fun XMLStreamReader.readPage(): Page? {
        var parsedTitle: String? = null
        var parsedText: String? = null
        while (hasNext() && !(this.eventType == XMLStreamReader.END_ELEMENT && this.localName == "page")) {
            readFieldIfPresent("title")?.let { parsedTitle = it }
            readFieldIfPresent("text")?.let { parsedText = it }
            next()
        }
        return if (parsedTitle != null && parsedText != null) {
             Page(
                parsedTitle ?: error("title has not been found"),
                parsedText ?: error("text has not been found")
            )
        } else {
            null
        }
    }

    data class TokenizedPage(val title: String, val lemmas: Map<String, Int>)

    private suspend fun XMLStreamReader.readPages(source: ESource) {
        var i = 0

        while (hasNext()) {
            if (this@readPages.eventType == XMLStreamReader.START_ELEMENT && this@readPages.localName == "page") {
                val page = readPage()
                savePage(source, page)
                i++
            }
            next()
        }

//        val topTokens = frequencyMap.entries.sortedByDescending { it.value }.take(30).joinToString("\n")
//        println(topTokens)
    }

    private suspend fun savePage(source: ESource, page: Page?) {
        val frequencyMap = mutableMapOf<String, Int>()
        if (page != null && !storage.existPage(page.title)) {
            val parsedPage = parseWikiText(page.title, page.text)
            println("parsed title <${page.title}> text length ${page.text.length}, total tokens ${parsedPage.lemmas.size}")
            parsedPage.lemmas.forEach {
                val prev = frequencyMap.getOrPut(it) { 0 }
                frequencyMap[it] = prev + 1
            }
            storage.savePage(source, page.title, frequencyMap)
            yield()
        } else {
            println("Skip ${page?.title}")
        }
    }
}