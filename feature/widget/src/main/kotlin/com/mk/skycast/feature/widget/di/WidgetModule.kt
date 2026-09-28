package com.mk.skycast.feature.widget.di

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.mk.skycast.core.domain.repository.WidgetRefresher
import com.mk.skycast.feature.widget.BriefWidget
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object WidgetModule {
    @Provides
    fun provideWidgetRefresher(@ApplicationContext context: Context): WidgetRefresher =
        WidgetRefresher { BriefWidget().updateAll(context) }
}
