package de.tob.wcf.ui.main

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import de.tob.wcf.R
import de.tob.wcf.addLifecycleLogging
import de.tob.wcf.databinding.FragmentInputBinding
import de.tob.wcf.databinding.FragmentOutputBinding
import de.tob.wcf.db.Input

class OutputFragment : Fragment() {

    private var m1: String? = null
    private lateinit var binding: FragmentOutputBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            var m1 = it.getSerializable("input") as List<*>
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentOutputBinding.inflate(layoutInflater)
        addLifecycleLogging()

        binding.view

        return binding.root
    }

}