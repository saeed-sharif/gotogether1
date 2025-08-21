package com.bodyshapeeditor.slim_body_photo_editor.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bodyshapeeditor.slim_body_photo_editor.R
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentSettingBinding

class SettingFragment : Fragment() {
    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rateUs.setOnClickListener { rateUs() }
        binding.privacyPolicy.setOnClickListener { privacyPolicy() }
        binding.shareBtn.setOnClickListener { shareApp() }
        binding.backBtn.setOnClickListener { findNavController().popBackStack() }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack()
        }

    }



    private fun privacyPolicy() {
        val privacyPolicyUrl = getText(R.string.privacy_policy_link).toString()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))
        startActivity(intent)
    }

    private fun rateUs() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=${requireContext().packageName}")
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data =
                    Uri.parse("https://play.google.com/store/apps/details?id=${requireContext().packageName}")
            }
            startActivity(intent)

        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Check out this app!")
            putExtra(
                Intent.EXTRA_TEXT,
                "Download the app: https://play.google.com/store/apps/details?id=${requireContext().packageName}"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }


}