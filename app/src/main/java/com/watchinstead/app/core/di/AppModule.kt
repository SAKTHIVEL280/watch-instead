package com.watchinstead.app.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.watchinstead.app.data.ApplicationScope
import com.watchinstead.app.data.Clock
import com.watchinstead.app.data.DefaultLinkRepository
import com.watchinstead.app.data.DefaultStatsRepository
import com.watchinstead.app.data.LinkRepository
import com.watchinstead.app.data.StatsRepository
import com.watchinstead.app.data.local.WatchInsteadDatabase
import com.watchinstead.app.data.local.MIGRATION_1_2
import com.watchinstead.app.data.local.PromptEventDao
import com.watchinstead.app.data.local.SavedLinkDao
import com.watchinstead.app.data.metadata.HttpMetadataFetcher
import com.watchinstead.app.data.metadata.MetadataFetcher
import com.watchinstead.app.data.settings.DataStoreSettingsRepository
import com.watchinstead.app.data.settings.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun statsRepository(impl: DefaultStatsRepository): StatsRepository
    @Binds abstract fun linkRepository(impl: DefaultLinkRepository): LinkRepository
    @Binds abstract fun settingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds abstract fun metadataFetcher(impl: HttpMetadataFetcher): MetadataFetcher
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): WatchInsteadDatabase =
        Room.databaseBuilder(context, WatchInsteadDatabase::class.java, "watch_instead.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun savedLinkDao(database: WatchInsteadDatabase): SavedLinkDao = database.savedLinkDao()

    @Provides
    fun promptEventDao(database: WatchInsteadDatabase): PromptEventDao = database.promptEventDao()

    @Provides @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("settings") }

    @Provides @Singleton @ApplicationScope
    fun applicationScope() = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun clock() = Clock(System::currentTimeMillis)

    @Provides @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .callTimeout(10, TimeUnit.SECONDS)
        .build()
}
