package com.devfalah.quiz.ui.mcq

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.devfalah.quiz.R
import com.devfalah.quiz.databinding.FragmentMcqBinding
import com.devfalah.quiz.ui.base.BaseFragment
import com.devfalah.quiz.utilities.goToFragment

class McqFragment : BaseFragment<FragmentMcqBinding>() {
    override val layoutId = R.layout.fragment_mcq
    override val bindingInflater: (LayoutInflater, Int, ViewGroup?, Boolean) -> FragmentMcqBinding =
        DataBindingUtil::inflate
    private val viewModel: McqViewModel by viewModels()

    override fun setup() {
        binding?.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@McqFragment.viewModel
        }
        addCallbacks()
        goToResultFragmentWhenGameOver()

    }


    private fun addCallbacks(){
        binding!!.exitIcon.setOnClickListener{ view ->
            view.goToFragment(McqFragmentDirections.actionMcqFragmentToExitDialog())
        }
    }


    private fun goToResultFragmentWhenGameOver(){
        viewModel.isGameOver.observe(this) { isGameOver ->
            if (isGameOver) {
                val action = McqFragmentDirections.actionMcqFragmentToResultFragment(
                    this.viewModel.correctAnswersCount.value!!,
                    this.viewModel.score.value!!
                )
                requireView().goToFragment(action)
            }
        }
    }
}