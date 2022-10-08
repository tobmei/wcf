package de.tob.wcf.ui.main

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import de.tob.wcf.Utility
import de.tob.wcf.WCFApplication
import de.tob.wcf.db.Input
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class InputViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as WCFApplication).repository

    val allInputs = repository.allInputs.asLiveData()

    private lateinit var currentSelection: Input
    private lateinit var currentPatternList: List<Input>
    private lateinit var currentPatternToSum: Map<Int,Int>
    private lateinit var currentPatternToAdj: Map<Int, List<BitSet>>

    private val _stateFlow: MutableStateFlow<InputViewState> by lazy { MutableStateFlow(initialState()) }
    val stateFlow: StateFlow<InputViewState> by lazy { _stateFlow }

    private val _eventFlow: MutableSharedFlow<InputViewEvent> = MutableSharedFlow()
    val eventFlow: SharedFlow<InputViewEvent> = _eventFlow

    fun onAction(action: InputViewAction) {
        Log.i(this.javaClass.name, "onAction(): $action")
        when (action) {
            is InputViewAction.OnGenerateClicked -> {
                mutateState { InputViewState.Loading }
                generatePatterns(action.rotate)
            }
            is InputViewAction.onInputSelected -> currentSelection = action.selection
            is InputViewAction.OnCreateClicked -> {}
            else -> {}
        }
    }

    private fun generatePatterns(rotate: Boolean) {
        viewModelScope.launch(Default) {
            Utility.getPatternsFromInput(currentSelection, rotate).let { triple ->
                currentPatternList = triple.first.map { Input(x=3, y=3, pixels=it) }
                currentPatternToSum = triple.second
                currentPatternToAdj = triple.third
            }
            _eventFlow.emit(InputViewEvent.PatternsGenerated(currentPatternList))
            mutateState { InputViewState.Loaded }
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

sealed class InputViewState {
    object Idle: InputViewState()
    object Loading: InputViewState()
    object Loaded: InputViewState()
}

sealed class InputViewEvent {
    data class PatternsGenerated(val patternList: List<Input>): InputViewEvent()
    data class NavigateTo(val intent: Intent): InputViewEvent()
}

sealed class InputViewAction {
    data class OnGenerateClicked(val rotate: Boolean): InputViewAction()
    object OnCreateClicked: InputViewAction()
    data class onInputSelected(val selection: Input): InputViewAction()

}