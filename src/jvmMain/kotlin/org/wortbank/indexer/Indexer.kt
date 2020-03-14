package org.wortbank.indexer

interface Indexer {
    val sourceName: String
    val storage: Storage
    suspend fun perform()
}