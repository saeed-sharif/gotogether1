package com.bodyshapeeditor.slim_body_photo_editor.fragments

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.bodyshapeeditor.slim_body_photo_editor.CustomGalleryAdapter
import com.bodyshapeeditor.slim_body_photo_editor.PhotoEditorViewModel
import com.bodyshapeeditor.slim_body_photo_editor.R
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentGalleryBinding

class GalleryFragment : Fragment() {

    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!
    private val args: GalleryFragmentArgs by navArgs()
    private val photoEditorViewModel: PhotoEditorViewModel by viewModels()
    private lateinit var customAdapter: CustomGalleryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_galleryFragment_to_homeFragment)
                }
            }
        )

        val category = args.category
        setupRecyclerView(category)
        setupObservers()
    }

    private fun setupRecyclerView(category: String?) {
        val imageUris = getDemoImageUris()
        customAdapter = CustomGalleryAdapter(
            images = imageUris.toMutableList(),
            onItemClick = { imageUri ->
                navigateToEditor(category, imageUri)
                Log.d("GalleryFragment", imageUri.toString())
                Log.d("GalleryFragment", "$category")
            },
            category = "customgallery"
        )

        binding.costomgalleryRV.apply {
            layoutManager = GridLayoutManager(requireContext(), 4)
            adapter = customAdapter
        }
    }

    private fun getDemoImageUris(): List<Uri> {
        val demoImageIds = listOf(
            R.drawable.model_1,
            R.drawable.model_2,
            R.drawable.model_3
        )

        return demoImageIds.map { drawableId ->
            Uri.parse("android.resource://${requireContext().packageName}/$drawableId")
        }
    }

    private fun setupObservers() {
        photoEditorViewModel.imageUris.observe(viewLifecycleOwner) { uris ->
            val combinedUris = getDemoImageUris() + uris
            customAdapter.updateData(combinedUris)
        }
    }

    private fun navigateToEditor(category: String?, imageUri: Uri) {
        when (category) {
            "height" -> {
                val action = GalleryFragmentDirections.actionGalleryFragmentToHeightFragment(imageUri.toString())
                findNavController().navigate(action)
            }
            "slim" -> {
                val action = GalleryFragmentDirections.actionGalleryFragmentToSlimFragment(imageUri.toString())
                findNavController().navigate(action)
            }
            "waist" -> {
                val action = GalleryFragmentDirections.actionGalleryFragmentToWaistFragment(imageUri.toString())
                findNavController().navigate(action)
            }
            "belly" -> {
                val action = GalleryFragmentDirections.actionGalleryFragmentToBellyFragment(imageUri.toString())
                findNavController().navigate(action)
            }
            "breast" -> {
                val action = GalleryFragmentDirections.actionGalleryFragmentToBreastFragment(imageUri.toString())
                findNavController().navigate(action)
            }
            "hip" -> {
                val action = GalleryFragmentDirections.actionGalleryFragmentToHipFragment(imageUri.toString())
                findNavController().navigate(action)
            }
            else -> {
                findNavController().navigateUp()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        photoEditorViewModel.loadImagesFromDevice()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}