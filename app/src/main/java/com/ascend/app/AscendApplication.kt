package com.ascend.app

import android.app.Application
import com.ascend.app.core.database.AscendDatabase
import com.ascend.app.core.datastore.UserPreferences
import com.ascend.app.cloud.GoogleProgressService
import com.ascend.app.cloud.SystemAiService

class AscendApplication : Application() {
    val database by lazy { AscendDatabase.create(this) }
    val preferences by lazy { UserPreferences(this) }
    val cloudProgress by lazy { GoogleProgressService(this) }
    val systemAi by lazy { SystemAiService(this) }
    val repository by lazy { AscendRepository(database.dao(), preferences, cloudProgress, systemAi) }
}
