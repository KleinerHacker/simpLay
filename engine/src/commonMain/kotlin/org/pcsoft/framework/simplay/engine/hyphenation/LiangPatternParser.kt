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

package org.pcsoft.framework.simplay.engine.hyphenation

/**
 * A single node of the [LiangPatternSet] trie. Each edge is one letter of a pattern; a node that
 * terminates at least one pattern carries that pattern's [values] array.
 */
internal class PatternTrieNode {
    val children: MutableMap<Char, PatternTrieNode> = HashMap()
    var values: IntArray? = null
}

/**
 * A parsed set of Liang/TeX hyphenation patterns, organised as a trie over the letters of every
 * pattern for fast prefix lookup by [LiangHyphenator].
 */
class LiangPatternSet internal constructor(internal val root: PatternTrieNode)

/**
 * Parses hyphenation pattern files in the plain hyph-utf8/CTAN `.pat.txt` format: one Liang pattern
 * per line, digits `0`-`9` interspersed between letters encode the hyphenation value of the gap in
 * front of the following letter (or, for a leading digit, the gap before the first letter). An odd
 * value marks a permissible break, an even value forbids one; the highest value found for a gap wins
 * once patterns are combined by [LiangHyphenator].
 */
object LiangPatternParser {

    /**
     * Parses [text] as a whole pattern file. Blank lines and lines starting with `%` (TeX comment
     * convention) are ignored; every other line may hold one or more whitespace-separated patterns.
     */
    fun parse(text: String): LiangPatternSet {
        val root = PatternTrieNode()
        for (line in text.lineSequence()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("%")) continue
            for (pattern in trimmed.splitToSequence(' ', '\t').filter { it.isNotEmpty() }) {
                insert(root, pattern)
            }
        }
        return LiangPatternSet(root)
    }

    private fun insert(root: PatternTrieNode, pattern: String) {
        val letters = StringBuilder(pattern.length)
        val values = IntArray(pattern.length + 1)
        var pos = 0
        for (c in pattern) {
            if (c in '0'..'9') {
                values[pos] = c - '0'
            } else {
                letters.append(c)
                pos++
            }
        }
        if (letters.isEmpty()) return

        var node = root
        for (c in letters) {
            node = node.children.getOrPut(c) { PatternTrieNode() }
        }
        node.values = values.copyOf(letters.length + 1)
    }
}
