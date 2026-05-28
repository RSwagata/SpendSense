package com.spendsense.di

import com.spendsense.llm.DynamicLlmProvider
import com.spendsense.llm.LlmProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LlmModule {

    @Binds
    @Singleton
    abstract fun bindLlmProvider(impl: DynamicLlmProvider): LlmProvider
}
