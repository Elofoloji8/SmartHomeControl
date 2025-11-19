package com.elo.smarthomecontrol.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {

    companion object {
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val LANGUAGE_KEY = stringPreferencesKey("language")
    }

    val isDarkMode: Flow<Boolean> =
        context.dataStore.data.map { it[DARK_MODE_KEY] ?: false }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[DARK_MODE_KEY] = enabled }
    }

    val languageFlow: Flow<String> =
        context.dataStore.data.map { it[LANGUAGE_KEY] ?: "tr" }

    suspend fun setLanguage(languageCode: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = languageCode }
    }
}