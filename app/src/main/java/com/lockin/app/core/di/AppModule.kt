package com.lockin.app.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.lockin.app.data.ApplicationScope
import com.lockin.app.data.Clock
import com.lockin.app.data.DefaultLinkRepository
import com.lockin.app.data.LinkRepository
import com.lockin.app.data.local.LockinDatabase
import com.lockin.app.data.local.SavedLinkDao
import com.lockin.app.data.metadata.HttpMetadataFetcher
import com.lockin.app.data.metadata.MetadataFetcher
import com.lockin.app.data.settings.DataStoreSettingsRepository
import com.lockin.app.data.settings.SettingsRepository
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
    @Binds abstract fun linkRepository(impl: DefaultLinkRepository): LinkRepository
    @Binds abstract fun settingsRepository(impl: DataStoreSettingsRepository): SettingsRepository
    @Binds abstract fun metadataFetcher(impl: HttpMetadataFetcher): MetadataFetcher
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun database(@ApplicationContext context: Context): LockinDatabase =
        Room.databaseBuilder(context, LockinDatabase::class.java, "lockin.db").build()

    @Provides
    fun savedLinkDao(database: LockinDatabase): SavedLinkDao = database.savedLinkDao()

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
