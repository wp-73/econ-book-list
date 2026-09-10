package com.wp73.econbooklist.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

class UserPreferences(private val context: Context) {
    private object Keys {
        val tickedIds = stringSetPreferencesKey("ticked_book_ids")
        val themeMode = stringPreferencesKey("theme_mode")
    }

    private val preferences = context.userPreferencesDataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    val tickedIds: Flow<Set<String>> = preferences.map { it[Keys.tickedIds] ?: emptySet() }

    val themeMode: Flow<ThemeMode> = preferences.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[Keys.themeMode] ?: ThemeMode.SYSTEM.name) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setBookTicked(bookId: String, ticked: Boolean) {
        context.userPreferencesDataStore.edit { prefs ->
            val updated = (prefs[Keys.tickedIds] ?: emptySet()).toMutableSet()
            if (ticked) updated.add(bookId) else updated.remove(bookId)
            prefs[Keys.tickedIds] = updated
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.userPreferencesDataStore.edit { prefs -> prefs[Keys.themeMode] = mode.name }
    }
}
