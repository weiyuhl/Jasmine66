package com.lhzkml.jasmine.feature.sandbox.impl.di

import android.content.Context
import com.android.sandbox.AndroidSandboxController
import com.android.sandbox.SandboxController
import com.android.sandbox.core.LinuxSandboxManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object SandboxModule {

    @Provides
    @ActivityRetainedScoped
    fun provideLinuxSandboxManager(
        @ApplicationContext context: Context,
    ): LinuxSandboxManager {
        return LinuxSandboxManager(context)
    }

    @Provides
    @ActivityRetainedScoped
    fun provideSandboxController(
        sandboxManager: LinuxSandboxManager,
    ): SandboxController {
        return AndroidSandboxController(sandboxManager)
    }
}
