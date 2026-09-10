# Economics reading catalogue

A searchable, sourced catalogue of economics and finance books assembled from university reading lists, finance-industry recommendations, economist interviews, and RePEc citation records.

The catalogue currently contains:

- 847 books and multi-volume works
- 107 distinct reading lists
- 796 recorded source-list appearances
- 595 works with first-publication dates
- 206 citation-only additions

## Android app

The repository now also contains a native Android app in the [`app`](app) module. Open the repository root in Android Studio and run the `app` configuration.

The first Android release includes:

- the complete offline 847-book catalogue
- persistent per-book tick / untick state
- All, Ticked, and Unticked filters with a progress count
- title, author, and subject search
- subject filtering and ranking/title/newest/citation sorting
- book-detail pages with links to the recorded recommendation sources
- browsable source lists and the catalogue methodology
- System, Light, and Dark appearance modes, persisted between launches

The Android build packages [`data/economics-reading-catalogue.json`](data/economics-reading-catalogue.json) directly, so the existing structured catalogue remains the single source of truth. Tick state and the appearance preference are user data stored separately with Android DataStore.

GitHub Actions verifies the Android project with `gradle :app:assembleDebug` on changes affecting the app or catalogue.

## Browse the catalogue on the web

Open [`index.html`](index.html) to search and combine filters for subject area, publication period, authorship, recommending-source type, repeated recommendations, and citation evidence. The page is self-contained and works without a server.

The original ranking orders books by:

1. Number of distinct reading lists, descending
2. Mean first-listed position, ascending
3. Best first-listed position, ascending
4. Title and author for remaining ties

Every ranked book includes links back to its recorded recommendation sources. Counts describe this collected sample rather than the entire web.

## Other formats

- [`economics-reading-list.md`](economics-reading-list.md) contains the complete catalogue as a Markdown table.
- [`data/economics-reading-catalogue.json`](data/economics-reading-catalogue.json) contains the structured books, metadata, source register, recommendation positions, citation records, and methodology.

## Coverage and metadata

The source collection includes 21 university lists, five finance-industry lists, and 81 economist or expert lists. Eighty of the expert selections are separate Five Books interviews, so that platform has substantial weight in the results.

Publication years were matched primarily through Open Library. Uncertain dates remain explicitly marked as unknown. Subject areas are broad catalogue labels inferred from book titles, recommendation context, and bibliographic subjects. “Author type” describes authorship structure—single, joint, edited, or institutional—rather than the writer's profession.

Compiled 10 September 2026.
