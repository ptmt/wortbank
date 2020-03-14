package org.wortbank.indexer

import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent
import org.apache.uima.cas.FeatureStructure
import org.apache.uima.fit.component.JCasConsumer_ImplBase
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.fit.descriptor.TypeCapability
import org.apache.uima.fit.factory.AnalysisEngineFactory
import org.apache.uima.fit.factory.JCasFactory
import org.apache.uima.fit.pipeline.SimplePipeline
import org.apache.uima.fit.util.FSCollectionFactory
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import org.apache.uima.jcas.cas.FSArray
import org.apache.uima.jcas.tcas.Annotation
import org.dkpro.core.icu.IcuSegmenter
import org.dkpro.core.io.tiger.internal.model.*
import org.dkpro.core.ixa.IxaLemmatizer
import org.dkpro.core.opennlp.OpenNlpPosTagger
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.collections.set


object WikiCleaner {
    val deCleaner = WikiPlainText(language = WikiPlainText.WikiLanguage.DE)
}

object NLPPipeline {
    private val language = "de"
    private val segmenter = AnalysisEngineFactory.createEngine(
        IcuSegmenter::class.java,
        IcuSegmenter.PARAM_LANGUAGE,
        language
    )

    private val lemma = AnalysisEngineFactory.createEngine(
        IxaLemmatizer::class.java,
        IxaLemmatizer.PARAM_LANGUAGE,
        language
    )

    private val pos = AnalysisEngineFactory.createEngine(
        OpenNlpPosTagger::class.java,
        OpenNlpPosTagger.PARAM_LANGUAGE,
        language
    )

    private fun writer(id: String) = AnalysisEngineFactory.createEngine(
        CasMemoryReader::class.java,
        PARAM_UNIQUE_ID, id
    )

    private fun reader(content: String) = JCasFactory.createText(content, language)

    fun runPipeline(id: String, content: String) = SimplePipeline.runPipeline(reader(content), segmenter, pos, lemma, writer(id))
}


fun parseWikiText(id: String, wiki: String): Lemmas {

    val content = WikiCleaner.deCleaner.clean(wiki)

    NLPPipeline.runPipeline(id, content)
    val tokens = GlobalResultMap.map[id]?.flatMap { it.graph.terminals.map { it.wordOrLemma }.filterNot { it in filterOutSymbols} } ?: emptyList()
    GlobalResultMap.map.remove(id)
    return Lemmas(tokens)
}

val TigerTerminal.wordOrLemma get(): String {
    return if (lemma in listOf("-", "_", "*")) word else lemma
}

val filterOutSymbols = listOf("(", ")", "*", ",", "-", ".", ":", "/", "\\", "–", " ")

const val PARAM_UNIQUE_ID = "PARAM_UNIQUE_ID"

object GlobalResultMap {
    val map = ConcurrentHashMap<String, List<TigerSentence>>()
}

@TypeCapability(inputs = ["de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token", "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS", "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma", "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent"])
class CasMemoryReader : JCasConsumer_ImplBase() {

    @ConfigurationParameter(
        name = PARAM_UNIQUE_ID,
        mandatory = true
    )
    private val uniqueId: String? = null

    override fun process(aJCas: JCas?) {
        val id = uniqueId ?: error("uniqueId is not set")
        aJCas?.let { processCas(uniqueId, aJCas) }
    }

    private fun processCas(id: String, aJCas: JCas) {

        val sentences = mutableListOf<TigerSentence>()
        for ((i, s) in JCasUtil.select(aJCas, Sentence::class.java).withIndex()) {
            s.convertSentence(i)?.let {
                sentences.add(it)
            }
        }
       // println("$id converted ${sentences.count()} terminals ${sentences.sumBy { it.graph.terminals.count() }}")
        GlobalResultMap.map[id] = sentences
    }

    private fun Sentence.convertSentence(
        aSentNum: Int
    ): TigerSentence? {
        // Reset values
        var nodeNum = 1
        val nodes: MutableMap<FeatureStructure, TigerNode> =
            HashMap()
        val sentence = TigerSentence()
        sentence.id = if (id != null) id else "s_$aSentNum"
        sentence.graph = TigerGraph()
        sentence.graph.terminals = ArrayList()

        // Convert the tokens
        for (token in JCasUtil.selectCovered(
            Token::class.java, this
        )) {
            val terminal = TigerTerminal()
            terminal.id = if (token.id != null) token.id else sentence.id + "_" + nodeNum
            if (token.pos != null) {
                terminal.pos = token.pos.posValue
            }
            if (token.lemma != null) {
                terminal.lemma = token.lemma.value
            }
            terminal.word = token.coveredText
            sentence.graph.terminals.add(terminal)
            nodes[token] = terminal
            nodeNum++
        }

        // Convert the parse tree (pass 1: nodes)
        sentence.graph.nonTerminals = ArrayList()
        val constituents =
            JCasUtil.selectCovered(
                Constituent::class.java, this
            )
        for (constituent in constituents) {
            val node = TigerNonTerminal()
            node.id = sentence.id + "_" + nodeNum
            node.cat = constituent.constituentType
            node.edges = ArrayList()
            sentence.graph.nonTerminals.add(node)
            nodes[constituent] = node
            nodeNum++
            if (constituent.parent == null) {
                sentence.graph.root = node.id
            }
        }

        // Convert the parse tree (pass 2: edges)
        for (constituent in constituents) {
            val node = nodes[constituent]
            for (c in FSCollectionFactory.create(
                constituent.children as FSArray<Annotation>
            )) {
                if (c is Constituent) {
                    val synFun =
                        c.syntacticFunction
                    val edge = TigerEdge()
                    edge.label = synFun ?: "--"
                    edge.idref = nodes[c]!!.id
                    node!!.edges.add(edge)
                }
                if (c is Token) {
                    val edge = TigerEdge()
                    edge.label = "--"
                    edge.idref = nodes[c]!!.id
                    node!!.edges.add(edge)
                }
            }
        }
        return sentence
    }
}