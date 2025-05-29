package com.nemo.api

import com.lagradost.cloudstream3.app
import com.lagradost.nicehttp.NiceResponse
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("SpellCheckingInspection")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnimeOfflineDatabaseTest {

    companion object {
        @JvmStatic
        @AfterAll
        fun reset() {
            AnimeOfflineDatabase.reset()

        }
    }

    private val testAnimeList = listOf(
        Data.Anime(
            _title = "Naruto",
            type = Data.Anime.Type.TV,
            picture = "",
            _synonyms = listOf("Naruto Shippuden", "Naruto: Shippuuden"),
            ids = mapOf("anilist" to 20, "mal" to 20)
        ),
        Data.Anime(
            _title = "Boruto",
            type = Data.Anime.Type.TV,
            picture = "",
            _synonyms = listOf("Boruto Next Generations", "WTF HAPPENED TO NARUTO"),
            ids = mapOf("anilist" to 20, "mal" to 20)
        ),
        Data.Anime(
            _title = "Hagane no Renkinjutsushi",
            type = Data.Anime.Type.TV,
            picture = "",
            _synonyms = listOf("Fullmetal Alchemist", "FMA"),
            ids = mapOf("anilist" to 5114, "mal" to 5114)
        ),
        Data.Anime(
            _title = "Boku no Hero Academia",
            type = Data.Anime.Type.TV,
            picture = "",
            _synonyms = listOf("My Hero Academia", "Boku no Hero Academia: Heroes Rising"),
            ids = mapOf("anilist" to 101122, "mal" to 31964)
        )
    )

    @BeforeEach
    fun setup() {

        mockkObject(AnimeOfflineDatabase)

        val mockResponse = mockk<NiceResponse>()
        every { mockResponse.text } returns ""
        every { mockResponse.parsed<Data>() } returns Data(testAnimeList)

        mockkObject(app)
        coEvery { app.get(any()) } returns mockResponse

    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test string normalization`() {
        val testString = "  Hello`World  "
        val normalized = testString.normalize()
        assertEquals("Hello'World", normalized)
    }

    @Test
    fun `test exact match returns highest score`() = runTest {
        val results = AnimeOfflineDatabase.searchTitlesAsync("Naruto")
        assertTrue(results.isNotEmpty(), "Expected at least one result for exact match")
        assertEquals("Naruto", results[0].first.title)
        assertEquals(results[0].second, results.maxOf { it.second }, "Expected high score for exact match")
    }

    @Test
    fun `test synonym match returns high score`() = runTest {
        val results = AnimeOfflineDatabase.searchTitlesAsync("Naruto Shippuden")
        assertTrue(results.isNotEmpty(), "Expected at least one result for synonym match")
        assertEquals("Naruto", results[0].first.title)
        assertTrue(results[0].second >= 90, "Expected high score for synonym match")
    }

    @Test
    fun `test partial match returns good score`() = runTest {
        val results = AnimeOfflineDatabase.searchTitlesAsync("Fullmetal")
        assertTrue(results.isNotEmpty(), "Expected at least one result for partial match")
        assertEquals("Hagane no Renkinjutsushi", results[0].first.title)
        assertTrue(results[0].second >= 80, "Expected good score for partial match")
    }

    @Test
    fun `test search with empty query returns empty list`() = runTest {
        val results = AnimeOfflineDatabase.searchTitlesAsync("")
        assertTrue(results.isEmpty(), "Expected empty results for empty query")
    }

    @Test
    fun `test search with exact title returns match`() = runTest {
        val results = AnimeOfflineDatabase.searchTitlesAsync("Naruto")
        assertTrue(results.isNotEmpty(), "Expected at least one result for exact title match")
        assertEquals("Naruto", results[0].first.title)
    }

    @Test
    fun `test search with synonym returns match`() = runTest {
        val results = AnimeOfflineDatabase.searchTitlesAsync("Fullmetal Alchemist")
        assertTrue(results.isNotEmpty(), "Expected at least one result for synonym match")
        assertEquals("Hagane no Renkinjutsushi", results[0].first.title)
    }

    @Test
    fun `test search with limit parameter`() = runTest {
        val results = AnimeOfflineDatabase.searchTitlesAsync("nar", limit = 1)
        assertEquals(1, results.size, "Expected exactly one result due to limit")
    }

    @Test
    fun `test search with min score filter`() = runTest {
        val results = AnimeOfflineDatabase.searchTitlesAsync("naruto", minScore = 100)
        assertEquals(1, results.size, "Expected exactly one result due to minScore")
        assertTrue(results.all { it.second == 100 }, "All results should have score == 100")
    }

    @Test
    fun `test anime data class properties`() = runTest {
        val anime = testAnimeList[3] // Boku no Hero Academia
        assertTrue(anime.aliases.contains("Boku no Hero Academia"), "Title should be in aliases")
        assertTrue(anime.aliases.contains("My Hero Academia"), "Synonym should be in aliases")
        assertEquals(Data.Anime.Type.TV, anime.type)
    }
}
