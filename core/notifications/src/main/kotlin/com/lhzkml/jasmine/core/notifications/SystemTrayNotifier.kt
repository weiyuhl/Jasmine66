
package com.lhzkml.jasmine.core.notifications

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class SystemTrayNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : Notifier
