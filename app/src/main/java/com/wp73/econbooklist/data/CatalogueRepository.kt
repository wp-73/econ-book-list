package com.wp73.econbooklist.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CatalogueSummary(
    val bookCount: Int,
    val readingListCount: Int,
    val appearanceCount: Int,
    val datedBookCount: Int,
    val genreCount: Int
)

data class Appearance(
    val sourceId: String,
    val position: Int
)

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val appearances: List<Appearance>,
    val citations: Long?,
    val rank: Int,
    val year: Int?,
    val yearBand: String?,
    val authorship: String?,
    val genre: String?,
    val meanPosition: Double?,
    val bestPosition: Int?
) {
    val listCount: Int get() = appearances.size
}

data class SourceList(
    val id: String,
    val title: String,
    val category: String,
    val url: String,
    val entryCount: Int
)

data class Catalogue(
    val summary: CatalogueSummary,
    val methodology: List<String>,
    val sources: List<SourceList>,
    val books: List<Book>
)

class CatalogueRepository(private val context: Context) {
    fun load(): Catalogue {
        val raw = context.assets.open("economics-reading-catalogue.json")
            .bufferedReader()
            .use { it.readText() }
        val root = JSONObject(raw)
        val summaryJson = root.getJSONObject("summary")
        val summary = CatalogueSummary(
            bookCount = summaryJson.optInt("book_count"),
            readingListCount = summaryJson.optInt("reading_list_count"),
            appearanceCount = summaryJson.optInt("appearance_count"),
            datedBookCount = summaryJson.optInt("dated_book_count"),
            genreCount = summaryJson.optInt("genre_count")
        )

        val methodology = root.optJSONArray("methodology").toStringList()
        val sources = root.optJSONArray("sources").toObjectList { source ->
            SourceList(
                id = source.optString("id"),
                title = source.optString("title"),
                category = source.optString("category"),
                url = source.optString("url"),
                entryCount = source.optJSONArray("entries")?.length() ?: 0
            )
        }
        val books = root.optJSONArray("books").toObjectListIndexed { index, book ->
            val appearances = book.optJSONArray("appearances").toObjectList { appearance ->
                Appearance(
                    sourceId = appearance.optString("source_id"),
                    position = appearance.optInt("position")
                )
            }
            val calculatedMean = appearances.takeIf { it.isNotEmpty() }
                ?.map { it.position }
                ?.average()
            Book(
                id = book.optString("id"),
                title = book.optString("title"),
                author = book.optString("author"),
                appearances = appearances,
                citations = book.numberOrNull("citations")?.toLong(),
                rank = book.intOrNull("rank") ?: index + 1,
                year = book.intOrNull("year"),
                yearBand = book.stringOrNull("year_band"),
                authorship = book.stringOrNull("authorship"),
                genre = book.stringOrNull("genre"),
                meanPosition = book.doubleOrNull("mean_position") ?: calculatedMean,
                bestPosition = book.intOrNull("best_position")
            )
        }

        return Catalogue(summary, methodology, sources, books)
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) add(optString(index))
    }
}

private fun <T> JSONArray?.toObjectList(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) add(transform(getJSONObject(index)))
    }
}

private fun <T> JSONArray?.toObjectListIndexed(transform: (Int, JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) add(transform(index, getJSONObject(index)))
    }
}

private fun JSONObject.stringOrNull(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private fun JSONObject.intOrNull(key: String): Int? =
    if (!has(key) || isNull(key)) null else (opt(key) as? Number)?.toInt()

private fun JSONObject.doubleOrNull(key: String): Double? =
    if (!has(key) || isNull(key)) null else (opt(key) as? Number)?.toDouble()

private fun JSONObject.numberOrNull(key: String): Number? =
    if (!has(key) || isNull(key)) null else opt(key) as? Number
