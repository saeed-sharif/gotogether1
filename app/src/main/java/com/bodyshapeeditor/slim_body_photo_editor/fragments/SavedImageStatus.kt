package com.bodyshapeeditor.slim_body_photo_editor.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bodyshapeeditor.slim_body_photo_editor.PhotoEditorHelper
import com.bodyshapeeditor.slim_body_photo_editor.R
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentSavedImageStatusBinding

class SavedImageStatus : Fragment() {

    private var _binding: FragmentSavedImageStatusBinding? = null
    private val binding get() = _binding!!
     var photoEditorHelper= PhotoEditorHelper()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedImageStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val imageUri = arguments?.getString("imageUri")?.let {
            Uri.parse(it)
        }
        if (imageUri != null) {
            binding.savedImage.text = "Image saved!"
        } else {
            binding.savedImage.text = "Failed to save image."
        }
        binding.imageView.setImageURI(imageUri)

        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_savedImageStatus_to_homeFragment)
        }
        requireActivity().onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.action_savedImageStatus_to_homeFragment)
            }

        })
        binding.homeBtn.setOnClickListener {
            findNavController().navigate(R.id.action_savedImageStatus_to_homeFragment)
        }
        binding.shareBtn.setOnClickListener {
         photoEditorHelper.shareImageToAnyApp(requireContext(),imageUri!!)
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
