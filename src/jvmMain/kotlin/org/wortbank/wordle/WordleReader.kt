package org.wortbank.wordle

import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.morph.MorphologicalFeatures
import de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence
import de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token
import de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent
import org.apache.uima.cas.FeatureStructure
import org.apache.uima.fit.component.JCasConsumer_ImplBase
import org.apache.uima.fit.descriptor.ConfigurationParameter
import org.apache.uima.fit.descriptor.TypeCapability
import org.apache.uima.fit.util.FSCollectionFactory
import org.apache.uima.fit.util.JCasUtil
import org.apache.uima.jcas.JCas
import org.apache.uima.jcas.cas.FSArray
import org.apache.uima.jcas.tcas.Annotation
import org.dkpro.core.io.tiger.internal.model.*
import org.wortbank.indexer.PARAM_UNIQUE_ID
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap


data class MorphFeatures(val pos: String?, val case: String?, val number: String?, val gender: String?)

object GlobalMResultMap {
    val map = ConcurrentHashMap<String, MorphFeatures?>()
}

@TypeCapability(
    inputs = [
        "de.tudarmstadt.ukp.dkpro.core.api.metadata.type.DocumentMetaData",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Sentence",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Token",
        "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS",
        "de.tudarmstadt.ukp.dkpro.core.api.segmentation.type.Lemma",
        "de.tudarmstadt.ukp.dkpro.core.api.syntax.type.constituent.Constituent"]
)
class CasMorphMemoryReader : JCasConsumer_ImplBase() {

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

        var value: String? = null
        var pos: String? = null

        for (s in JCasUtil.select(aJCas, MorphologicalFeatures::class.java)) {
            value = s.value
        }
        for (s in JCasUtil.select(aJCas, POS::class.java)) {
            pos = s.coarseValue
        }
        GlobalMResultMap.map[id] = value.parse()?.copy(pos = pos)
    }

    private fun String?.parse(): MorphFeatures? {
        return this?.let { str ->
            if (str == "_") return MorphFeatures(null, null, null, null)
            val dict = str.split("|").associate {
                val (left, right) = it.split("=")
                left to right
            }
            MorphFeatures(null, dict["case"], dict["number"], dict["gender"])
        }
    }
}
