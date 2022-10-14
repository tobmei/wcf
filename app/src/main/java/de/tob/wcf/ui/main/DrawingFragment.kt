package de.tob.wcf.ui.main

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet.Motion
import androidx.core.view.doOnAttach
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.tob.wcf.ColorAdapter
import de.tob.wcf.addLifecycleLogging
import de.tob.wcf.databinding.FragmentDrawingBinding
import de.tob.wcf.db.Input
import kotlinx.coroutines.launch

class DrawingFragment : Fragment() {

    private lateinit var binding: FragmentDrawingBinding
    private val viewModel: DrawingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments.let { bundle ->
            if (bundle != null) {
                bundle.getParcelable<Input>("toEdit")?.let {
                    viewModel.onAction(DrawingViewAction.EditRecieved(it))
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDrawingBinding.inflate(layoutInflater, container, false)
        addLifecycleLogging()

        val recycler: RecyclerView = binding.colorList
        recycler.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val colorAdapter = ColorAdapter {
            viewModel.onAction(DrawingViewAction.ColorSelected(it))
        }
        recycler.adapter = colorAdapter
        colorAdapter.submitList(
            listOf(
                Color.BLACK, Color.DKGRAY, Color.GRAY, Color.LTGRAY, Color.BLUE,
                Color.CYAN, Color.GREEN, Color.MAGENTA, Color.YELLOW
            )
        )

        binding.btnClear.setOnClickListener {
            viewModel.onAction(DrawingViewAction.ClearClicked)
        }
        binding.btnSave.setOnClickListener {
            viewModel.onAction(DrawingViewAction.SaveClicked)
            activity?.onBackPressed()
        }
        binding.btnFill.setOnClickListener {
            binding.btnPen.scaleX = 1F
            binding.btnPen.scaleY = 1F
            binding.btnFill.scaleX = 1.3F
            binding.btnFill.scaleY = 1.3F
            viewModel.onAction(DrawingViewAction.FillClicked)
        }
        binding.btnPen.setOnClickListener {
            binding.btnPen.scaleX = 1.3F
            binding.btnPen.scaleY = 1.3F
            binding.btnFill.scaleX = 1F
            binding.btnFill.scaleY = 1F
            viewModel.onAction(DrawingViewAction.DrawClicked)
        }
        binding.btnPen.scaleX = 1.3F
        binding.btnPen.scaleY = 1.3F
        binding.btnFill.scaleX = 1F
        binding.btnFill.scaleY = 1F
        processState()

        return binding.root
    }

    private fun processState() {
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stateFlow.collect { state ->
                    when (state) {
                        else -> {}
                    }
                }
            }
        }
    }

}