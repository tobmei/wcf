package de.tob.wcf.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import de.tob.wcf.InputAdapter
import de.tob.wcf.PatternAdapter
import de.tob.wcf.Utility
import de.tob.wcf.databinding.FragmentInputBinding

class InputFragment : Fragment() {

    companion object {
        fun newInstance() = InputFragment()
    }

    private lateinit var viewBinding: FragmentInputBinding

    private val viewModel: InputViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewBinding = FragmentInputBinding.inflate(layoutInflater)

        val inputList: RecyclerView = viewBinding.inputList
        inputList.layoutManager = GridLayoutManager(context, 3)
        val inputAdapter = InputAdapter {
            //viewModel.currentPatterns.postValue(Utility.getPatternsFromInput(it))
        }
        inputList.adapter = inputAdapter
        viewModel.allInputs.observe(viewLifecycleOwner){
            inputAdapter.submitList(it)
        }

        val patternList = viewBinding.patternList
        patternList.layoutManager = GridLayoutManager(context, 5)
        val patternAdapter = PatternAdapter {

        }
        patternList.adapter = patternAdapter
        viewModel.currentPatterns.observe(viewLifecycleOwner) {
            patternAdapter.submitList(it)
        }


        viewModel.currentSelection.observe(viewLifecycleOwner) { current ->

        }

        return viewBinding.root
    }
}