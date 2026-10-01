package eu.kanade.tachiyomi.extension.en.pornhwadb

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.text.InputType
import androidx.preference.EditTextPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import injekt.Injekt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Pornhwa DB (https://pornhwadb.com) as a Mihon source.
 *
 * This is a *catalogue* API, not a reader. Its `Chapter` resource is a scene record - a chapter
 * range plus tags and characters - and no endpoint exposes page image URLs, so there is nothing to
 * read. Chapters are therefore rendered into the description and the source exposes none, because a
 * chapter entry here could only ever dead-end in the reader.
 *
 * Authentication is an `X-API-Key` header and nothing else; the API has no user accounts. The key is
 * entered in the source's settings and is never written to this APK.
 */
class PornhwaDb : HttpSource(), ConfigurableSource {

    override val name = "Pornhwa DB"
    override val baseUrl = "https://pornhwadb.com"
    override val lang = "en"
    override val supportsLatest = true

    private val json = Json { ignoreUnknownKeys = true }

    private val prefs: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    }

    private val apiKey: String
        get() = prefs.getString(PREF_API_KEY, "").orEmpty().trim()

    override fun headersBuilder() = super.headersBuilder().apply {
        add("X-API-Key", apiKey)
        add("Accept", "application/json")
    }

    // ---- catalogue -----------------------------------------------------------------------------

    override fun popularMangaRequest(page: Int): Request =
        GET("$baseUrl$API_PREFIX/pornhwa", headers, pageParams(page, "average_rating", "desc"))

    override fun popularMangaParse(response: Response): MangasPage {
        val body = response.parse<ListResponse>()
        return MangasPage(body.data.map(::toSManga), body.pagination.hasMore)
    }

    override fun latestUpdatesRequest(page: Int): Request =
        GET("$baseUrl$API_PREFIX/pornhwa", headers, pageParams(page, "updated_at", "desc"))

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val params = pageParams(page, "title", "asc") {
            addQueryParameter("q", query)
            addQueryParameter("type", "pornhwa")
        }
        return GET("$baseUrl$API_PREFIX/search", headers, params)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val body = response.parse<SearchResponse>()
        val page = body.pagination["pornhwa"]
        return MangasPage(body.data.pornhwa.map(::toSManga), page?.hasMore ?: false)
    }

    override fun mangaDetailsRequest(manga: SManga): Request =
        GET(baseUrl + manga.url, headers)

    override fun mangaDetailsParse(response: Response): SManga {
        val dto = response.parse<DetailResponse>().data
        return SManga.create().apply {
            url = "/pornhwa/${dto.slug}"
            title = dto.title
            author = dto.authors.joinToString(", ")
            artist = dto.artists.joinToString(", ")
            genre = dto.genreTags.joinToString(", ")
            status = dto.status.toSmangaStatus()
            thumbnail_url = dto.coverImage
            description = buildString {
                dto.description?.takeIf { it.isNotBlank() }?.let { appendLine(it).appendLine() }
                if (dto.alternativeTitles.isNotEmpty()) {
                    appendLine("Also known as: ${dto.alternativeTitles.joinToString(", ")}")
                }
                if (dto.externalLinks.isNotEmpty()) {
                    appendLine("Elsewhere:")
                    dto.externalLinks.forEach { appendLine("• ${it.siteName}: ${it.url}") }
                }
                append(READING_UNAVAILABLE)
            }
            // Everything the list view shows is already populated, so Mihon need not refetch.
            initialized = true
        }
    }

    override fun chapterListRequest(manga: SManga): Request =
        GET(baseUrl + manga.url + "/chapters", headers, PAGE_LIMIT.toString())

    // No chapters: the API has scene records, not readable pages. See the class comment.
    override fun chapterListParse(response: Response): List<SChapter> = emptyList()

    override fun pageListRequest(chapter: SChapter): Request =
        throw IOException(READING_UNAVAILABLE)

    override fun pageListParse(response: Response) =
        throw IOException(READING_UNAVAILABLE)

    // ---- settings ------------------------------------------------------------------------------

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        screen.addPreference(
            EditTextPreference(screen.context).apply {
                key = PREF_API_KEY
                title = "API key"
                dialogTitle = "API key"
                summary = apiKey.ifBlank { NOT_SET_SUMMARY }
                setOnBindEditTextListener { text ->
                    // Masked: it is a credential.
                    text.inputType =
                        InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
            },
        )
    }

    // ---- plumbing ------------------------------------------------------------------------------

    private fun pageParams(
        page: Int,
        sort: String,
        order: String,
        extra: okhttp3.HttpUrl.Builder.() -> Unit = {},
    ) = "$baseUrl$API_PREFIX/pornhwa".toHttpUrl().newBuilder()
        .addQueryParameter("page", page.toString())
        .addQueryParameter("limit", PAGE_LIMIT.toString())
        .addQueryParameter("sort", sort)
        .addQueryParameter("order", order)
        .apply(extra)
        .build()

    /**
     * Decodes the body, turning an HTTP error into a message that names the actual problem.
     *
     * Mihon renders a thrown message verbatim, so a missing or wrong key says so here instead of
     * surfacing as a generic failure. The API has no login, so there is nothing for the user to
     * sign into - a 401 is always the header.
     */
    private inline fun <reified T> Response.parse(): T {
        val text = body!!.string()
        if (!isSuccessful) {
            throw IOException(
                when (code) {
                    401, 403 -> "Pornhwa DB rejected the API key (HTTP $code). Set it in the source settings."
                    429 -> "Rate limited by Pornhwa DB (HTTP 429). Try again later."
                    else -> "Pornhwa DB request failed (HTTP $code)"
                },
            )
        }
        return json.decodeFromString(text)
    }

    private fun toSManga(dto: MangaDto) = SManga.create().apply {
        url = "/pornhwa/${dto.slug}"
        title = dto.title
        author = dto.authors.joinToString(", ")
        artist = dto.artists.joinToString(", ")
        genre = dto.genreTags.joinToString(", ")
        status = dto.status.toSmangaStatus()
        thumbnail_url = dto.coverImage
        description = dto.description
        initialized = true
    }

    private fun String?.toSmangaStatus(): Int = when (this?.lowercase()) {
        "completed" -> SManga.COMPLETED
        "on going", "ongoing" -> SManga.ONGOING
        "hiatus" -> SManga.CANCELLED
        else -> SManga.UNKNOWN
    }

    companion object {
        const val API_PREFIX = "/api/v1"
        const val PAGE_LIMIT = 20
        const val PREF_API_KEY = "pornhwadb_api_key"
        const val NOT_SET_SUMMARY = "Not set - every request will fail with HTTP 401"
        const val READING_UNAVAILABLE =
            "This catalogue does not host chapter images, so there is nothing to read here."

        private const val PREFS = "pornhwadb_extension"
    }
}

// ---- wire format -------------------------------------------------------------------------------

@Serializable
private data class MangaDto(
    val slug: String,
    val title: String,
    val coverImage: String? = null,
    val status: String? = null,
    val description: String? = null,
    val genreTags: List<String> = emptyList(),
    val authors: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
)

@Serializable
private data class ListResponse(
    val data: List<MangaDto>,
    val pagination: PaginationDto,
)

@Serializable
private data class SearchResponse(
    val data: SearchData,
    val pagination: Map<String, PaginationDto?> = emptyMap(),
)

@Serializable
private data class SearchData(
    @SerialName("pornhwa") val pornhwa: List<MangaDto> = emptyList(),
)

@Serializable
private data class PaginationDto(val hasMore: Boolean = false)

@Serializable
private data class DetailResponse(val data: DetailDto)

@Serializable
private data class DetailDto(
    val slug: String,
    val title: String,
    val coverImage: String? = null,
    val status: String? = null,
    val description: String? = null,
    val genreTags: List<String> = emptyList(),
    val authors: List<String> = emptyList(),
    val artists: List<String> = emptyList(),
    val alternativeTitles: List<String> = emptyList(),
    val externalLinks: List<ExternalLinkDto> = emptyList(),
)

@Serializable
private data class ExternalLinkDto(
    val siteName: String,
    val url: String,
)