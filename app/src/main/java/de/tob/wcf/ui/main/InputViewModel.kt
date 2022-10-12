package de.tob.wcf.ui.main

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tob.wcf.R
import de.tob.wcf.Utility
import de.tob.wcf.WCFApplication
import de.tob.wcf.db.Input
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class InputViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as WCFApplication).repository

    val allInputs = repository.allInputs

    private lateinit var currentSelection: Input
    private lateinit var currentPatternInputList: List<Input>
    private lateinit var currentPatternList: List<List<Int>>
    private lateinit var currentPatternToSum: Map<Int,Int>
    private lateinit var currentPatternToAdj: Map<Int, List<BitSet>>

    private val _stateFlow: MutableStateFlow<InputViewState> by lazy { MutableStateFlow(initialState()) }
    val stateFlow: StateFlow<InputViewState> by lazy { _stateFlow }

    private val _eventFlow: MutableSharedFlow<InputViewEvent> =
        MutableSharedFlow()
    val eventFlow: SharedFlow<InputViewEvent> = _eventFlow

    fun onAction(action: InputViewAction) {
        Log.i(this.javaClass.name, "onAction(): $action")
        when (action) {
            is InputViewAction.OnGenerateClicked -> {
                mutateState { InputViewState.Loading }
                generatePatterns(action.rotate)
            }
            is InputViewAction.onInputSelected -> currentSelection = action.selection
            is InputViewAction.OnCreateClicked -> {
                viewModelScope.launch {
                    _eventFlow.emit(InputViewEvent.NavigateTo(
                        R.id.action_inputFragment_to_outputFragment,
                        Bundle().apply {
                            putSerializable("input", currentPatternList as ArrayList)
                        }
                    ))
                }
            }
            InputViewAction.onDrawClicked -> {
                viewModelScope.launch {
                    _eventFlow.emit(InputViewEvent.NavigateTo(
                        R.id.action_inputFragment_to_drawingFragment,
                        Bundle().apply {
                            putInt("nCol", 12)
                            putInt("nRow", 12)
                        }
                    ))
                }
            }
        }
    }

    private fun generatePatterns(rotate: Boolean) {
        viewModelScope.launch(Default) {
            Utility.getPatternsFromInput(currentSelection, rotate).let { triple ->
                currentPatternInputList = triple.first.map { Input(x=3, y=3, pixels=it) }
                currentPatternList = triple.first
                currentPatternToSum = triple.second
                currentPatternToAdj = triple.third
            }
            _eventFlow.emit(InputViewEvent.PatternsGenerated(currentPatternInputList))
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
    data class NavigateTo(val destination: Int, val bundle: Bundle): InputViewEvent()
}

sealed class InputViewAction {
    data class OnGenerateClicked(val rotate: Boolean): InputViewAction()
    object OnCreateClicked: InputViewAction()
    object onDrawClicked: InputViewAction()
    data class onInputSelected(val selection: Input): InputViewAction()

}