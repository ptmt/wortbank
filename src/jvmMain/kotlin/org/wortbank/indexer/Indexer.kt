package org.wortbank.indexer

import org.wortbank.storage.Storage

interface Indexer {
    val sourceName: String
    val storage: Storage
    suspend fun perform()
}