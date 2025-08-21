package com.bodyshapeeditor.slim_body_photo_editor.fragments

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bodyshapeeditor.slim_body_photo_editor.PhotoEditorHelper
import com.bodyshapeeditor.slim_body_photo_editor.R
import com.bodyshapeeditor.slim_body_photo_editor.ads.showBanner
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentSavedImageDetailBinding
import com.bodyshapeeditor.slim_body_photo_editor.helper.ImagePagerAdapter

class SavedImageDetailFragment : Fragment() {
    private var _binding: FragmentSavedImageDetailBinding? = null
    private val binding get() = _binding!!
    var photoEditorHelper = PhotoEditorHelper()
    private lateinit var imageUris: List<Uri>
    private var startPosition: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedImageDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        showBanner(
            requireContext(),
            binding.root.findViewById(R.id.ad_view_container),
            binding.root.findViewById(R.id.layout_adView),
            binding.ads
        )

        val imageUriString = arguments?.getString("imageUri")
        val selectedImageUri = Uri.parse(imageUriString)

        // Get all saved images
        imageUris = photoEditorHelper.getAppImages(requireContext())

        // Find index of selected image
        startPosition = imageUris.indexOfFirst { it.toString() == selectedImageUri.toString() }

        val adapter = ImagePagerAdapter(imageUris)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(startPosition, false)

        binding.backBtn.setOnClickListener {
          findNavController().navigate(R.id.action_savedImageDetailFragment_to_savedFragment)
        }
        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_savedImageDetailFragment_to_savedFragment)


                }
            })

        binding.shareBtn.setOnClickListener {
            val currentUri = imageUris[binding.viewPager.currentItem]
            photoEditorHelper.shareImageToAnyApp(requireContext(), currentUri)
        }

        binding.deleteBtn.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Are you sure to delete")
                .setMessage("By pressing the OK then the image is permanently deleted.")
                .setPositiveButton("OK") { dialog, _ ->
                    val currentUri = imageUris[binding.viewPager.currentItem]
                    val deleted = photoEditorHelper.deleteImageByUri(requireContext(), currentUri)
                    if (deleted) {
                        // Remove from list and notify adapter
                        imageUris = photoEditorHelper.getAppImages(requireContext())
                        if (imageUris.isEmpty()) {
                            requireActivity().onBackPressed()
                        } else {
                            binding.viewPager.adapter = ImagePagerAdapter(imageUris)
                        }
                    }


                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()

        }
    }
}
