package com.ascend.app

import android.app.Application
import com.ascend.app.core.database.AscendDatabase
import com.ascend.app.core.datastore.UserPreferences
import com.ascend.app.cloud.GoogleProgressService
import com.ascend.app.cloud.SystemAiService
import com.google.firebase.FirebaseApp

class AscendApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.initializeApp(this) != null) {
            AppCheckConfigurator.install()
        }
    }

    val database by lazy { AscendDatabase.create(this) }
    val preferences by lazy { UserPreferences(this) }
    val cloudProgress by lazy { GoogleProgressService(this) }
    val systemAi by lazy { SystemAiService(this) }
    val repository by lazy { AscendRepository(database, preferences, cloudProgress, systemAi) }
}
