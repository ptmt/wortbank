package org.wortbank.wordle

import kotlinx.coroutines.delay
import org.wortbank.storage.Storage


class WordleFilter(val storage: Storage) {
    private val batch = 100
    suspend fun filterWords(): Pair<Int, Int> {
        var toProcess = storage.totalUnprocessedWordle()
        while (toProcess > 0) {
            println(">> to process $toProcess")

            val words = storage.wordleBatch(batch)
            val exceptionOnWords = mutableListOf<String>()
            val filtered = words.withIndex().mapNotNull { (index, value) ->
                try {
                    MPipeline.runPipeline(index.toString(), value.word.replaceFirstChar { it.uppercase() })
                    (value.word to GlobalMResultMap.map[index.toString()]).also {
                        GlobalMResultMap.map.remove(index.toString())
                    }
                } catch (t: Throwable) {
                    exceptionOnWords.add(value.word)
                    println(">> exception on word ${value.word} $t")
                    null
                }
            }.filter { it.second?.pos != null && it.second!!.pos in listOf("NOUN", "ADV", "ADP") }
                .toMap()

            val freqs = storage.wordFreqs(filtered.keys)

            storage.tx {
                words.filter { it.word in filtered }.forEach {
                    it.validated = true
                    it.meta = filtered[it.word]?.gender ?: filtered[it.word]?.pos ?: ""
                    it.freq = freqs[it.word] ?: 0
                }
                words.filter { it.word !in filtered }.forEach {
                    it.delete()
                }
            }

            println(">> filtered ${filtered.entries.joinToString("\n") { "${it.key}(${it.value})" }}")

            toProcess = storage.totalUnprocessedWordle()
            delay(1000)
        }


        return storage.totalProcessedWordle() to storage.totalUnprocessedWordle()
    }
}
