package org.wortbank.indexer

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
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

    private fun Transaction.getOrPutLemma(lemma: String): ELemma {
        val existing = ELemma.find { DBLemmas.lemma eq lemma }.singleOrNull()
        return existing ?: ELemma.new {
            this.lemma = lemma
        }
    }
}