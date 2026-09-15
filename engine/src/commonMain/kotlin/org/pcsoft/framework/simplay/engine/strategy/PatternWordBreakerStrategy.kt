/*
 * Copyright (c) KleinerHacker alias Pfeiffer C Soft 2026.
 * This work is licensed under the Apache License, Version 2.0.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, this software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations.
 */

package org.pcsoft.framework.simplay.engine.strategy

import org.pcsoft.framework.simplay.engine.FontMeasureCalculator
import org.pcsoft.framework.simplay.engine.hyphenation.LiangHyphenator
import org.pcsoft.framework.simplay.engine.hyphenation.LiangPatternParser
import org.pcsoft.framework.simplay.engine.hyphenation.LiangPatternSet
import org.pcsoft.framework.simplay.engine.hyphenation.patterns.EnglishPatterns
import org.pcsoft.framework.simplay.engine.hyphenation.patterns.GermanPatterns
import org.pcsoft.framework.simplay.engine.model.Font

/**
 * A [WordBreakerStrategy] that offers syllable-accurate hyphenation points, computed from bundled
 * Liang/TeX hyphenation patterns (see `.../hyphenation/patterns` and its `LICENSES.md` for the
 * per-language source and licence). Not every locale has bundled patterns; use [forLocale] and fall
 * back to [NoOpWordBreakerStrategy] when it returns `null`.
 */
class PatternWordBreakerStrategy internal constructor(
    private val patterns: LiangPatternSet,
) : WordBreakerStrategy {

    override fun breakOffsets(
        word: String,
        font: Font,
        maxWidth: Double,
        measurer: FontMeasureCalculator,
    ): List<Int> = LiangHyphenator.breakOffsets(word, patterns)

    companion object {

        private val cache = mutableMapOf<String, PatternWordBreakerStrategy?>()

        /**
         * Builds a [PatternWordBreakerStrategy] from the bundled hyphenation patterns for [locale]
         * (a bare language code such as `"de"` or `"en"`), parsing them once and caching the result.
         * Returns `null` if [locale] has no bundled pattern set.
         */
        fun forLocale(locale: String): PatternWordBreakerStrategy? = cache.getOrPut(locale) {
            val text = when (locale) {
                "de" -> GermanPatterns.TEXT
                "en" -> EnglishPatterns.TEXT
                else -> return@getOrPut null
            }
            PatternWordBreakerStrategy(LiangPatternParser.parse(text))
        }
    }
}
