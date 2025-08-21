package com.bodyshapeeditor.slim_body_photo_editor.fragments

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.bodyshapeeditor.slim_body_photo_editor.PhotoEditorHelper
import com.bodyshapeeditor.slim_body_photo_editor.PhotoEditorViewModel
import com.bodyshapeeditor.slim_body_photo_editor.Preferences
import com.bodyshapeeditor.slim_body_photo_editor.R
import com.bodyshapeeditor.slim_body_photo_editor.ads.getIsAdRemove
import com.bodyshapeeditor.slim_body_photo_editor.ads.saveIsAdsRemove
import com.bodyshapeeditor.slim_body_photo_editor.ads.showAdAndGo
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentHomeBinding
import com.google.firebase.messaging.FirebaseMessaging
import com.limurse.iap.BillingClientConnectionListener
import com.limurse.iap.DataWrappers
import com.limurse.iap.IapConnector
import com.limurse.iap.PurchaseServiceListener

class HomeFragment : Fragment() {
    private var backPressedOnce = false
    private var backPressDelay = 2000L
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PhotoEditorViewModel by viewModels()
    private val helper = PhotoEditorHelper()
    private lateinit var preferences: Preferences

    private lateinit var iapConnector: IapConnector

    val isBillingClientConnected: MutableLiveData<Boolean> = MutableLiveData()

    // Request permissions
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            preferences.setPermissionDenied(0) // Reset counter if permissions granted
            viewModel.loadImagesFromDevice()
            Toast.makeText(requireContext(), "Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            val deniedCount = preferences.getPermissionDenied()
            preferences.setPermissionDenied(deniedCount + 1)

            when (deniedCount + 1) {
                1 -> showPermissionRationaleDialog()
                2 -> showSettingsRedirectDialog()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        preferences = Preferences(requireContext())
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        /*playstore work */

        FirebaseMessaging.getInstance()
            .subscribeToTopic("com.bodyshapeeditor.slim_body_photo_editor")

        isBillingClientConnected.value = false

        val nonConsumablesList = listOf("remove_ads")
//        val consumablesList = listOf("base", "moderate", "quite")
//        val subsList = listOf("subscription", "yearly")

        iapConnector = IapConnector(
            context = requireContext(),
            nonConsumableKeys = nonConsumablesList,
            key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2OFDOMHzZyP+s5OiTLyE0eq2KjDXRMMrOUX73XNr+zuP/tjWtkRYwa2tOmrp/t7iuwvtfiRSwsK8Y3kg6jWSqQmrvoH9s+OR8802NVVrDKdXtE3wcmmSdpKmDaAUqItok6rfUxeLH7sUEz18Gqlqt6y6F/rMn/0BGOi0RNYijR8eBQjWLEgKU9dsikE9auNPshU2aBy2vDWZ8pboRH25pWHXcYDUFd42qQdRPq8MHGKhn+n4spaiTcCm6TgPrbUw+ZEPDNzFqKpcyBmXBdLRrF9Sc63l9wmMY96Njz3sAmaM0VRdmDWNArfSV/6DArWnFiCuDpX+AcAy9gqGiremiwIDAQAB",
            enableLogging = true
        )

        iapConnector.addBillingClientConnectionListener(object : BillingClientConnectionListener {
            override fun onConnected(status: Boolean, billingResponseCode: Int) {
                Log.d(
                    "KSA",
                    "This is the status: $status and response code is: $billingResponseCode"
                )
                isBillingClientConnected.value = status
            }
        })

        iapConnector.addPurchaseListener(object : PurchaseServiceListener {
            override fun onPricesUpdated(iapKeyPrices: Map<String, List<DataWrappers.ProductDetails>>) {
                // list of available products will be received here, so you can update UI with prices if needed
            }

            override fun onProductPurchased(purchaseInfo: DataWrappers.PurchaseInfo) {
                when (purchaseInfo.sku) {
                    "base" -> {
                        purchaseInfo.orderId
                    }

                    "remove_ads" -> {
                        saveIsAdsRemove(requireContext(), true)
                        Toast.makeText(
                            requireContext(),
                            "Purchased Successful",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onProductRestored(purchaseInfo: DataWrappers.PurchaseInfo) {
                // will be triggered fetching owned products using IapConnector;
            }

            override fun onPurchaseFailed(
                purchaseInfo: DataWrappers.PurchaseInfo?,
                billingResponseCode: Int?
            ) {
                // will be triggered whenever a product purchase is failed
                Toast.makeText(
                    requireContext(),
                    "Your purchase has been failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })


        isBillingClientConnected.observe(requireActivity()) { connected ->
            Log.d("KSA", "limurse $connected")
            when (connected) {
                true -> {
                    binding.billing.isEnabled = true
                    binding.billing.setOnClickListener {
                        iapConnector.purchase(requireActivity(), "remove_ads")
                    }
                }

                else -> {
                    binding.billing.isEnabled = false
                }
            }
        }

        if (getIsAdRemove(requireContext())) {
            binding.billing.visibility = View.GONE
        }


        //app work


        requireActivity().onBackPressedDispatcher.addCallback(requireActivity(),
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (backPressedOnce) {
                        isEnabled = false
                        requireActivity().finish()
                    } else {
                        backPressedOnce = true
                        Toast.makeText(
                            requireContext(),
                            "Press back again to exit",
                            Toast.LENGTH_SHORT
                        ).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            backPressedOnce = false
                        }, backPressDelay)
                    }
                }
            })

        // Check if we need to show permission dialog immediately
        if (helper.permission.any {
                requireContext().checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
            }) {
            if (preferences.getPermissionDenied() >= 2) {
                showSettingsRedirectDialog()
            } else {
                permissionLauncher.launch(helper.permission)
            }
        } else {
            // Permissions already granted
            viewModel.loadImagesFromDevice()
        }

        setupButtonListeners()
    }

    private fun setupButtonListeners() {
        binding.slimBtn.setOnClickListener {
            checkAndNavigate(R.id.action_home_to_slim, R.drawable.model_1)
        }

        binding.breastBtn.setOnClickListener {
            checkAndNavigate(R.id.action_home_to_breast, R.drawable.model_1)
        }

        binding.waistBtn.setOnClickListener {
            checkAndNavigate(R.id.action_home_to_waist, R.drawable.model_1)
        }

        binding.heightBtn.setOnClickListener {
            checkAndNavigate(R.id.action_home_to_height, R.drawable.model_1)
        }

        binding.bellyBtn.setOnClickListener {
            checkAndNavigate(R.id.action_home_to_belly, R.drawable.model_1)
        }

        binding.hipBtn.setOnClickListener {
            checkAndNavigate(R.id.action_home_to_hip, R.drawable.model_1)
        }

        binding.savedImages.setOnClickListener {

            if (checkPermission()) {
                requireActivity().showAdAndGo {
                    findNavController().navigate(R.id.action_homeFragment_to_savedFragment)
                }
            } else {
                checkDeniedCount(preferences.getPermissionDenied())
            }

        }

        binding.settingBtn.setOnClickListener {

            findNavController().navigate(R.id.action_homeFragment_to_settingFragment)

        }
    }

    private fun checkAndNavigate(actionId: Int, drawableId: Int) {
        val deniedCount = preferences.getPermissionDenied()
        if (checkPermission()) {
            requireActivity().showAdAndGo {
                // Create URI for drawable resource
                val uri =
                    Uri.parse("android.resource://${requireContext().packageName}/$drawableId")
                // Navigate with the URI as argument
                when (actionId) {
                    R.id.action_home_to_slim -> {
                        findNavController().navigate(
                            HomeFragmentDirections.actionHomeToSlim(uri.toString())
                        )
                    }

                    R.id.action_home_to_breast -> {
                        findNavController().navigate(
                            HomeFragmentDirections.actionHomeToBreast(uri.toString())
                        )
                    }

                    R.id.action_home_to_waist -> {
                        findNavController().navigate(
                            HomeFragmentDirections.actionHomeToWaist(uri.toString())
                        )
                    }

                    R.id.action_home_to_height -> {
                        findNavController().navigate(
                            HomeFragmentDirections.actionHomeToHeight(uri.toString())
                        )
                    }

                    R.id.action_home_to_belly -> {
                        findNavController().navigate(
                            HomeFragmentDirections.actionHomeToBelly(uri.toString())
                        )
                    }

                    R.id.action_home_to_hip -> {
                        findNavController().navigate(
                            HomeFragmentDirections.actionHomeToHip(uri.toString())
                        )
                    }
                }
            }
        } else {
            checkDeniedCount(deniedCount)
        }
    }

    private fun checkDeniedCount(deniedCount: Int) {
        when (deniedCount) {
            1 -> showPermissionRationaleDialog()
            2 -> showSettingsRedirectDialog()
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Required")
            .setMessage("This app needs storage permission to work properly. Please grant the permission.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                permissionLauncher.launch(helper.permission)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSettingsRedirectDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Permission Denied")
            .setMessage("You have denied permissions twice. To use this app, please grant permissions in app settings.")
            .setPositiveButton("Go to Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun checkPermission(): Boolean {
        return helper.permission.all { permission ->
            ContextCompat.checkSelfPermission(
                requireContext(),
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}