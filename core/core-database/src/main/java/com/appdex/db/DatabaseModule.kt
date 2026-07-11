package com.appdex.db

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDexDatabase {
        return Room.databaseBuilder(
            context,
            AppDexDatabase::class.java,
            "appdex.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideBookmarkDao(db: AppDexDatabase) = db.bookmarkDao()

    @Provides
    fun provideRecentPathDao(db: AppDexDatabase) = db.recentPathDao()

    @Provides
    fun provideSearchHistoryDao(db: AppDexDatabase) = db.searchHistoryDao()
}
