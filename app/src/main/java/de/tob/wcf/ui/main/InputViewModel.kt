package de.tob.wcf.ui.main

import android.app.Application
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tob.wcf.R
import de.tob.wcf.Utility
import de.tob.wcf.WCFApplication
import de.tob.wcf.db.Input
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class InputViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as WCFApplication).repository

    val allInputs = repository.allInputs

    private lateinit var currentPatternInputList: List<Input>
    private lateinit var currentPatternList: List<List<Int>>
    private lateinit var currentPatternToSum: Map<Int,Int>
    private lateinit var currentPatternToAdj: Map<Int, List<BitSet>>

    private val _stateFlow: MutableStateFlow<InputViewState> by lazy { MutableStateFlow(initialState()) }
    val stateFlow: StateFlow<InputViewState> by lazy { _stateFlow }

    private val _currentSelectionFlow: MutableStateFlow<SelectionState> by lazy { MutableStateFlow(SelectionState.NoSelection) }
    val currentSelectionFlow: StateFlow<SelectionState> by lazy { _currentSelectionFlow }

    private val _eventFlow: MutableSharedFlow<InputViewEvent> = MutableSharedFlow()
    val eventFlow: SharedFlow<InputViewEvent> = _eventFlow

    fun onAction(action: InputViewAction) {
        Log.i(this.javaClass.name, "onAction(): $action")
        when (action) {
            is InputViewAction.OnGenerateClicked -> {
                mutateState { InputViewState.Loading }
                generatePatterns()
            }
            is InputViewAction.onInputSelected -> mutateSelectionState { SelectionState.CurrentSelection(action.selection) }
            is InputViewAction.OnCreateClicked -> {
                viewModelScope.launch {
                    _eventFlow.emit(InputViewEvent.NavigateTo(
                        R.id.action_inputFragment_to_outputFragment,
                        Bundle().apply {
                            putSerializable("input", currentPatternList as ArrayList)
                            putSerializable("sum", currentPatternToSum as HashMap)
                            putSerializable("adj", currentPatternToAdj as HashMap)
                        }
                    ))
                    mutateState { InputViewState.Idle }
                }
            }
            InputViewAction.onDrawClicked -> {
                viewModelScope.launch {
                    _eventFlow.emit(InputViewEvent.NavigateTo(R.id.action_inputFragment_to_drawingFragment))
                    mutateState { InputViewState.Idle }
                }
            }
            InputViewAction.onDeleteClicked -> {
                viewModelScope.launch(Default) {
                    when (currentSelectionFlow.value) {
                        is SelectionState.CurrentSelection -> {
                            repository.delete((currentSelectionFlow.value as SelectionState.CurrentSelection).currentSelection)
                            mutateState { InputViewState.Idle }
                        }
                        SelectionState.NoSelection -> {} //never happens
                    }
                }
            }
            InputViewAction.onEditClicked -> {
                viewModelScope.launch {
                    _eventFlow.emit(InputViewEvent.NavigateTo(
                        R.id.action_inputFragment_to_drawingFragment,
                        Bundle().apply {
                            putParcelable("toEdit", (currentSelectionFlow.value as SelectionState.CurrentSelection).currentSelection)
                        }
                    ))
                    mutateState { InputViewState.Idle }
                }
            }
        }
    }

    private fun generatePatterns() {
        viewModelScope.launch(Default) {
            when (currentSelectionFlow.value) {
                is SelectionState.CurrentSelection -> {
                    Utility.getPatternsFromInput((currentSelectionFlow.value as SelectionState.CurrentSelection).currentSelection).let { triple ->
                        currentPatternInputList = triple.first.map { Input(x=3, y=3, pixels=it) }
                        currentPatternList = triple.first
                        currentPatternToSum = triple.second
                        currentPatternToAdj = triple.third
                    }
                    _eventFlow.emit(InputViewEvent.PatternsGenerated(currentPatternInputList))
                    mutateState { InputViewState.Loaded }
                }
                SelectionState.NoSelection -> {}
            }
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
    private fun mutateSelectionState(callback: (SelectionState) -> SelectionState) {
        viewModelScope.launch {
            _currentSelectionFlow.take(1).collect { state ->
                _currentSelectionFlow.emit(callback(state))
            }
        }
    }
}

sealed class SelectionState {
    object NoSelection: SelectionState()
    data class CurrentSelection(val currentSelection: Input): SelectionState()
}

sealed class InputViewState {
    object Idle: InputViewState()
    object Loading: InputViewState()
    object Loaded: InputViewState()
}

sealed class InputViewEvent {
    data class PatternsGenerated(val patternList: List<Input>): InputViewEvent()
    data class NavigateTo(val destination: Int, val bundle: Bundle? = null): InputViewEvent()
}

sealed class InputViewAction {
    object OnGenerateClicked: InputViewAction()
    object OnCreateClicked: InputViewAction()
    object onDrawClicked: InputViewAction()
    object onDeleteClicked: InputViewAction()
    object onEditClicked: InputViewAction()
    data class onInputSelected(val selection: Input): InputViewAction()

}