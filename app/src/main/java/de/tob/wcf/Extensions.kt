package de.tob.wcf

import android.util.Log
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

fun Lifecycle.addLogging(fragment: Fragment) {
    addObserver(object : LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            when (event) {
                Lifecycle.Event.ON_RESUME -> Log.i(fragment.javaClass.name, "onResume")
                Lifecycle.Event.ON_PAUSE -> Log.i(fragment.javaClass.name, "onPause")
                else -> Unit
            }
        }
    })
}

fun Fragment.addLifecycleLogging() = viewLifecycleOwner.lifecycle.addLogging(this)