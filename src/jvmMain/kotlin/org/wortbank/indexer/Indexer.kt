package org.wortbank.indexer

import kotlinx.coroutines.yield
import java.io.FileReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamConstants
import javax.xml.stream.XMLStreamReader
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.stax.StAXSource


object Indexer {
    suspend fun perform(): StringBuilder {
        return StringBuilder().apply {
            appendln("Hello world!")
            val stream = openStream("dewiki.xml")
            stream.readSomething()
        }
    }

    private fun openStream(name: String): XMLStreamReader {
        val xif = XMLInputFactory.newInstance()
        val xsr = xif.createXMLStreamReader(FileReader(name))
        xsr.nextTag() // Advance to statements element
        return xsr
    }

    suspend fun XMLStreamReader.readSomething() {
        val tf = TransformerFactory.newInstance()
        val t = tf.newTransformer()
        while (nextTag() == XMLStreamConstants.START_ELEMENT) {
            val result = DOMResult()
            println(result)
            t.transform(StAXSource(this), result)
            val domNode = result.node
            println(domNode)
            yield()
        }
    }
}