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

    private val _eventFlow: MutableSharedFlow<OutputViewEvent> = MutableSharedFlow(replay = 1)
    val eventFlow: SharedFlow<OutputViewEvent> = _eventFlow

    fun onAction(action: OutputViewAction) {
        Log.i(this.javaClass.name, "onAction(): $action")
        when (action) {
            is OutputViewAction.RedoClicked -> {
                viewModelScope.launch {
                    _eventFlow.emit(OutputViewEvent.Redo)
                }
            }
            is OutputViewAction.DataRecieved -> {
                viewModelScope.launch {
                    _eventFlow.emit(OutputViewEvent.Start(action.list, action.sum, action.adj))
                }
            }
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
    data class Start(
        val list: List<List<Int>>,
        val sum: Map<Int, Int>,
        val adj: Map<Int, MutableList<BitSet>>
    ): OutputViewEvent()
    object Redo: OutputViewEvent()
}

sealed class OutputViewAction {
    object RedoClicked: OutputViewAction()
    data class DataRecieved(
        val list: List<List<Int>>,
        val sum: Map<Int, Int>,
        val adj: Map<Int, MutableList<BitSet>>
        ): OutputViewAction()
}

class WCFRepository(private val inputDao: InputDao, private val outputDao: OutputDao) {

    val allInputs: Flow<List<Input>> = inputDao.getAll()

    @WorkerThread
    suspend fun insert(input: Input) = inputDao.insert(input)
    @WorkerThread
    suspend fun delete(input: Input) = inputDao.delete(input.id)
    @WorkerThread
    suspend fun update(input: Input) = inputDao.update(input)

}