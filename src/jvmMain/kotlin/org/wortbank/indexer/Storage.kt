package org.wortbank.indexer

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.select
import org.wortbank.*
import java.time.Instant
import java.util.*

class Storage(private val db: Database) {
    fun getOrPutSource(sourceName: String, sourceUrl: String): ESource {
        return db.tx {
            val existing = ESource.find { DBSources.name eq sourceName }.singleOrNull()
            existing ?: ESource.new {
                this.name = sourceName
                this.url = sourceUrl
            }
        }
    }

    fun savePage(source: ESource, title: String, lemmas: Map<String, Int>) {
        val document = db.tx {
            val existing = EDocument.find { DBDocuments.title eq title }.singleOrNull()
            existing ?: EDocument.new {
                this.title = title
                this.url = source.url + title
                this.parsedAt = Instant.now().epochSecond
                this.provider = source.id
            }
        }
        lemmas.entries.forEach {
            db.tx {
                val lemma = getOrPutLemma(it.key)
                ELemmaInDocument.new {
                    this.count = it.value
                    this.document = document.id
                    this.lemma = lemma.id
                }
            }
        }
    }

    fun search(words: List<String>): List<SearchResult> {
        val res = db.tx {
            DBLemmasInDocuments.innerJoin(DBLemmas).innerJoin(DBDocuments)
                .select { DBLemmas.lemma inList words }
                .orderBy(DBLemmasInDocuments.count)
                .map {
                    Pair(EDocument.wrapRow(it), Pair(it[DBLemmas.lemma], it[DBLemmasInDocuments.count]))
                }
        }
        val searchResults = res.groupBy({ it.first }) {
            it.second
        }.map { SearchResult(it.key, it.value.toMap()) }
        //.mapValues { it.value.groupBy({ it.second }) { it.third } }

//        s.forEach {
//            println("res in ${it.key.title} keys ${it.value.keys} total ${it.value.values.sumBy { it.sum() }}")
//        }
        return searchResults
    }

    private fun Transaction.getOrPutLemma(lemma: String): ELemma {
        val existing = ELemma.find { DBLemmas.lemma eq lemma }.singleOrNull()
        return existing ?: ELemma.new {
            this.lemma = lemma
        }
    }
}

data class SearchResult(val document: EDocument, val lemmas: Map<String, Int>)