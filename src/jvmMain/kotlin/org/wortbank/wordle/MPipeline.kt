package org.wortbank.wordle

import org.apache.uima.fit.factory.AnalysisEngineFactory
import org.apache.uima.fit.factory.JCasFactory
import org.apache.uima.fit.pipeline.SimplePipeline
import org.dkpro.core.corenlp.CoreNlpPosTagger
import org.dkpro.core.icu.IcuSegmenter
import org.dkpro.core.ixa.IxaLemmatizer
import org.dkpro.core.matetools.MateMorphTagger
import org.dkpro.core.opennlp.OpenNlpPosTagger
import org.wortbank.indexer.NLPPipeline
import org.wortbank.indexer.PARAM_UNIQUE_ID


object MPipeline {
    private val language = "de"

    private val segmenter = AnalysisEngineFactory.createEngine(
        IcuSegmenter::class.java,
        IcuSegmenter.PARAM_LANGUAGE,
        language
    )

    private val pos = AnalysisEngineFactory.createEngine(
        CoreNlpPosTagger::class.java,
        CoreNlpPosTagger.PARAM_LANGUAGE,
        language
    )

    private val lemma = AnalysisEngineFactory.createEngine(
        IxaLemmatizer::class.java,
        IxaLemmatizer.PARAM_LANGUAGE,
        language
    )


    private val morphAnnotator = AnalysisEngineFactory.createEngine(
        MateMorphTagger::class.java,
        MateMorphTagger.PARAM_LANGUAGE,
        language
    )

//    private val morphAnnotator2 = AnalysisEngineFactory.createEngine(
//        RfTagger::class.java,
//        RfTagger.PARAM_LANGUAGE,
//        language
//    )

    private fun writer(id: String) = AnalysisEngineFactory.createEngine(
        CasMorphMemoryReader::class.java,
        PARAM_UNIQUE_ID, id
    )

    private fun reader(content: String) = JCasFactory.createText(content, language)

    fun runPipeline(id: String, content: String) = SimplePipeline.runPipeline(reader(content), segmenter, pos, lemma, morphAnnotator, writer(id))
}