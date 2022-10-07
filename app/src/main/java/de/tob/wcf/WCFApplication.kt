package de.tob.wcf

import android.app.Application
import de.tob.wcf.db.WCFDatabase
import de.tob.wcf.ui.main.WCFRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class WCFApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob())
    val database by lazy { WCFDatabase.getDatabase(this, applicationScope) }
    val repository by lazy { WCFRepository(database.inputDao(), database.outputDao()) }
}