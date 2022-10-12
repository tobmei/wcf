package de.tob.wcf.ui.main

import android.app.Application
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.WorkerThread
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    private val _pixelFlow: MutableSharedFlow<Array<Int>> = MutableSharedFlow(extraBufferCapacity = 2, onBufferOverflow = BufferOverflow.SUSPEND)
    val pixelFlow: SharedFlow<Array<Int>> = _pixelFlow

    private val nCol = 4
    private val nRow = 4
    private var pixels = emptyArray<Int>()

    init {
        pixels = Array(nCol*nRow){0}
    }

    private fun setup() {
        viewModelScope.launch {
            _pixelFlow.emit(pixels)
        }
        viewModelScope.launch {
            _eventFlow.tryEmit(DrawingViewEvent.CanvasSetup(nCol, nRow))
        }
    }

    fun onAction(action: DrawingViewAction) {
        Log.i(this.javaClass.name, "onAction(): $action")
        when (action) {
            is DrawingViewAction.Setup -> {
                viewModelScope.launch {
                    _eventFlow.emit(DrawingViewEvent.PixelsChanged(pixels))
                }
            }
            is DrawingViewAction.ColorSelected -> {
                viewModelScope.launch {
                    _eventFlow.emit(DrawingViewEvent.ColorChanged(action.color))
                }
            }
        }
    }

    private fun initialState(): DrawingViewState = DrawingViewState.Foo

    private fun mutateState(callback: (DrawingViewState) -> DrawingViewState) {
        viewModelScope.launch {
            _stateFlow.take(1).collect {state ->
                _stateFlow.emit(callback(state))
            }
        }
    }
}

sealed class DrawingViewState {
    object Foo: DrawingViewState()
}

sealed class DrawingViewEvent {
    data class ColorChanged(val color: Int): DrawingViewEvent()
    data class PixelsChanged(val pixels: Array<Int>): DrawingViewEvent()
    data class CanvasSetup(val nCol: Int, val nRow: Int): DrawingViewEvent()
}

sealed class DrawingViewAction {
    object Setup: DrawingViewAction()
    data class ColorSelected(val color: Int): DrawingViewAction()
}
