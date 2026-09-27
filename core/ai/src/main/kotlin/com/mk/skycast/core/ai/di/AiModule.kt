package com.mk.skycast.core.ai.di

import com.mk.skycast.core.ai.FirebaseAdviceGenerator
import com.mk.skycast.core.ai.TelephonyAiRegionPolicy
import com.mk.skycast.core.domain.ai.AdviceGenerator
import com.mk.skycast.core.domain.ai.AiRegionPolicy
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AiModule {
    @Binds
    abstract fun bindAdviceGenerator(impl: FirebaseAdviceGenerator): AdviceGenerator

    @Binds
    abstract fun bindRegionPolicy(impl: TelephonyAiRegionPolicy): AiRegionPolicy
}
