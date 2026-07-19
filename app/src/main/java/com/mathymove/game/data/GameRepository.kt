package com.mathymove.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mathymove.game.model.GameState
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
