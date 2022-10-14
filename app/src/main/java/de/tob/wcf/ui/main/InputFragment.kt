package de.tob.wcf.ui.main

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
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
import de.tob.wcf.db.Input
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InputFragment : Fragment() {
    private lateinit var binding: FragmentInputBinding
    private val viewModel: InputViewModel by viewModels()

    private lateinit var inputAdapter: InputAdapter
    private lateinit var patternAdapter: PatternAdapter
    private lateinit var input: List<Input>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentInputBinding.inflate(layoutInflater)
        addLifecycleLogging()

        val recyclerViewInput: RecyclerView = binding.inputList
        recyclerViewInput.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.allInputs.collect { list ->
                    input = list
                    if(list.isNotEmpty()) {
                        viewModel.onAction(InputViewAction.onInputSelected(input.first()))
                    }
                    inputAdapter = InputAdapter(input) {
                        viewModel.onAction(InputViewAction.onInputSelected(it))
                    }
                    recyclerViewInput.adapter = inputAdapter
                }
            }
        }

        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.currentSelectionFlow.collectLatest { selectionState ->
                    Log.i(this.javaClass.name, "selectionState collected: $selectionState")
                    when (selectionState) {
                        is SelectionState.NoSelection -> {}
                        is SelectionState.CurrentSelection -> {
                            binding.btnGenerate.isEnabled = true
                            binding.btnDelete.isEnabled = true
                            binding.btnEdit.isEnabled = true
                        }
                    }
                }
            }
        }

        bindToViewModel()
        setListener()

        return binding.root
    }

    private fun bindToViewModel() {
        with (binding) {
            val recyclerViewPattern: RecyclerView = patternList
            recyclerViewPattern.layoutManager = GridLayoutManager(context, 5)
            patternAdapter = PatternAdapter {}
            recyclerViewPattern.adapter = patternAdapter

            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    viewModel.eventFlow.collect { event ->
                        Log.i(this.javaClass.name, "eventFlow collected: $event")
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
                        Log.i(this.javaClass.name, "stateFlow collected: $state")
                        when (state) {
                            is InputViewState.Idle -> {
                                patternAdapter.submitList(emptyList())
                                setUpInitialState()
                            }
                            is InputViewState.Loading -> {
                                patternAdapter.submitList(emptyList())
                                progressBar.visibility = View.VISIBLE
                                tvHint.visibility = View.INVISIBLE
                                btnGenerate.isEnabled = false
                                btnCreate.isEnabled = false
                            }
                            is InputViewState.Loaded -> {
                                progressBar.visibility = View.INVISIBLE
                                tvHint.visibility = View.INVISIBLE
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
                viewModel.onAction(InputViewAction.OnGenerateClicked)
            }
            btnCreate.setOnClickListener {
                viewModel.onAction(InputViewAction.OnCreateClicked)
            }
            btnDraw.setOnClickListener {
                viewModel.onAction(InputViewAction.onDrawClicked)
            }
            btnDelete.setOnClickListener {
                viewModel.onAction(InputViewAction.onDeleteClicked)
            }
            btnEdit.setOnClickListener {
                viewModel.onAction(InputViewAction.onEditClicked)
            }
        }
    }

    private fun setUpInitialState() {
        Log.i(this.javaClass.name, "setupInitialState()")
        with (binding) {
            progressBar.visibility = View.INVISIBLE
            tvHint.visibility = View.VISIBLE
            btnCreate.isEnabled = false
        }
    }

}