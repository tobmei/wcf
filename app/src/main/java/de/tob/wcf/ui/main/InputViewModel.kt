package de.tob.wcf.ui.main

import android.app.Application
import android.os.Build.VERSION_CODES.N
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import de.tob.wcf.WCFApplication
import de.tob.wcf.db.Input
import de.tob.wcf.db.InputDao
import de.tob.wcf.db.OutputDao
import kotlinx.coroutines.flow.Flow
import java.util.*
import kotlin.math.absoluteValue

class InputViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as WCFApplication).repository

    val allInputs = repository.allInputs.asLiveData()

    val currentPatterns = MutableLiveData<List<Input>>()

    val currentSelection = MutableLiveData<Input>()

}



class WCFRepository(private val inputDao: InputDao, outputDao: OutputDao) {

    val allInputs: Flow<List<Input>> = inputDao.getAll()

    @WorkerThread
    suspend fun insert(input: Input) {
        inputDao.insert(input)
    }
}