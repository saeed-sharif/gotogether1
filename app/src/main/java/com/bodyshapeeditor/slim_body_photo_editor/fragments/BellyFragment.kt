package com.bodyshapeeditor.slim_body_photo_editor.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.net.Uri
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bodyshapeeditor.slim_body_photo_editor.PhotoEditorHelper
import com.bodyshapeeditor.slim_body_photo_editor.R
import com.bodyshapeeditor.slim_body_photo_editor.ads.showAdAndGo
import com.bodyshapeeditor.slim_body_photo_editor.ads.showBanner
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentBellyBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class BellyFragment : Fragment() {
    private var _binding: FragmentBellyBinding? = null
    private val binding get() = _binding!!
    private lateinit var helper: PhotoEditorHelper
    lateinit var ImageUri: Uri


    // Launch camera
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && ImageUri != null) {
            val bitmap = helper.fixImageOrientation(requireContext(), ImageUri)
            bitmap?.let {
                binding.strechBelly.setImageBitmap(it)
                binding.strechBelly.setStretchFactor(1.0f)

            }

        } else {
            Toast.makeText(
                requireContext(),
                "Failed to capture image. Try again.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBellyBinding.inflate(inflater, container, false)
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
        helper = PhotoEditorHelper()

        // Load image from arguments if passed
        val imageUriString = arguments?.getString("imageUri")
        val imageUri = imageUriString?.let { Uri.parse(it) }

        if (imageUri != null) {
            val bitmap = helper.fixImageOrientation(requireContext(), imageUri)
            bitmap?.let {
                binding.strechBelly.setImageBitmap(it)
                binding.strechBelly.setStretchFactor(1.0f)
            }
        } else {
            Toast.makeText(requireContext(), "Image not found", Toast.LENGTH_SHORT).show()
        }
        setupScaleControls()

        binding.galleryButton.setOnClickListener {

            val action = BellyFragmentDirections.actionBellyFragmentToGalleryFragment(
                "belly",
                imageUri.toString()
            )
            findNavController().navigate(action)

        }
        binding.btnClose.setOnClickListener {
            findNavController().navigate(R.id.action_belly_to_home)
        }
        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_belly_to_home)
        }

        binding.cameraButton.setOnClickListener {
            ImageUri = helper.createImageUri(requireContext())
            cameraLauncher.launch(ImageUri)

        }

        binding.btnTick.setOnClickListener {


            val bitmap = binding.strechBelly.getStretchedBitmap()
            bitmap?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    val uri = helper.saveBitmapToGallery(it, requireContext())
                    withContext(Dispatchers.Main) {
                        if (uri != null) {

                            Toast.makeText(
                                requireContext(),
                                "Image saved!",
                                Toast.LENGTH_SHORT
                            ).show()
                            requireActivity().showAdAndGo {
                                val action =
                                    BellyFragmentDirections.actionBellyFragmentToSavedImageStatus(
                                        uri.toString()
                                    )
                                findNavController().navigate(action)
                            }
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Failed to save image.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }


                    }
                }

            }
        }


        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_belly_to_home)
                }
            })

    }


    private fun setupScaleControls() {
        binding.scaleSeekBar.max = 100
        binding.scaleSeekBar.progress = 50

        binding.scaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val stretchFactor = 0.7f + (progress / 100f) * 0.6f
                binding.strechBelly.setStretchFactor(stretchFactor)
                binding.strechBelly.setBlurFactor(0.2f)
                binding.scaleValueText.text = when {
                    stretchFactor > 1.05f -> "Fatten Belly: +${"%.0f".format((stretchFactor - 1f) * 100)}%"
                    stretchFactor < 0.95f -> "Slim Belly: -${"%.0f".format((1f - stretchFactor) * 100)}%"
                    else -> "Normal Belly"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        binding.scaleSeekBar.progress = 50
        binding.strechBelly.setStretchFactor(1.0f)
        binding.strechBelly.setBlurFactor(0.0f)
        binding.scaleValueText.text = "Normal Belly"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class FattenIBelly @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // Drawing tools
    private val circlePaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val handlePaint = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val whitePaint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 4f
        isAntiAlias = true
    }

    //Bitmap management
    private var originalBitmap: Bitmap? = null
    private var workingBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null

    // Effect parameters
    private var circleCenterX = 0.522f
    private var circleCenterY = 0.4f
    private var circleRadius = 100f
    private var stretchFactor = 1.0f
    private var blurFactor = 0f   //0 = no blur, 1 = max blur

    // Mesh grid parameters
    private val meshWidth = 20
    private val meshHeight = 20
    private lateinit var meshVertices: FloatArray
    private var meshVerticesCount = 0

    // Touch handling
    private val handleRadius = 20f
    private var activeHandle: HandleType? = null
    private var isDraggingCircle = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var verticalOffset = 0f

    // Thread management
    private val executor = Executors.newSingleThreadExecutor()
    private enum class HandleType { RIGHT }
    override fun setImageBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        scaleBitmapToView()
        applyEffectAsync()
    }

    fun setStretchFactor(factor: Float) {
        stretchFactor = factor.coerceIn(0f, 2f)
        applyEffectAsync()
    }

    fun getStretchFactor(): Float = stretchFactor

    fun setBlurFactor(factor: Float) {
        blurFactor = factor.coerceIn(0f, 1f)
        applyEffectAsync()
    }

    fun getBlurFactor(): Float = blurFactor

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scaleBitmapToView()
        applyEffectAsync()
    }

    private fun scaleBitmapToView() {
        originalBitmap?.let { bitmap ->
            if (width > 0 && height > 0) {
                // Maintain aspect ratio like StretchImageView
                val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                val scaledWidth = width
                val scaledHeight = (scaledWidth * aspectRatio).toInt()

                workingBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    scaledWidth,
                    scaledHeight,
                    true
                )
                initMeshGrid()
            }
        }
    }

    private fun initMeshGrid() {
        workingBitmap?.let { bitmap ->
            val width = bitmap.width
            val height = bitmap.height

            meshVerticesCount = (meshWidth + 1) * (meshHeight + 1) * 2
            meshVertices = FloatArray(meshVerticesCount)

            val xStep = width.toFloat() / meshWidth
            val yStep = height.toFloat() / meshHeight

            var index = 0
            for (y in 0..meshHeight) {
                val fy = y * yStep
                for (x in 0..meshWidth) {
                    meshVertices[index++] = x * xStep
                    meshVertices[index++] = fy
                }
            }
        }
    }

    private fun applyEffectAsync() {
        executor.execute {
            workingBitmap?.let { bitmap ->
                val stretched = applyStretchEffect(bitmap)
                val blurred = if (blurFactor > 0) applyBlurEffect(stretched) else stretched
                post {
                    resultBitmap?.recycle()
                    resultBitmap = blurred
                    invalidate()
                }
            }
        }
    }

    private fun applyStretchEffect(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        // Create a copy of the original vertices
        val distortedVertices = meshVertices.copyOf()

        // Apply distortion to the mesh
        applyMeshDistortion(bitmap, distortedVertices)

        // Draw the bitmap with the distorted mesh
        canvas.drawBitmapMesh(bitmap, meshWidth, meshHeight, distortedVertices, 0, null, 0, null)

        return output
    }

    private fun applyMeshDistortion(bitmap: Bitmap, vertices: FloatArray) {
        val centerX = circleCenterX * bitmap.width
        val centerY = circleCenterY * bitmap.height
        val radius = circleRadius
        val radiusSquared = radius * radius

        // Reduced strength calculation for more subtle effects
        val strength = (stretchFactor - 1.0f) * 1.2f // Reduced from 2.0f to 1.2f

        if (abs(strength) < 0.01f) return

        for (i in 0 until vertices.size step 2) {
            val x = vertices[i]
            val y = vertices[i + 1]

            val dx = x - centerX
            val dy = y - centerY
            val distanceSquared = dx * dx + dy * dy

            if (distanceSquared <= radiusSquared) {
                val distance = sqrt(distanceSquared)
                val normalizedDistance = distance / radius

                // Softer falloff curve
                val falloff = 1f - normalizedDistance.pow(3f) // Reduced from ^4 to ^3

                // Reduced effect strength
                val scale = 1.0f + (falloff * strength * 0.8f) // Reduced from 1.5f to 0.8f

                vertices[i] = (centerX + dx * scale).coerceIn(0f, bitmap.width.toFloat())
                vertices[i + 1] = (centerY + dy * scale).coerceIn(0f, bitmap.height.toFloat())
            }
        }
    }

    private fun applyBlurEffect(bitmap: Bitmap): Bitmap {
        if (blurFactor <= 0) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // 1. Create blurred version of JUST the circle area
        val circleArea = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvasCircle = Canvas(circleArea)

        // Draw only the circle area from original bitmap
        val paintCircle = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        canvasCircle.drawCircle(
            circleCenterX * width,
            circleCenterY * height,
            circleRadius,
            paintCircle
        )
        paintCircle.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvasCircle.drawBitmap(bitmap, 0f, 0f, paintCircle)

        // Blur just the circle area
        fastBlur(circleArea, (25 * blurFactor).toInt())

        // 2. Combine with original
        val canvas = Canvas(output)
        canvas.drawBitmap(bitmap, 0f, 0f, null) // Draw original

        // Draw blurred circle with transparency
        val paintBlend = Paint().apply {
            alpha = (blurFactor * 255).toInt()
        }
        canvas.drawBitmap(circleArea, 0f, 0f, paintBlend)

        return output
    }

    private fun fastBlur(bitmap: Bitmap, radius: Int): Bitmap {
        if (radius <= 0) return bitmap

        return try {
            val rs = RenderScript.create(context)
            val input = Allocation.createFromBitmap(rs, bitmap)
            val output = Allocation.createTyped(rs, input.type)

            ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)).apply {
                setRadius(radius.coerceAtMost(25).toFloat())
                setInput(input)
                forEach(output)
            }
            output.copyTo(bitmap)
            rs.destroy()
            bitmap
        } catch (e: Exception) {
            // Fallback: Simple blur by scaling down and up
            val scale = 1f / (1 + radius / 10f)
            val small = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
            Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
        }
    }

    override fun onDraw(canvas: Canvas) {
        resultBitmap?.let { bitmap ->
            // Calculate vertical offset to center the image
            verticalOffset = (height - bitmap.height) / 2f

            // Draw the bitmap centered vertically
            canvas.drawBitmap(bitmap, 0f, verticalOffset, null)
            drawControlElements(canvas, bitmap)
        } ?: run {
            super.onDraw(canvas)
        }
    }

    private fun drawControlElements(canvas: Canvas, bitmap: Bitmap) {
        val centerX = circleCenterX * bitmap.width
        val centerY = circleCenterY * bitmap.height + verticalOffset

        // Draw circle
        canvas.drawCircle(centerX, centerY, circleRadius, circlePaint)

        // Draw handle
        val handleX = centerX + circleRadius
        val handleY = centerY
        canvas.drawCircle(handleX, handleY, handleRadius, handlePaint)
        canvas.drawLine(
            handleX - handleRadius / 2, handleY,
            handleX + handleRadius / 2, handleY,
            whitePaint
        )
        canvas.drawLine(
            handleX, handleY - handleRadius / 2,
            handleX, handleY + handleRadius / 2,
            whitePaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        workingBitmap?.let { bitmap ->
            val centerX = circleCenterX * bitmap.width
            val centerY = circleCenterY * bitmap.height + verticalOffset
            val handleX = centerX + circleRadius
            val handleY = centerY

            // Adjust touch coordinates for vertical offset
            val adjustedY = event.y - verticalOffset

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Check if touching the handle first
                    activeHandle = if (hypot(
                            event.x - handleX,
                            event.y - handleY
                        ) <= handleRadius + touchSlop
                    ) {
                        HandleType.RIGHT
                    } else {
                        null
                    }

                    // If not touching handle, check if touching circle
                    if (activeHandle == null && hypot(
                            event.x - centerX,
                            event.y - centerY
                        ) <= circleRadius + touchSlop
                    ) {
                        isDraggingCircle = true
                    }

                    if (activeHandle != null || isDraggingCircle) {
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isDraggingCircle) {
                        // Update circle center position (normalized coordinates)
                        circleCenterX = (event.x / bitmap.width).coerceIn(0f, 1f)
                        circleCenterY =
                            ((event.y - verticalOffset) / bitmap.height).coerceIn(0f, 1f)
                        applyEffectAsync()
                        return true
                    } else if (activeHandle == HandleType.RIGHT) {
                        val newRadius =
                            (event.x - centerX).coerceIn(20f, min(bitmap.width, bitmap.height) / 2f)
                        if (abs(newRadius - circleRadius) > 2f) {
                            circleRadius = newRadius
                            applyEffectAsync()
                        }
                        return true
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isDraggingCircle || activeHandle != null) {
                        applyEffectAsync()
                    }
                    isDraggingCircle = false
                    activeHandle = null
                    parent.requestDisallowInterceptTouchEvent(false)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    fun getStretchedBitmap(): Bitmap? {
        return resultBitmap?.copy(resultBitmap?.config ?: Bitmap.Config.ARGB_8888, true)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        executor.shutdownNow()
        originalBitmap?.recycle()
        workingBitmap?.recycle()
        resultBitmap?.recycle()
    }
}