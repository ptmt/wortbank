package org.wortbank

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

object DBSources: IntIdTable() {
    val name = varchar("name", 255)
    val size = long("size")
    val url = varchar("url", 255)
}

object DBWords: IntIdTable() {
    val word = varchar("word", 255)
}

class ESource(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<ESource>(DBSources)

    var name by DBSources.name
    var size by DBSources.size
    var url by DBSources.url
}

class EWord(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<EWord>(DBWords)

    var word by DBWords.word
}