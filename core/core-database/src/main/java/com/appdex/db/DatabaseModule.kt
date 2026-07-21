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
    fun provideDatabase(@ApplicationContext context: Context): AppXDatabase {
        return Room.databaseBuilder(
            context,
            AppXDatabase::class.java,
            "AppX.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideBookmarkDao(db: AppXDatabase) = db.bookmarkDao()

    @Provides
    fun provideRecentPathDao(db: AppXDatabase) = db.recentPathDao()

    @Provides
    fun provideSearchHistoryDao(db: AppXDatabase) = db.searchHistoryDao()
}
