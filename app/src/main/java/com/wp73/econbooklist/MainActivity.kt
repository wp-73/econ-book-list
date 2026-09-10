package com.wp73.econbooklist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wp73.econbooklist.data.Book
import com.wp73.econbooklist.data.Catalogue
import com.wp73.econbooklist.data.CatalogueRepository
import com.wp73.econbooklist.data.SourceList
import com.wp73.econbooklist.data.ThemeMode
import com.wp73.econbooklist.data.UserPreferences
import com.wp73.econbooklist.ui.EconBookTheme
import java.text.DecimalFormat
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val preferences by lazy { UserPreferences(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val catalogueResult = runCatching { CatalogueRepository(applicationContext).load() }
        setContent {
            val themeMode by preferences.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            EconBookTheme(themeMode) {
                if (catalogueResult.isSuccess) {
                    val catalogue = catalogueResult.getOrThrow()
                    val tickedIds by preferences.tickedIds.collectAsState(initial = emptySet())
                    val scope = rememberCoroutineScope()
                    EconBookApp(
                        catalogue = catalogue,
                        tickedIds = tickedIds,
                        themeMode = themeMode,
                        onBookTicked = { id, ticked -> scope.launch { preferences.setBookTicked(id, ticked) } },
                        onThemeMode = { mode -> scope.launch { preferences.setThemeMode(mode) } }
                    )
                } else {
                    LoadError(catalogueResult.exceptionOrNull())
                }
            }
        }
    }
}

private enum class AppSection(val label: String) { BOOKS("Books"), SOURCES("Sources"), ABOUT("About") }
private enum class BookStatus(val label: String) { ALL("All"), TICKED("Ticked"), UNTICKED("Unticked") }
private enum class BookSort(val label: String) { RANKING("Ranking"), TITLE("Title A–Z"), NEWEST("Newest"), CITATIONS("Citations") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EconBookApp(
    catalogue: Catalogue,
    tickedIds: Set<String>,
    themeMode: ThemeMode,
    onBookTicked: (String, Boolean) -> Unit,
    onThemeMode: (ThemeMode) -> Unit
) {
    var section by rememberSaveable { mutableStateOf(AppSection.BOOKS.name) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Economics Books") },
                actions = { TextButton(onClick = { showAppearance = true }) { Text("Appearance") } }
            )
        },
        bottomBar = {
            NavigationBar {
                AppSection.entries.forEach { item ->
                    NavigationBarItem(
                        selected = section == item.name,
                        onClick = { section = item.name },
                        icon = { Text(if (section == item.name) "●" else "○") },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { padding ->
        when (AppSection.valueOf(section)) {
            AppSection.BOOKS -> BooksScreen(catalogue, tickedIds, onBookTicked, Modifier.padding(padding))
            AppSection.SOURCES -> SourcesScreen(catalogue.sources, Modifier.padding(padding))
            AppSection.ABOUT -> AboutScreen(catalogue, tickedIds.size, Modifier.padding(padding))
        }
    }

    if (showAppearance) {
        AppearanceDialog(
            selected = themeMode,
            onSelect = onThemeMode,
            onDismiss = { showAppearance = false }
        )
    }
}

@Composable
private fun BooksScreen(
    catalogue: Catalogue,
    tickedIds: Set<String>,
    onBookTicked: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    var statusName by rememberSaveable { mutableStateOf(BookStatus.ALL.name) }
    var genre by rememberSaveable { mutableStateOf<String?>(null) }
    var sortName by rememberSaveable { mutableStateOf(BookSort.RANKING.name) }
    var selectedBookId by rememberSaveable { mutableStateOf<String?>(null) }

    val selectedBook = catalogue.books.firstOrNull { it.id == selectedBookId }
    if (selectedBook != null) {
        BookDetailScreen(
            book = selectedBook,
            sources = catalogue.sources.associateBy { it.id },
            ticked = selectedBook.id in tickedIds,
            onTicked = { onBookTicked(selectedBook.id, it) },
            onBack = { selectedBookId = null },
            modifier = modifier
        )
        return
    }

    val status = BookStatus.valueOf(statusName)
    val sort = BookSort.valueOf(sortName)
    val genres = remember(catalogue.books) { catalogue.books.mapNotNull { it.genre }.distinct().sorted() }
    val visibleBooks = remember(catalogue.books, query, status, genre, sort, tickedIds) {
        val needle = query.trim().lowercase()
        val filtered = catalogue.books.filter { book ->
            val matchesText = needle.isBlank() || listOf(book.title, book.author, book.genre.orEmpty())
                .any { it.lowercase().contains(needle) }
            val matchesStatus = when (status) {
                BookStatus.ALL -> true
                BookStatus.TICKED -> book.id in tickedIds
                BookStatus.UNTICKED -> book.id !in tickedIds
            }
            matchesText && matchesStatus && (genre == null || book.genre == genre)
        }
        when (sort) {
            BookSort.RANKING -> filtered.sortedBy { it.rank }
            BookSort.TITLE -> filtered.sortedBy { it.title.lowercase() }
            BookSort.NEWEST -> filtered.sortedWith(compareByDescending<Book> { it.year ?: Int.MIN_VALUE }.thenBy { it.rank })
            BookSort.CITATIONS -> filtered.sortedWith(compareByDescending<Book> { it.citations ?: -1L }.thenBy { it.rank })
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Reading economics, one list at a time.",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${tickedIds.size} of ${catalogue.summary.bookCount} ticked",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Search title, author or subject") }
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(BookStatus.entries) { option ->
                    FilterChip(
                        selected = status == option,
                        onClick = { statusName = option.name },
                        label = { Text(option.label) }
                    )
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SimpleMenuButton(
                    label = genre ?: "All subjects",
                    options = listOf("All subjects") + genres,
                    onSelect = { genre = it.takeUnless { value -> value == "All subjects" } },
                    modifier = Modifier.weight(1f)
                )
                SimpleMenuButton(
                    label = sort.label,
                    options = BookSort.entries.map { it.label },
                    onSelect = { selected -> sortName = BookSort.entries.first { it.label == selected }.name },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Text(
                "${visibleBooks.size} books",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(visibleBooks, key = { it.id }) { book ->
            BookCard(
                book = book,
                ticked = book.id in tickedIds,
                onTicked = { onBookTicked(book.id, it) },
                onOpen = { selectedBookId = book.id }
            )
        }
        if (visibleBooks.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No books match these filters.")
                }
            }
        }
    }
}

@Composable
private fun BookCard(book: Book, ticked: Boolean, onTicked: (Boolean) -> Unit, onOpen: () -> Unit) {
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${book.rank}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    if (book.genre != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(book.genre, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(book.author, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(7.dp))
                val stats = buildList {
                    add("${book.listCount} list${if (book.listCount == 1) "" else "s"}")
                    book.year?.let { add(it.toString()) }
                    book.citations?.let { add("${formatInteger(it)} citations") }
                }
                Text(stats.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            }
            Checkbox(checked = ticked, onCheckedChange = onTicked)
        }
    }
}

@Composable
private fun BookDetailScreen(
    book: Book,
    sources: Map<String, SourceList>,
    ticked: Boolean,
    onTicked: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TextButton(onClick = onBack) { Text("← Back to books") } }
        item {
            Text(book.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(book.author, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (ticked) "Ticked" else "Not ticked", fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(12.dp))
                Switch(checked = ticked, onCheckedChange = onTicked)
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Catalogue details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    DetailLine("Rank", "#${book.rank}")
                    DetailLine("Subject", book.genre ?: "Unknown")
                    DetailLine("First published", book.year?.toString() ?: "Unknown")
                    DetailLine("Authorship", book.authorship ?: "Unknown")
                    DetailLine("Reading lists", book.listCount.toString())
                    book.meanPosition?.let { DetailLine("Average position", DecimalFormat("0.00").format(it)) }
                    book.citations?.let { DetailLine("RePEc citations", formatInteger(it)) }
                }
            }
        }
        if (book.appearances.isNotEmpty()) {
            item { Text("Recommendation sources", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
            items(book.appearances.distinctBy { it.sourceId }, key = { it.sourceId }) { appearance ->
                val source = sources[appearance.sourceId]
                Card(
                    onClick = { source?.url?.takeIf { it.isNotBlank() }?.let(uriHandler::openUri) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(source?.title ?: appearance.sourceId, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOfNotNull(source?.category, "Position #${appearance.position}").joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            item { Text("This is a citation-only catalogue addition and has no reading-list appearance in the collected sample.") }
        }
    }
}

@Composable
private fun SourcesScreen(sources: List<SourceList>, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val uriHandler = LocalUriHandler.current
    val visible = remember(sources, query) {
        val needle = query.trim().lowercase()
        sources.filter { needle.isBlank() || it.title.lowercase().contains(needle) || it.category.lowercase().contains(needle) }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Source lists", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search sources") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(visible, key = { it.id }) { source ->
            Card(onClick = { if (source.url.isNotBlank()) uriHandler.openUri(source.url) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text(source.title, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("${source.category} · ${source.entryCount} books", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun AboutScreen(catalogue: Catalogue, tickedCount: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("About the catalogue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text("A sourced catalogue of economics and finance books assembled from university reading lists, finance-industry recommendations, economist interviews and RePEc citation records.")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = {}, label = { Text("${catalogue.summary.bookCount} books") })
            AssistChip(onClick = {}, label = { Text("${catalogue.summary.readingListCount} lists") })
        }
        Text("Your progress: $tickedCount of ${catalogue.summary.bookCount} ticked.", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Text("Method & limits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        catalogue.methodology.forEachIndexed { index, method ->
            Text("${index + 1}. $method", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AppearanceDialog(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Appearance") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selected == mode, onClick = { onSelect(mode) })
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "Use system setting"
                                ThemeMode.LIGHT -> "Light"
                                ThemeMode.DARK -> "Dark"
                            }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun SimpleMenuButton(label: String, options: List<String>, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    }
                )
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun LoadError(error: Throwable?) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Could not load the catalogue", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(error?.message ?: "Unknown error", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatInteger(value: Long): String = "%,d".format(value)
