package com.condorserg.scopedstorage

import android.app.Activity
import android.app.RemoteAction
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.condorserg.scopedstorage.adapter.VideosListAdapter
import com.condorserg.scopedstorage.databinding.FragmentVideosListBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class VideosListFragment : Fragment(R.layout.fragment_videos_list) {
    private var _binding: FragmentVideosListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: VideosListViewModel by viewModels()
    private var videosAdapter: VideosListAdapter? = null

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var recoverableActionLauncher: ActivityResultLauncher<IntentSenderRequest>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVideosListBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.addVideoButton.setOnClickListener {
            BottomFragment().show(childFragmentManager, "tag")
        }

        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPermissionResultListener()
        initRecoverableActionListener()

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initList()
        bindViewModel()
        if (hasPermission().not()) {
            requestPermissions()
        }
        viewModel.loadList()
        viewModel.showToast
            .observe(viewLifecycleOwner, ::showToast)
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.updatePermissionState(hasPermission())
    }

    private fun initPermissionResultListener() {
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissionToGrantedMap: Map<String, Boolean> ->
            if (permissionToGrantedMap.values.all { it }) {
                viewModel.permissionsGranted()
            } else {
                viewModel.permissionsDenied()
            }
        }
    }

    private fun initRecoverableActionListener() {
        recoverableActionLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { activityResult ->
            val isConfirmed = activityResult.resultCode == Activity.RESULT_OK
            if (isConfirmed) {
                viewModel.confirmDelete()
            } else {
                viewModel.declineDelete()
            }
        }
    }

    private fun hasPermission(): Boolean {
        return PERMISSIONS.all {
            ActivityCompat.checkSelfPermission(
                requireContext(),
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(PERMISSIONS.toTypedArray())
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun bindViewModel() {
        viewModel.videosLiveData.observe(viewLifecycleOwner) { videosAdapter?.items = it }
        viewModel.recoverableActionLiveData.observe(viewLifecycleOwner, ::handleRecoverableAction)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleRecoverableAction(action: RemoteAction) {
        val request = IntentSenderRequest.Builder(action.actionIntent.intentSender)
            .build()
        recoverableActionLauncher.launch(request)
    }

    private fun initList() {
        videosAdapter = VideosListAdapter(
            onLongClicked = { position ->
                val dialogItems = arrayOf(getString(R.string.Delete))
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.ChooseAction))
                    .setItems(dialogItems)
                    { _, which ->
                        when (which) {
                            0 -> {
                                videosAdapter?.items?.let { viewModel.deleteVideo(it[position].id) }
                            }
                        }
                    }
                    .show()
            }
        )
        with(binding.videosList) {
            adapter = videosAdapter
            layoutManager = LinearLayoutManager(context)
            setHasFixedSize(true)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        videosAdapter = null
    }

    companion object {
        private val PERMISSIONS = listOfNotNull(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                .takeIf { Build.VERSION.SDK_INT < Build.VERSION_CODES.Q }
        )
    }
}

