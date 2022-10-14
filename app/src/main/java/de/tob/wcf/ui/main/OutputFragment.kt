package de.tob.wcf.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import de.tob.wcf.R
import de.tob.wcf.addLifecycleLogging
import de.tob.wcf.databinding.FragmentInputBinding
import de.tob.wcf.databinding.FragmentOutputBinding
import de.tob.wcf.db.Input
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.util.BitSet

class OutputFragment : Fragment() {

    private val viewModel: OutputViewModel by viewModels()

    private lateinit var list: List<List<Int>>
    private lateinit var sum: Map<Int,Int>
    private lateinit var adj: Map<Int, MutableList<BitSet>>
    private lateinit var binding: FragmentOutputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            list = it.getSerializable("input") as List<List<Int>>
            sum = it.getSerializable("sum") as Map<Int, Int>
            adj = it.getSerializable("adj") as Map<Int, MutableList<BitSet>>
            viewModel.onAction(OutputViewAction.DataRecieved(list, sum, adj))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOutputBinding.inflate(layoutInflater)
        addLifecycleLogging()

        binding.btnRedo.setOnClickListener {
            viewModel.onAction(OutputViewAction.RedoClicked)
        }
        return binding.root
    }
}