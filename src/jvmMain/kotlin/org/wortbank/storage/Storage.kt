package org.wortbank.storage

import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.count
import org.jetbrains.exposed.sql.select
import org.wortbank.*
import java.time.Instant

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

    private fun totalAmountOfLemmas(documents: List<EntityID<Int>>): Map<EntityID<Int>, Int> {
        return db.tx {
            DBLemmasInDocuments.innerJoin(DBDocuments)
                .slice(DBLemmasInDocuments.id.count(), DBDocuments.id)
                .select { DBDocuments.id inList documents }
                .groupBy(DBDocuments.id)
                .map { it[DBDocuments.id] to it[DBLemmasInDocuments.id.count()] }
                .toMap()
        }
    }

    fun search(words: List<String>): List<SearchResult> {
        val res = db.tx {
            DBLemmasInDocuments.innerJoin(DBLemmas).innerJoin(
                    DBDocuments
                )
                .select { DBLemmas.lemma inList words }
                .orderBy(DBLemmasInDocuments.count)
                .map {
                    Pair(EDocument.wrapRow(it), Pair(it[DBLemmas.lemma], it[DBLemmasInDocuments.count]))
                }
        }
        val docs = res.groupBy({ it.first }) { it.second }
            .map { it.key to it.value.toMap() }
            .asSequence()
            .sortedByDescending { it.second.size }
            .take(100)

        val lemmasInDocs = totalAmountOfLemmas(docs.map { it.first.id }.toList())

        return docs.map { SearchResult(
            it.first,
            it.second.toMap(),
            lemmasInDocs[it.first.id] ?: 0) }.toList()
    }

    private fun Transaction.getOrPutLemma(lemma: String): ELemma {
        val existing = ELemma.find { DBLemmas.lemma eq lemma }.singleOrNull()
        return existing ?: ELemma.new {
            this.lemma = lemma.take(200)
        }
    }
}

data class SearchResult(val document: EDocument, val lemmas: Map<String, Int>, val totalUniqueLemmas: Int)