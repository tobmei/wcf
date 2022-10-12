package de.tob.wcf.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.tob.wcf.InputAdapter
import de.tob.wcf.PatternAdapter
import de.tob.wcf.R
import de.tob.wcf.addLifecycleLogging
import de.tob.wcf.databinding.FragmentInputBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class InputFragment : Fragment() {
    private lateinit var binding: FragmentInputBinding
    private val viewModel: InputViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInputBinding.inflate(layoutInflater)
        addLifecycleLogging()

        bindToViewModel()
        setListener()

        return binding.root
    }

    private fun bindToViewModel() {
        with (binding) {
            val recyclerViewInput: RecyclerView = inputList
            recyclerViewInput.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            val inputAdapter = InputAdapter {
                viewModel.onAction(InputViewAction.onInputSelected(it))
                btnGenerate.isEnabled = true
            }
            recyclerViewInput.adapter = inputAdapter
            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.allInputs.collect{
                        inputAdapter.submitList(it)
                    }
                }
            }

            val recyclerViewPattern: RecyclerView = patternList
            recyclerViewPattern.layoutManager = GridLayoutManager(context, 5)
            val patternAdapter = PatternAdapter {}
            recyclerViewPattern.adapter = patternAdapter

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.eventFlow.collect { event ->
                        when (event) {
                            is InputViewEvent.PatternsGenerated -> patternAdapter.submitList(event.patternList)
                            is InputViewEvent.NavigateTo -> findNavController().navigate(event.destination, event.bundle)
                        }
                    }
                }
            }

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.stateFlow.collectLatest { state ->
                        when (state) {
                            is InputViewState.Idle -> {
                                setUpInitialState()
                            }
                            is InputViewState.Loading -> {
                                patternAdapter.submitList(emptyList())
                                progressBar.visibility = View.VISIBLE
                                btnGenerate.isEnabled = false
                                btnCreate.isEnabled = false
                                hideOptions()
                            }
                            is InputViewState.Loaded -> {
                                progressBar.visibility = View.INVISIBLE
                                btnGenerate.isEnabled = true
                                btnCreate.isEnabled = true
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setListener() {
        with (binding) {
            btnGenerate.setOnClickListener {
                viewModel.onAction(InputViewAction.OnGenerateClicked(cbRotation.isChecked))
            }

            btnInputExpand.setOnClickListener {
                toggleExpanded(inputList, it)
            }

            btnOptionsExpand.setOnClickListener {
                toggleExpanded(clPatternOptions, it)
            }

            btnCreate.setOnClickListener {
                viewModel.onAction(InputViewAction.OnCreateClicked)
            }

            btnDraw.setOnClickListener {
                viewModel.onAction(InputViewAction.onDrawClicked)
            }
        }
    }

    private fun setUpInitialState() {
        with (binding) {
            progressBar.visibility = View.INVISIBLE
            clPatternOptions.visibility = View.GONE
            btnOptionsExpand.setBackgroundResource(R.drawable.ic_arrow_right)
            btnInputExpand.setBackgroundResource(R.drawable.ic_arrow_down)
            btnGenerate.isEnabled = false
            btnCreate.isEnabled = false
        }
    }

    private fun hideOptions() {
        binding.clPatternOptions.visibility = View.GONE
        binding.btnOptionsExpand.setBackgroundResource(R.drawable.ic_arrow_right)
    }

    private fun toggleExpanded(v: View, b: View) {
        when (v.visibility) {
            View.VISIBLE -> {
                v.visibility = View.GONE
                b.setBackgroundResource(R.drawable.ic_arrow_right)
            }
            else -> {
                v.visibility = View.VISIBLE
                b.setBackgroundResource(R.drawable.ic_arrow_down)
            }
        }
    }
}