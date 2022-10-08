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

    private val _stateFlow: MutableStateFlow<InputViewState> by lazy { MutableStateFlow(initialState()) }
    val stateFlow: StateFlow<InputViewState> by lazy { _stateFlow }

    private val _eventFlow: MutableSharedFlow<InputViewEvent> = MutableSharedFlow()
    val eventFlow: SharedFlow<InputViewEvent> = _eventFlow

    fun onAction(action: InputViewAction) {
        Log.i(this.javaClass.name, "onAction(): $action")
    }

    private fun generatePatterns() {
        viewModelScope.launch(Default) {

        }
    }

    private fun initialState(): InputViewState = InputViewState.Idle

    private fun mutateState(callback: (InputViewState) -> InputViewState) {
        viewModelScope.launch {
            _stateFlow.take(1).collect {state ->
                _stateFlow.emit(callback(state))
            }
        }
    }
}

sealed class OutputViewState {
    object Idle: InputViewState()
    object Loading: InputViewState()
    data class Loaded(val patterns: List<Input>): InputViewState()
}

sealed class OutputViewEvent {
    data class GeneratePattern(val option: Int): InputViewEvent()
}

sealed class OutputViewAction {
    object OnGenerateClicked: InputViewAction()
    data class OnRotationChecked(val checked: Boolean): InputViewAction()
    data class onInputSelected(val selection: Input): InputViewAction()
}

class WCFRepository(private val inputDao: InputDao, outputDao: OutputDao) {

    val allInputs: Flow<List<Input>> = inputDao.getAll()

    @WorkerThread
    suspend fun insert(input: Input) {
        inputDao.insert(input)
    }
}