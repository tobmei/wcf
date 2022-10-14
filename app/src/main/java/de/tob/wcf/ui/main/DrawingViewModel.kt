package de.tob.wcf.ui.main

import android.app.Application
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import de.tob.wcf.WCFApplication
import de.tob.wcf.db.Input
import de.tob.wcf.db.InputDao
import de.tob.wcf.db.OutputDao
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import kotlin.collections.ArrayList

class DrawingViewModel(application: Application) : AndroidViewModel(application) {

    private val _stateFlow: MutableStateFlow<DrawingViewState> by lazy { MutableStateFlow(initialState()) }
    val stateFlow: StateFlow<DrawingViewState> by lazy { _stateFlow }

    private val _eventFlow: MutableSharedFlow<DrawingViewEvent> = MutableSharedFlow()
    val eventFlow: SharedFlow<DrawingViewEvent> = _eventFlow

    private val _pixelFlow: MutableStateFlow<PixelState> = MutableStateFlow(initialPixelState())
    val pixelFlow: StateFlow<PixelState> = _pixelFlow

    private var isEdit = false

    private val repository = (application as WCFApplication).repository

    fun onAction(action: DrawingViewAction) {
        Log.i(this.javaClass.name, "onAction(): $action")
        when (action) {
            is DrawingViewAction.ColorSelected -> {
                viewModelScope.launch {
                    _eventFlow.emit(DrawingViewEvent.ColorChanged(action.color))
                }
            }
            is DrawingViewAction.SaveClicked -> {
                viewModelScope.launch {
                    _pixelFlow.collect { pixelState ->
                        when (pixelState) {
                            is PixelState.Pixels -> {
                                if (isEdit) {
                                    repository.update(pixelState.input)
                                } else {
                                    repository.insert(pixelState.input)
                                    isEdit = false
                                }
                            }
                        }
                    }
                }
            }
            is DrawingViewAction.ClearClicked -> mutatePixelState { initialPixelState() }
            is DrawingViewAction.EditRecieved -> {
                    isEdit = true
                    mutatePixelState { PixelState.Pixels(action.input) }
            }
        }
    }

    private fun mutateState(callback: (DrawingViewState) -> DrawingViewState) {
        viewModelScope.launch {
            _stateFlow.take(1).collect { state ->
                _stateFlow.emit(callback(state))
            }
        }
    }

    fun mutatePixelState(callback: (PixelState) -> PixelState) {
        viewModelScope.launch {
            _pixelFlow.take(1).collect {state ->
                _pixelFlow.emit(callback(state))
            }
        }
    }
    private fun initialState(): DrawingViewState = DrawingViewState.Foo

    private fun initialPixelState() = PixelState.Pixels(Input(x=12,y=12,pixels=(Array(12*12) {0}).toList()))
}

sealed class DrawingViewState {
    object Foo: DrawingViewState()
}

sealed class PixelState {
    data class Pixels(val input: Input): PixelState()
}

sealed class DrawingViewEvent {
    data class ColorChanged(val color: Int): DrawingViewEvent()
    data class CanvasSetup(val nCol: Int, val nRow: Int): DrawingViewEvent()
}

sealed class DrawingViewAction {
    data class ColorSelected(val color: Int): DrawingViewAction()
    object SaveClicked: DrawingViewAction()
    object ClearClicked: DrawingViewAction()
    data class EditRecieved(val input: Input): DrawingViewAction()
}
