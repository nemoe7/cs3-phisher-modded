package com.nemo.api

import androidx.annotation.VisibleForTesting
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

import me.xdrop.fuzzywuzzy.FuzzySearch

fun String.normalize() = this
    .replace('`', '\'')
    .trim()

object AnimeOfflineDatabase {

    private const val URL =
        "https://raw.githubusercontent.com/nemoe7/cs3-phisher-modded/refs/heads/builds/raw/aod/aod.json"

    private var entries: List<Data.Anime>? = null

    private fun getBestMatchScore(
        anime: Data.Anime,
        search: String
    ): Int {

        anime.aliases.firstOrNull {
            it.equals(search, ignoreCase = true)
        }?.let { return 100 }

        var maxScore = 0
        val searchLower = search.normalize().lowercase()
        val searchTerms = searchLower.split(" ").filter { it.length > 2 }

        for (alias in anime.aliases) {
            if (alias.length * 2 < search.length) continue

            val aliasLower = alias.lowercase()
            if (searchTerms.all { term -> aliasLower.contains(term) }) {
                val orderedScore = if (aliasLower.contains(searchLower.replace(" ", ""))) 90 else 85
                if (orderedScore > maxScore) maxScore = orderedScore
                continue
            }

            val ratio = FuzzySearch.ratio(aliasLower, searchLower)
            if (ratio > 85) {
                val weightedScore = FuzzySearch.weightedRatio(aliasLower, searchLower)
                if (weightedScore > maxScore) {
                    maxScore = weightedScore
                    if (maxScore >= 95) break
                }
            } else if (ratio > maxScore) {
                maxScore = ratio
            }
        }
        return maxScore
    }

    suspend fun init() {

        if (entries != null) return

        Log.d("nemo", "Initializing AnimeOfflineDatabase...")
        val initTime = kotlin.system.measureTimeMillis {
            // TODO: Implement cache
            entries = app.get(URL).parsed<Data>().data
        }
        Log.d("nemo", "Initialized AnimeOfflineDatabase in ${initTime}ms")

    }

    suspend fun searchTitlesAsync(
        search: String,
        limit: Int = 10,
        minScore: Int = 70
    ): List<Pair<Data.Anime, Int>> = coroutineScope {
        if (entries == null) init()
        val normSearch = search.normalize().lowercase()
        if (normSearch.length < 2) return@coroutineScope emptyList()

        val chunkSize =
            ((entries?.size ?: 0) / Runtime.getRuntime().availableProcessors())
                .coerceAtLeast(1)
        val chunks = entries?.chunked(chunkSize) ?: emptyList()

        chunks.map { chunk ->
            async(Dispatchers.Default) {
                processSearchChunk(chunk, normSearch, minScore)
            }
        }
            .awaitAll()
            .flatten()
            .sortedByDescending { it.second }
            .take(limit)
    }

    private fun processSearchChunk(
        chunk: List<Data.Anime>,
        normSearch: String,
        minScore: Int
    ): List<Pair<Data.Anime, Int>> {
        val results = ArrayList<Pair<Data.Anime, Int>>(chunk.size / 2)
        for (entry in chunk) {
            val score = getBestMatchScore(entry, normSearch)
            if (score >= minScore) {
                results.add(entry to score)
            }
        }
        return results
    }

    @VisibleForTesting
    internal fun reset() {
        entries = null
    }
}

data class Data(
    @JsonProperty("data") val `data`: List<Anime>
) {
    data class Anime(
        @JsonProperty("title") private val _title: String,
        @JsonProperty("type") val type: Type,
        @JsonProperty("picture") val picture: String,
        @JsonProperty("synonyms") private val _synonyms: List<String>,
        @JsonProperty("ids") val ids: Map<String, Int?> = emptyMap()
    ) {
        val title: String = _title.normalize()
        val synonyms: List<String> = _synonyms.map { it.normalize() }
        val aliases: List<String> by lazy { listOf(title) + synonyms }

        @Suppress("unused")
        enum class Type(val value: Any) {
            TV("TV"),
            MOVIE("MOVIE"),
            OVA("OVA"),
            ONA("ONA"),
            SPECIAL("SPECIAL"),
            UNKNOWN("UNKNOWN")
        }
    }
}
