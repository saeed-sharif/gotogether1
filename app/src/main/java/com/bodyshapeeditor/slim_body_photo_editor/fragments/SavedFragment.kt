package com.bodyshapeeditor.slim_body_photo_editor.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.bodyshapeeditor.slim_body_photo_editor.CustomGalleryAdapter
import com.bodyshapeeditor.slim_body_photo_editor.PhotoEditorHelper
import com.bodyshapeeditor.slim_body_photo_editor.R
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentSavedBinding

class SavedFragment : Fragment() {
    lateinit var _binding: FragmentSavedBinding
    private val binding get() = _binding
    lateinit var savedAdapter: CustomGalleryAdapter
    var photoEditorHelper = PhotoEditorHelper()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSavedRecyclerView()
        getSavedImages()
        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_savedFragment_to_homeFragment)
        }
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_savedFragment_to_homeFragment)


                }
            })
    }

    private fun getSavedImages() {
        val savedImages = photoEditorHelper.getAppImages(requireContext())
        Log.d("ImagesUri", savedImages.toString())
        if (savedImages.isEmpty()) {
            binding.savedRv.visibility = View.GONE
            return
        } else {
            binding.imagesDescription.visibility = View.GONE
            binding.savedRv.visibility = View.VISIBLE

            savedAdapter.updateData(savedImages)

        }
    }

    fun setupSavedRecyclerView() {
        // Initialize the adapter
        savedAdapter = CustomGalleryAdapter(
            images = mutableListOf(),
            onItemClick = { imageUri ->
                val action = SavedFragmentDirections
                    .actionSavedFragmentToSavedImageDetailFragment(imageUri.toString())
                findNavController().navigate(action)
            },
            category = "savedfragment"
        )

        binding.savedRv.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = savedAdapter
        }
    }

}