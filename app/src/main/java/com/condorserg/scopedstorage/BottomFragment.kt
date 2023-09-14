package com.condorserg.scopedstorage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.condorserg.scopedstorage.databinding.BottomSheetDialogBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.processNextEventInCurrentThread

class BottomFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDialogBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VideosListViewModel by viewModels()

    private lateinit var createFileLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initCreateFileLauncher()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            BottomSheetDialogBinding.bind(inflater.inflate(R.layout.bottom_sheet_dialog, container))
        val view = binding.root

        binding.downloadButton.setOnClickListener {
            //https://download.samplelib.com/mp4/sample-5s.mp4
            val url = binding.urlEditText.text.toString()
            var fileName = binding.fileNameEditText.text.toString()
            if (url.isNotBlank()) {
                if (fileName.isBlank()) {
                    fileName = url.substringAfterLast("/")
                    binding.fileNameEditText.setText(fileName)
                }
                viewModel.saveVideo(url, fileName)
                viewModel.saveFileState.observe(viewLifecycleOwner, ::setSavingState)
            }
        }

        binding.downloadDifferentButton.setOnClickListener {
            createFile()
            viewModel.saveFileState.observe(viewLifecycleOwner, ::setSavingState)

        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.showToast
            .observe(viewLifecycleOwner, ::showToast)
        viewModel.isLoading.observe(viewLifecycleOwner, ::updateLoadingState)

    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.downloadButton.isEnabled = isLoading.not()
        binding.downloadDifferentButton.isEnabled = isLoading.not()
        binding.fileNameTextField.isEnabled = isLoading.not()
        binding.urlTextField.isEnabled = isLoading.not()

        binding.progressBar.isVisible = isLoading
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private fun setSavingState(state:Boolean) {
        if (state) this.dismiss()
    }

    private fun createFile() {
        val fileName = if (binding.fileNameEditText.text.toString().isBlank()) {
            binding.urlEditText.text.toString().substringAfterLast("/")
        } else {
            binding.fileNameEditText.text.toString()
        }
        createFileLauncher.launch(fileName)
    }

    private fun initCreateFileLauncher() {
        createFileLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument()
        ) { uri ->
            val url = binding.urlEditText.text.toString()
            viewModel.handleCreateFile(url, uri)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}