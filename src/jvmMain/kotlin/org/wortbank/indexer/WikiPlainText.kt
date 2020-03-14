package org.wortbank.indexer

import org.apache.commons.text.StringEscapeUtils
import java.util.regex.Pattern

class WikiPlainText(private val language: WikiLanguage) {
    enum class WikiLanguage {
        EN, DE
    }

    fun clean(page: String): String {
        var content = page
        content = removeFooter(content)
        content = removeRefs(content)
        content = removeInterWikiLinks(content)
        content = removeParentheticals(content)
        content = fixUnitConversion(content)
        content = ImageCaptionsRemover.remove(content)
        content = DoubleBracesRemover.remove(content)
        content = removeHtmlComments(content)
        content = removeEmphasis(content)
        content = removeHeadings(content)
        content = removeCategoryLinks(content)
        content = removeLinks(content)
        content = removeMath(content)
        content = removeGallery(content)
        content = removeNoToc(content)
        content = removeIndentation(content)
        content = TableRemover.remove(content)

        // For some reason, some HTML entities are doubly encoded.
        content = StringEscapeUtils.unescapeHtml4(StringEscapeUtils.unescapeHtml4(content))
        content = removeHtmlTags(content)

        // Finally, fold multiple newlines.
        content = compressMultipleNewlines(content)
        return content.trim { it <= ' ' }
    }

    private fun fixUnitConversion(s: String?): String {
        val t = UNIT_CONVERSION1.matcher(s).replaceAll("$1 $2")
        return UNIT_CONVERSION2.matcher(t).replaceAll("$1 $2")
    }

    private fun removeHtmlTags(s: String?): String {
        return HTML_TAGS.matcher(s).replaceAll("")
    }

    private fun removeGallery(s: String?): String {
        return GALLERY.matcher(s).replaceAll("")
    }

    private fun removeNoToc(s: String?): String {
        return NO_TOC.matcher(s).replaceAll("")
    }

    private fun removeIndentation(s: String?): String {
        return INDENTATION.matcher(s).replaceAll("\n")
    }

    private fun removeMath(s: String?): String {
        return MATH.matcher(s).replaceAll("")
    }

    private fun removeParentheticals(s: String): String {
        // Take care of things like: id 36
        // '''Albedo''' ({{IPAc-en|icon|æ|l|ˈ|b|iː|d|oʊ}}), or ''reflection coefficient'' ...
        //
        // Note that we shouldn't just leave to the double-curly remover, since that would leave
        // the dangling empty parens.
        var s = s
        s = IPA1.matcher(s).replaceAll("")

        // Straight-up IPA, with no parenthetical.
        s = IPA2.matcher(s).replaceAll("")
        return s
    }

    private fun compressMultipleNewlines(s: String?): String {
        return MULTIPLE_NEWLINES.matcher(s).replaceAll("\n\n")
    }

    private fun removeFooter(s: String): String {
        var s = s
        if (language == WikiLanguage.EN) {
            s = FOOTER_EN1.matcher(s).replaceAll("")
            s = FOOTER_EN2.matcher(s).replaceAll("")
            s = FOOTER_EN3.matcher(s).replaceAll("")
            s = FOOTER_EN4.matcher(s).replaceAll("")
        } else if (language == WikiLanguage.DE) {
            s = FOOTER_DE1.matcher(s).replaceAll("")
            s = FOOTER_DE2.matcher(s).replaceAll("")
            s = FOOTER_DE3.matcher(s).replaceAll("")
            s = FOOTER_DE4.matcher(s).replaceAll("")
            s = FOOTER_DE5.matcher(s).replaceAll("")
            s = FOOTER_DE6.matcher(s).replaceAll("")
        }
        return s
    }

    private fun removeCategoryLinks(s: String): String {
        if (language == WikiLanguage.EN) {
            return CATEGORY_LINKS_EN.matcher(s).replaceAll("")
        }
        return if (language == WikiLanguage.DE) {
            CATEGORY_LINKS_DE.matcher(s).replaceAll("")
        } else s
    }

    private fun removeLinks(s: String?): String {
        return LINKS2.matcher(LINKS1.matcher(s).replaceAll("$1")).replaceAll("")
    }

    private fun removeHeadings(s: String?): String {
        // Make sure there's an extra newline after headings.
        return HEADINGS.matcher(s).replaceAll("$1\n")
    }

    private fun removeEmphasis(s: String?): String {
        return EMPHASIS.matcher(s).replaceAll("")
    }

    private fun removeHtmlComments(s: String?): String {
        return HTML_COMMENT.matcher(s).replaceAll("")
    }

    private fun removeRefs(s: String): String {
        var s = s
        s = BR.matcher(s).replaceAll("") // See test case for why we do this.
        s = REF1.matcher(s).replaceAll("")
        s = REF2.matcher(s).replaceAll("")
        return s
    }

    private fun removeInterWikiLinks(s: String?): String {
        return INTER_WIKI_LINKS.matcher(s).replaceAll(" ")
    }

    private object ImageCaptionsRemover {
        private const val DEFAULT_NO_BRACKET = 0
        private const val STATE_1CLOSE_BRACKET = 1
        private const val STATE_1OPEN_BRACKET = 2
        internal fun remove(s: String): String {
            var s = s
            val labels = arrayOf(
                "[[File:", "[[Image:",
                "[[Datei" // We see this in de wikipedia.
            )
            for (label in labels) {
                s = removeLabel(s, label)
            }
            return s
        }

        // This method encodes a finite state machine to handle links in caption, which result in
        // nested [[ ... [[foo]] ... ]] constructs.
        internal fun removeLabel(s: String, label: String): String {
            var s = s
            var i = s.indexOf(label)
            while (i != -1) {
                var state = DEFAULT_NO_BRACKET
                var level = 1
                var cur = i + label.length
                while (cur < s.length) {
                    if (state == STATE_1OPEN_BRACKET && s[cur] == '[') {
                        level++
                        state = DEFAULT_NO_BRACKET
                    }
                    // If there's only one close, move back to default state.
                    if (state == STATE_1OPEN_BRACKET) {
                        state = DEFAULT_NO_BRACKET
                    }
                    if (s[cur] == '[') {
                        state = STATE_1OPEN_BRACKET
                    }
                    if (state == STATE_1CLOSE_BRACKET && s[cur] == ']') {
                        level--
                        if (level == 0) {
                            break
                        }
                        state = DEFAULT_NO_BRACKET
                    } else {
                        // If there's only one close, move back to default state.
                        if (state == STATE_1CLOSE_BRACKET) {
                            state = DEFAULT_NO_BRACKET
                        }
                        if (s[cur] == ']') {
                            state = STATE_1CLOSE_BRACKET
                        }
                    }
                    cur++
                }
                if (cur == s.length) {
                    return s.substring(0, i)
                }
                s = s.substring(0, i) + s.substring(cur + 1, s.length)
                i = s.indexOf(label, i)
            }
            return s
        }
    }

    private object DoubleBracesRemover {
        private const val DEFAULT_NO_BRACE = 0
        private const val STATE_1CLOSE_BRACE = 1
        private const val STATE_1OPEN_BRACE = 2

        // This method encodes a finite state machine to handle nested double braces (e.g., in infoboxes).
        internal fun remove(s: String): String {
            var s = s
            var i = s.indexOf("{{")
            while (i != -1) {
                var state = DEFAULT_NO_BRACE
                var level = 1
                var cur = i + 2
                while (cur < s.length) {
                    if (state == STATE_1OPEN_BRACE && s[cur] == '{') {
                        level++
                        state = DEFAULT_NO_BRACE
                    }
                    // If there's only one close, move back to default state.
                    if (state == STATE_1OPEN_BRACE) {
                        state = DEFAULT_NO_BRACE
                    }
                    if (s[cur] == '{') {
                        state = STATE_1OPEN_BRACE
                    }
                    if (state == STATE_1CLOSE_BRACE && s[cur] == '}') {
                        level--
                        if (level == 0) {
                            break
                        }
                        state = DEFAULT_NO_BRACE
                    } else {
                        // If there's only one close, move back to default state.
                        if (state == STATE_1CLOSE_BRACE) {
                            state = DEFAULT_NO_BRACE
                        }
                        if (s[cur] == '}') {
                            state = STATE_1CLOSE_BRACE
                        }
                    }
                    cur++
                }
                if (cur == s.length) {
                    return s.substring(0, i)
                }
                s = s.substring(0, i) + s.substring(cur + 1, s.length)
                i = s.indexOf("{{", i)
            }
            return s
        }
    }

    private object TableRemover {
        private const val DEFAULT = 0
        private const val STATE_PIPE = 1
        private const val STATE_1OPEN_BRACE = 2
        internal fun remove(s: String): String {
            var s = s
            var i = s.indexOf("{|")
            while (i != -1) {
                var state = DEFAULT
                var level = 1
                var cur = i + 2
                while (cur < s.length) {
                    if (state == STATE_1OPEN_BRACE && s[cur] == '|') {
                        level++
                        state = DEFAULT
                    }
                    // If there's only one close, move back to default state.
                    if (state == STATE_1OPEN_BRACE) {
                        state = DEFAULT
                    }
                    if (s[cur] == '{') {
                        state = STATE_1OPEN_BRACE
                    }
                    if (state == STATE_PIPE && s[cur] == '}') {
                        level--
                        if (level == 0) {
                            break
                        }
                        state = DEFAULT
                    } else {
                        // If there's a pipe but no close brace, move back to default state.
                        if (state == STATE_PIPE) {
                            state = DEFAULT
                        }
                        if (s[cur] == '|') {
                            state = STATE_PIPE
                        }
                    }
                    cur++
                }
                if (cur == s.length) {
                    return s.substring(0, i)
                }
                s = s.substring(0, i) + s.substring(cur + 1, s.length)
                i = s.indexOf("{|", i)
            }
            return s
        }
    }

    companion object {
        private const val XML_START_TAG_TITLE = "<title>"
        private const val XML_END_TAG_TITLE = "</title>"
        private const val XML_START_TAG_ID = "<id>"
        private const val XML_END_TAG_ID = "</id>"
        private const val XML_START_TAG_TEXT = "<text xml:space=\"preserve\""
        private const val XML_END_TAG_TEXT = "</text>"
        private val UNIT_CONVERSION1 =
            Pattern.compile("\\{\\{convert\\|(\\d+)\\|([^|]+)\\}\\}")
        private val UNIT_CONVERSION2 =
            Pattern.compile("\\{\\{convert\\|(\\d+)\\|([^|]+)\\|[^}]+\\}\\}")
        private val HTML_TAGS = Pattern.compile("<[^>]+>")
        private val GALLERY = Pattern.compile(
            "&lt;gallery&gt;.*?&lt;/gallery&gt;",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val NO_TOC = Pattern.compile("__NOTOC__")
        private val INDENTATION = Pattern.compile("[\\n\\r]:\\s*")
        private val MATH = Pattern.compile(
            "&lt;math&gt;.*?&lt;/math&gt;",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )

        // IPA parenthetical may be enclosed either with parentheses or brackets (de articles).
        private val IPA1 =
            Pattern.compile(" (\\(|\\[)\\{\\{IPA[^\\}]+\\}\\}(\\)|\\])")
        private val IPA2 = Pattern.compile(" \\{\\{IPA[^\\}]+\\}\\}")
        private val MULTIPLE_NEWLINES = Pattern.compile("[\\n\\r][\\n\\r]+")
        private val FOOTER_EN1 = Pattern.compile(
            "==\\s*See also\\s*==.*",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val FOOTER_EN2 = Pattern.compile(
            "==\\s*References\\s*==.*",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val FOOTER_EN3 = Pattern.compile(
            "==\\s*Further reading\\s*==.*",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val FOOTER_EN4 = Pattern.compile(
            "==\\s*External Links\\s*==.*",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val FOOTER_DE1 = Pattern.compile(
            "==\\s*Referenzen\\s*==.*",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val FOOTER_DE2 = Pattern.compile(
            "==\\s*Weblinks\\s*==.*",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val FOOTER_DE3 = Pattern.compile(
            "==\\s*Literatur\\s*==.*",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val FOOTER_DE4 = Pattern.compile(
            "==\\s*Einzelnachweise\\s*==.*",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val FOOTER_DE5 = Pattern.compile(
            "==\\s*Siehe auch\\s*==.*",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val FOOTER_DE6 = Pattern.compile(
            "==\\s*Quellen\\s*==.*",
            Pattern.CASE_INSENSITIVE or Pattern.DOTALL
        )
        private val CATEGORY_LINKS_EN =
            Pattern.compile("\\[\\[Category:([^\\]]+)\\]\\]")
        private val CATEGORY_LINKS_DE =
            Pattern.compile("\\[\\[Kategorie:([^\\]]+)\\]\\]")
        private val LINKS1 = Pattern.compile("\\[\\[[^\\]]+\\|([^\\]]+)\\]\\]")
        private val LINKS2 = Pattern.compile("(\\[\\[|\\]\\])")
        private val HEADINGS = Pattern.compile("=+\\s?(.*?)=+")
        private val EMPHASIS = Pattern.compile("('''|'')")
        private val HTML_COMMENT = Pattern.compile(
            "(<|&lt;|&#60;)!--.*?--(>|&gt;|&#62;)",
            Pattern.DOTALL
        )
        private val BR = Pattern.compile("&lt;br */&gt;")
        private val REF1 =
            Pattern.compile("&lt;ref[^/]+/&gt;", Pattern.DOTALL)
        private val REF2 =
            Pattern.compile("&lt;ref.*?&lt;/ref&gt;", Pattern.DOTALL)

        // Note that WiktionaryLinks have the form [[wikt:anarchism|anarchism]], which is easily confused with
        // inter-wikilinks. The distinguishing characteristic is the lack of pipe (|).
        private val INTER_WIKI_LINKS =
            Pattern.compile("\\[\\[[a-z\\-]+:[^|\\]]+\\]\\]")
    }
}

