package com.stark.memorygame.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

class UserDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    companion object {
        private val USER_NAME = stringPreferencesKey("creator_user_name")
    }

    suspend fun setUserName(username: String) {
        dataStore.edit { preferences ->
            preferences[USER_NAME] = username
        }
    }

    suspend fun getUserName(): Flow<String?> {
        return dataStore.data.catch { e ->
            if (e is IOException) {
                emit(emptyPreferences())
            } else {
                throw e
            }
        }.map {
            it[USER_NAME]
        }
    }

}
