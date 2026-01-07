package com.jksalcedo.fossia

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Fossia Application Entry Point
 * 
 * Annotated with @HiltAndroidApp to trigger Hilt's code generation,
 * including a base class for the application that serves as the
 * application-level dependency container.
 */
@HiltAndroidApp
class FossiaApp : Application()
