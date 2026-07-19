package com.mathymove.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mathymove.game.model.GameState
import com.mathymove.game.model.HighScoreEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mathymove_game_prefs")

class GameRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    companion object {
        private val KEY_SAVED_GAME = stringPreferencesKey("saved_game_state_json")
        private val KEY_HIGH_SCORES = stringPreferencesKey("high_scores_json")
    }

    val savedGameState: Flow<GameState?> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_SAVED_GAME]
        if (!jsonStr.isNullOrEmpty()) {
            try {
                json.decodeFromString<GameState>(jsonStr).copy(hasSavedGame = true)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    val highScores: Flow<List<HighScoreEntry>> = context.dataStore.data.map { prefs ->
        val jsonStr = prefs[KEY_HIGH_SCORES]
        if (!jsonStr.isNullOrEmpty()) {
            try {
                json.decodeFromString<List<HighScoreEntry>>(jsonStr)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun addHighScore(score: Int) {
        if (score <= 0) return
        val formatter = java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault())
        val now = System.currentTimeMillis()
        val formattedDate = formatter.format(java.util.Date(now))
        val newEntry = com.mathymove.game.model.HighScoreEntry(
            score = score,
            timestamp = now,
            formattedDateTime = formattedDate
        )

        context.dataStore.edit { prefs ->
            val jsonStr = prefs[KEY_HIGH_SCORES]
            val currentList = if (!jsonStr.isNullOrEmpty()) {
                try { json.decodeFromString<List<HighScoreEntry>>(jsonStr) } catch (e: Exception) { emptyList() }
            } else emptyList()

            val updatedList = (currentList + newEntry)
                .sortedByDescending { it.score }
                .take(10)

            prefs[KEY_HIGH_SCORES] = json.encodeToString(updatedList)
        }
    }

    suspend fun saveGame(state: GameState) {
        // Do not save game over state as active continue state
        if (state.isGameOver) {
            clearSavedGame()
            return
        }
        val jsonStr = json.encodeToString(state)
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVED_GAME] = jsonStr
        }
    }

    suspend fun clearSavedGame() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_SAVED_GAME)
        }
    }
}
