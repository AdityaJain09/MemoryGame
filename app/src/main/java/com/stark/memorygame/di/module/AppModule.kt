package com.stark.memorygame.di.module

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.stark.memorygame.data.UserDataSource
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class AppModule {

    companion object {
        private const val USER_INFO = "memory_game_user_info"
    }

    private val Context.dataStore by preferencesDataStore(USER_INFO)

    @Singleton
    @Provides
    fun provideUserDataStore(context: Context): UserDataSource {
        return UserDataSource(context.dataStore)
    }

}