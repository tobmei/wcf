package de.tob.wcf.ui.main

import android.app.Application
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tob.wcf.db.Input
import de.tob.wcf.db.InputDao
import de.tob.wcf.db.OutputDao
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class OutputViewModel(application: Application) : AndroidViewModel(application) {

    private lateinit var currentSelection: Input
    private lateinit var currentPatternList: List<Input>
    private lateinit var currentPatternToAdj: Map<Int, List<BitSet>>
    private var rotationOption: Boolean = false

    private val _stateFlow: MutableStateFlow<OutputViewState> by lazy { MutableStateFlow(initialState()) }
    val stateFlow: StateFlow<OutputViewState> by lazy { _stateFlow }

    private val _eventFlow: MutableSharedFlow<OutputViewEvent> = MutableSharedFlow()
    val eventFlow: SharedFlow<OutputViewEvent> = _eventFlow

    fun onAction(action: OutputViewState) {
        Log.i(this.javaClass.name, "onAction(): $action")
    }

    private fun generatePatterns() {
        viewModelScope.launch(Default) {

        }
    }

    private fun initialState(): OutputViewState = error("")

    private fun mutateState(callback: (OutputViewState) -> OutputViewState) {
        viewModelScope.launch {
            _stateFlow.take(1).collect {state ->
                _stateFlow.emit(callback(state))
            }
        }
    }
}

sealed class OutputViewState {
}

sealed class OutputViewEvent {

}


sealed class OutputViewAction {

}

class WCFRepository(private val inputDao: InputDao, private val outputDao: OutputDao) {

    val allInputs: Flow<List<Input>> = inputDao.getAll()

    @WorkerThread
    suspend fun insert(input: Input) {
        inputDao.insert(input)
    }
    @WorkerThread
    suspend fun delete(input: Input) {
        inputDao.delete(input.id)
    }
}