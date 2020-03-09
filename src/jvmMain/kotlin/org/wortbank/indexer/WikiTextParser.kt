package org.wortbank.indexer

object WikiCleaner {
    val deCleaner = WikiPlainText(language = WikiPlainText.WikiLanguage.DE)
}



fun parseWikiText(wiki: String): TokenizedText {
    val content = WikiCleaner.deCleaner.clean(wiki)
    return TokenizedText(content.split(" "))
}