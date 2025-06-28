package com.phisher98

import android.content.SharedPreferences
import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.APIHolder.unixTime
import com.lagradost.cloudstream3.LoadResponse.Companion.addAniListId
import com.lagradost.cloudstream3.LoadResponse.Companion.addMalId
import com.lagradost.cloudstream3.mvvm.suspendSafeApiCall
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jsoup.Jsoup
import org.jsoup.nodes.Element


class AnimePahe(val sharedPref: SharedPreferences? = null) : MainAPI() {

    companion object {
        const val MAIN_URL = "https://animepahe.ru"
        val headers = mapOf("Cookie" to "__ddg2_=1234567890")
        private const val PROXY="https://animepaheproxy.phisheranimepahe.workers.dev/?url="
        //var cookies: Map<String, String> = mapOf()
        private fun getType(t: String): TvType {
            return if (t.contains("OVA") || t.contains("Special")) TvType.OVA
            else if (t.contains("Movie")) TvType.AnimeMovie
            else TvType.Anime
        }
    }

    override var mainUrl = MAIN_URL
    override var name = "AnimePahe"
    override val hasQuickSearch = false
    override val hasMainPage = true

    override val supportedTypes = setOf(
        TvType.AnimeMovie, TvType.Anime, TvType.OVA
    )

    override val mainPage =
        listOf(MainPageData("Latest Releases", "$PROXY$MAIN_URL/api?m=airing&page=", true))

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {

        data class LatestReleasesEntry(
            // @JsonProperty("id") val id: Int,
            // @JsonProperty("anime_id") val animeId: Int,
            @JsonProperty("anime_title") val title: String,
            // @JsonProperty("anime_slug") val animeSlug: String,
            @JsonProperty("episode") val episode: Int?,
            @JsonProperty("snapshot") val snapshot: String?,
            @JsonProperty("created_at") val createdAt: String?,
            @JsonProperty("anime_session") val session: String,
        )

        data class LatestReleasesResponse(
            @JsonProperty("total") val total: Int,
            @JsonProperty("data") val data: List<LatestReleasesEntry>
        )

        val res = app.get(request.data + page, headers = headers).text
        val episodes = parseJson<LatestReleasesResponse>(res).data.amap { entry ->

            var title: String = entry.title.ifBlank { "404: Not Found" }

            val preferJpTitle = sharedPref?.getBoolean("jpTitle", false) ?: false
            if (preferJpTitle || title.isBlank()) {
                val html = app.get("$PROXY$MAIN_URL/anime/${entry.session}", headers = headers).text
                val doc = Jsoup.parse(html)
                title = doc.selectFirst("h2.japanese")?.text() ?: title
            }

            newAnimeSearchResponse(
                title,
                Session(entry.session, unixTime, entry.title).toJson(),
                fix = false
            ) {
                this.posterUrl = entry.snapshot
                addDubStatus(DubStatus.Subbed, entry.episode)
            }
        }

        return newHomePageResponse(
            HomePageList(request.name, episodes, isHorizontalImages = true), hasNext = true
        )
    }

    data class SearchEntry(
        @JsonProperty("id") val id: Int?,
        @JsonProperty("slug") val slug: String?,
        @JsonProperty("title") val title: String,
        @JsonProperty("type") val type: String?,
        @JsonProperty("episodes") val episodes: Int?,
        @JsonProperty("status") val status: String?,
        @JsonProperty("season") val season: String?,
        @JsonProperty("year") val year: Int?,
        @JsonProperty("score") val score: Double?,
        @JsonProperty("poster") val poster: String?,
        @JsonProperty("session") val session: String,
        @JsonProperty("relevance") val relevance: String?
    )

    data class SearchResponse(
        @JsonProperty("total") val total: Int, @JsonProperty("data") val data: List<SearchEntry>
    )

    override suspend fun search(query: String): List<com.lagradost.cloudstream3.SearchResponse> {
        val url = "$PROXY$MAIN_URL/api?m=search&l=8&q=$query"
        val headers = mapOf("referer" to "$MAIN_URL/","Cookie" to "__ddg2_=1234567890")

        val res = app.get(url, headers = headers).text
        val data = parseJson<SearchResponse>(res).data

        return data.amap { entry ->

            var title: String = entry.title.ifBlank { "404: Not Found" }

            val preferJpTitle = sharedPref?.getBoolean("jpTitle", false) ?: false
            if (preferJpTitle || title.isBlank()) {
                val html = app.get("$PROXY$MAIN_URL/anime/${entry.session}", headers = Companion.headers).text
                val doc = Jsoup.parse(html)
                title = doc.selectFirst("h2.japanese")?.text() ?: title
            }

            newAnimeSearchResponse(
                title,
                Session(entry.session, unixTime, entry.title).toJson(),
                fix = false
            ) {
                this.posterUrl = entry.poster
                addDubStatus(DubStatus.Subbed, entry.episodes)
            }
        }
    }

    private data class EpisodeData(
        @JsonProperty("id") val id: Int,
        @JsonProperty("anime_id") val animeId: Int,
        @JsonProperty("episode") val episode: Int,
        @JsonProperty("title") val title: String,
        @JsonProperty("snapshot") val snapshot: String,
        @JsonProperty("session") val session: String,
        @JsonProperty("filler") val filler: Int,
        @JsonProperty("created_at") val createdAt: String
    )

    private data class PageData(
        @JsonProperty("total") val total: Int,
        @JsonProperty("per_page") val perPage: Int,
        @JsonProperty("current_page") val currentPage: Int,
        @JsonProperty("last_page") val lastPage: Int,
        @JsonProperty("next_page_url") val nextPageUrl: String?,
        @JsonProperty("prev_page_url") val prevPageUrl: String?,
        @JsonProperty("from") val from: Int,
        @JsonProperty("to") val to: Int,
        @JsonProperty("data") val episodeList: List<EpisodeData>
    )

    data class EpisodeLinkData(
        @JsonProperty("is_play_page") val isPlayPage: Boolean,
        @JsonProperty("episode_num") val episodeNum: Int,
        @JsonProperty("page") val page: Int,
        @JsonProperty("session") val session: String,
        @JsonProperty("episode_session") val episodeSession: String,
        @JsonProperty("dubType") var dubType: String,
    ) {
        private val headers = mapOf("Cookie" to "__ddg2_=1234567890")
        suspend fun getUrl(): String? {
            return if (isPlayPage) {
                "$PROXY$MAIN_URL/play/${session}/${episodeSession}"
            } else {
                val url =
                    "$PROXY$MAIN_URL/api?m=release&id=${session}&sort=episode_asc&page=${page + 1}"
                val jsonResponse =
                    app.get(url, headers = headers).parsedSafe<PageData>() ?: return null
                val episode =
                    jsonResponse.episodeList.firstOrNull { it.episode == episodeNum }?.session
                    ?: return null
                "$PROXY$MAIN_URL/play/${session}/${episode}"
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun buildEpisodeList(session: String): ArrayList<Episode> {

        val episodes = ArrayList<Episode>()
        val semaphore = Semaphore(5) // Limit to 5 concurrent requests

        try {
            val url = "$PROXY$MAIN_URL/api?m=release&id=$session&sort=episode_asc&page=1"
            val res = app.get(url, headers = headers).text
            val pageData = parseJson<PageData>(res)

            val lastPage = pageData.lastPage
            val perPage = pageData.perPage
            val total = pageData.total
            var currentEpisode = 1

            fun getEpisodeTitle(episodeData: EpisodeData): String {
                return episodeData.title.ifEmpty { "Episode ${episodeData.episode}" }
            }

            // If only one page, process all episodes in that page
            if (lastPage == 1 && perPage > total) {
                episodes += pageData.episodeList.map { episodeData ->
                    newEpisode(
                        EpisodeLinkData(
                            isPlayPage = true,
                            episodeNum = episodeData.episode,
                            page = 0,
                            session = session,
                            episodeSession = episodeData.session,
                            dubType = "sub"
                        ).toJson()
                    ) {
                        addDate(episodeData.createdAt)
                        this.name = getEpisodeTitle(episodeData)
                        this.posterUrl = episodeData.snapshot
                    }
                }
            } else {
                // Fetch multiple pages concurrently with limited threads
                val deferredResults = (1..lastPage).map { page ->
                    GlobalScope.async {
                        semaphore.withPermit {
                            try {
                                val pageUrl =
                                    "$PROXY$MAIN_URL/api?m=release&id=$session&sort=episode_asc&page=$page"
                                val pageRes = app.get(pageUrl, headers = headers).text
                                currentEpisode++

                                // Return PageData for further processing
                                parseJson<PageData>(pageRes)

                            } catch (e: Exception) {
                                Log.e(
                                    "AnimePahe",
                                    "buildEpisodeList: Error on page $page: ${e.message}"
                                )
                                emptyList<Episode>()
                            }
                        }
                    }
                }

                // Wait for all pages to load and filter results
                val resolvedPages = deferredResults.awaitAll()
                resolvedPages.forEach { resolvedPage ->
                    resolvedPage as PageData
                    episodes += resolvedPage.episodeList.map { episodeData ->
                        newEpisode(
                            EpisodeLinkData(
                                isPlayPage = true,
                                episodeNum = episodeData.episode,
                                page = resolvedPage.currentPage,
                                session = session,
                                episodeSession = episodeData.session,
                                dubType = "sub"
                            ).toJson()
                        ) {
                            addDate(episodeData.createdAt)
                            this.name = getEpisodeTitle(episodeData)
                            this.posterUrl = episodeData.snapshot
                        }
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("AnimePahe", "buildEpisodeList: Error generating episodes: ${e.message}")
        }

        return episodes
    }

    /**
     * Required to make bookmarks work with a session system
     **/
    data class Session(
        val session: String, val sessionDate: Long, val name: String
    )

    override suspend fun load(url: String): LoadResponse? {

        // Replace with safeAsync once new stable is released
        return suspendSafeApiCall {
            val session = parseJson<Session>(url).let { data ->

                // idk if i understood the outdated comment ~nemo
                val isOutdated = data.sessionDate + 60 * 10 < unixTime
                if (isOutdated) {
                    val newUrl = search(data.name).firstOrNull()?.url ?: return@let null
                    parseJson<Session>(newUrl).session
                } else {
                    data.session
                }
            } ?: return@suspendSafeApiCall null
            val html = app.get("$PROXY$mainUrl/anime/$session",headers=headers).text
            val doc = Jsoup.parse(html)
            val jpTitle = doc.selectFirst("h2.japanese")?.text()
            val mainTitle = doc.selectFirst("span.sr-only.unselectable")?.text()
            val poster = doc.selectFirst(".anime-poster a")?.attr("href")
            val tvType = doc.selectFirst("""a[href*="/anime/type/"]""")?.text()


            val preferJpTitle = sharedPref?.getBoolean("jpTitle", false) ?: false
            var title = mainTitle.takeUnless { it.isNullOrBlank() } ?: "404: Not Found"
            if (preferJpTitle) title = jpTitle ?: title

            val subEpisodes = buildEpisodeList(session)

            // Copy subEpisodes but change dubType to dub
            val dubEpisodes = subEpisodes.map { episode ->
                val dubData = parseJson<EpisodeLinkData>(episode.data).apply { dubType = "dub" }
                episode.copy(data = dubData.toJson())
            }

            val year = Regex("""<strong>Aired:</strong>[^,]*, (\d+)""")
                .find(html)?.destructured?.component1()?.toIntOrNull()

            val status = when {
                doc.selectFirst("a[href='/anime/airing']") != null -> ShowStatus.Ongoing
                doc.selectFirst("a[href='/anime/completed']") != null -> ShowStatus.Completed
                else -> null
            }

            val synopsis = doc.selectFirst(".anime-synopsis")?.text()

            var aniListId: Int? = null
            var malId: Int? = null

            doc.select(".external-links > a").forEach { aTag ->
                val href = aTag.attr("href")
                val id = href.substringAfterLast("/").toIntOrNull() ?: return@forEach

                @Suppress("SpellCheckingInspection")
                when {
                    "anilist.co" in href -> aniListId = id
                    "myanimelist.net" in href -> malId = id
                }
            }

            val genres = doc.select(".anime-genre > ul a")
                .takeIf { it.isNotEmpty() }
                ?.map { it.text() }
                ?.let { ArrayList(it) }

            newAnimeLoadResponse(
                title, url, getType(tvType.toString())
            ) {
                engName = mainTitle
                japName = jpTitle
                this.posterUrl = poster
                this.year = year
                addEpisodes(DubStatus.Subbed, subEpisodes)
                addEpisodes(DubStatus.Dubbed, dubEpisodes)
                this.showStatus = status
                plot = synopsis
                this.tags = genres

                addMalId(malId)
                addAniListId(aniListId)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {

        val episodeLinkData = parseJson<EpisodeLinkData>(data)
        val episodeUrl = episodeLinkData.getUrl() ?: return false
        val doc = app.get(episodeUrl, headers = headers).document

        fun extractInfo(element: Element): Triple<String, String, Int> {
            val dubText = element.select("span").text().lowercase()
            val type = if (dubText.contains("eng", ignoreCase = true)) "DUB" else "SUB"

            val qualityRegex = Regex("""(.+?)\s+·\s+(\d{3,4}p)""")
            val text = element.text()
            val match = qualityRegex.find(text)

            val source = match?.groupValues?.getOrNull(1)?.trim() ?: "Unknown"
            val quality = match?.groupValues?.getOrNull(2)?.removeSuffix("p")?.toIntOrNull()
                ?: Qualities.Unknown.value

            return Triple(type, source, quality)
        }

        val sourceAEnabled = sharedPref?.getBoolean("sourceAEnabled", true) ?: true
        val sourceBEnabled = sharedPref?.getBoolean("sourceBEnabled", true) ?: true

        if (sourceAEnabled) {
            doc.select("#resolutionMenu button").amap {

                val (type, source, quality) = extractInfo(it)

                if (episodeLinkData.dubType.equals(type, ignoreCase = true)) {
                    val href = it.attr("data-src")
                    @Suppress("SpellCheckingInspection") if ("kwik.si" in href) {
                        loadCustomExtractor(
                            "AnimePahe A [$source]", url = href, "", subtitleCallback, callback, quality
                        )
                    }
                }
            }
        }

        if (sourceBEnabled) {
            doc.select("div#pickDownload > a").amap {
                val (type, source, quality) = extractInfo(it)
                if (episodeLinkData.dubType.equals(type, ignoreCase = true)) {
                    val href = it.attr("href")
                    loadCustomExtractor(
                        "AnimePahe B [$source]", href, "", subtitleCallback, callback, quality
                    )
                }
            }
        }

        return true
    }
}
