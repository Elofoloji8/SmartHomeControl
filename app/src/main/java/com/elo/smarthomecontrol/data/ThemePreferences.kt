package com.elo.smarthomecontrol.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 🔹 Context'e DataStore genişletmesi ekliyoruz
val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreferences(private val context: Context) {
    companion object {
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    // 🌗 Dark mode açık mı kapalı mı takip ediyoruz
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE] ?: false
        }

    // 💾 Tema durumunu kaydeden fonksiyon
    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE] = enabled
        }
    }
}