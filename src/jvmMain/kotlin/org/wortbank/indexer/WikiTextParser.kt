package org.wortbank.indexer

import edu.stanford.nlp.simple.*;

object WikiCleaner {
    val deCleaner = WikiPlainText(language = WikiPlainText.WikiLanguage.DE)
}


fun parseWikiText(wiki: String): TokenizedText {
    val content = WikiCleaner.deCleaner.clean(wiki)
    val doc = Document(content)
    return TokenizedText(doc.sentences().mapNotNull { it.lemmas() }.flatten())
}