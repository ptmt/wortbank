package org.wortbank

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object DBSources: IntIdTable() {
    val name = varchar("name", 255).uniqueIndex()
    val url = varchar("url", 255)
}

object DBDocuments: IntIdTable() {
    val title = varchar("name", 255).uniqueIndex()
    val parsedAt = long("parsedAt")
    val url = varchar("url", 255)
    val provider = reference("provider", DBSources)
}

object DBLemmas: IntIdTable() {
    val lemma = varchar("lemma", 255).uniqueIndex()
}

object DBLemmasInDocuments: IntIdTable() {
    val count = integer("count")
    val document = reference("document", DBDocuments)
    val lemma = reference("lemma", DBLemmas)
}

class ESource(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ESource>(DBSources)

    var name by DBSources.name
    var url by DBSources.url
}

class ELemma(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ELemma>(DBLemmas)

    var lemma by DBLemmas.lemma
}

class EDocument(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EDocument>(DBDocuments)

    var title by DBDocuments.title
    var parsedAt by DBDocuments.parsedAt
    var url by DBDocuments.url
    var provider by DBDocuments.provider

}

class ELemmaInDocument(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ELemmaInDocument>(DBLemmasInDocuments)

    var count by DBLemmasInDocuments.count
    var lemma by DBLemmasInDocuments.lemma
    var document by DBLemmasInDocuments.document
}