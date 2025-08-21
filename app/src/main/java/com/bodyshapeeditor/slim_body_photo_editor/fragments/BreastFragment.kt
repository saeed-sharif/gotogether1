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
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentBreastBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class BreastFragment : Fragment() {

    private var _binding: FragmentBreastBinding? = null
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
                binding.breastfatify.setImageBitmap(it)
                binding.breastfatify.setStretchFactor(1.0f)

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
        _binding = FragmentBreastBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        helper = PhotoEditorHelper()

        // Load image from arguments if passed
        val imageUriString = arguments?.getString("imageUri")
        val imageUri = imageUriString?.let { Uri.parse(it) }

        if (imageUri != null) {
            val bitmap = helper.fixImageOrientation(requireContext(), imageUri)
            bitmap?.let {
                binding.breastfatify.setImageBitmap(it)
                binding.breastfatify.setStretchFactor(1.0f)
            }
        } else {
            Toast.makeText(requireContext(), "Image not found", Toast.LENGTH_SHORT).show()
        }
        setupScaleControls()


        binding.btnClose.setOnClickListener {
            findNavController().navigate(R.id.action_breast_to_home)
        }
        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_breast_to_home)
        }
        binding.cameraButton.setOnClickListener {
            ImageUri = helper.createImageUri(requireContext())
            cameraLauncher.launch(ImageUri)

        }


        binding.galleryButton.setOnClickListener {
            val action = BreastFragmentDirections.actionBreastFragmentToGalleryFragment("breast", imageUri.toString())
            findNavController().navigate(action)

        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_breast_to_home)
                }
            })

        binding.btnTick.setOnClickListener {

                val bitmap = binding.breastfatify.getStretchedBitmap()
                bitmap?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val uri = helper.saveBitmapToGallery(it, requireContext())
                        withContext(Dispatchers.Main) {
                            if (uri != null) {
                                Toast.makeText(requireContext(), "Image saved!", Toast.LENGTH_SHORT)
                                    .show()
                                requireActivity().showAdAndGo {
                                    val action = BreastFragmentDirections.actionBreastFragmentToSavedImageStatus(uri.toString())
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
    }



    private fun setupScaleControls() {
        binding.scaleSeekBar.max = 100
        binding.scaleSeekBar.progress = 50

        binding.scaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val stretchFactor = 0.7f + (progress / 100f) * 0.6f
                binding.breastfatify.setStretchFactor(stretchFactor)
                binding.breastfatify.setBlurFactor(0.2f)
                binding.scaleValueText.text = when {
                    stretchFactor > 1.05f -> "Fatten Breast: +${"%.0f".format((stretchFactor - 1f) * 100)}%"
                    stretchFactor < 0.95f -> "Slim Breast: -${"%.0f".format((1f - stretchFactor) * 100)}%"
                    else -> "Normal Breast"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        binding.scaleSeekBar.progress = 50
        binding.breastfatify.setStretchFactor(1.0f)
        binding.breastfatify.setBlurFactor(0.0f)
        binding.scaleValueText.text = "Normal Breast"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class BreastFatify @JvmOverloads constructor(
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

    // Bitmap management
    private var originalBitmap: Bitmap? = null
    private var workingBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null

    // Effect parameters and you can adjust the hieght of the
    private val circle1 = CircleParams(0.419f, 0.31f, 60f)
    private val circle2 = CircleParams(0.6f, 0.31f, 60f)
    private var stretchFactor = 1.0f
    private var blurFactor = 0f // 0 = no blur, 1 = max blur

    // Mesh grid parameters
    private val meshWidth = 20
    private val meshHeight = 20
    private lateinit var meshVertices: FloatArray
    private var meshVerticesCount = 0

    // Touch handling
    private val handleRadius = 20f
    private var activeHandle: HandleType? = null
    private var activeCircle: CircleParams? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var verticalOffset = 0f

    // Thread management
    private val executor = Executors.newSingleThreadExecutor()

    private data class CircleParams(
        var centerX: Float,
        var centerY: Float,
        var radius: Float
    )

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

        // Apply distortion to the mesh for both circles
        applyMeshDistortion(bitmap, distortedVertices, circle1)
        applyMeshDistortion(bitmap, distortedVertices, circle2)

        // Draw the bitmap with the distorted mesh
        canvas.drawBitmapMesh(bitmap, meshWidth, meshHeight, distortedVertices, 0, null, 0, null)

        return output
    }

    private fun applyMeshDistortion(bitmap: Bitmap, vertices: FloatArray, circle: CircleParams) {
        val centerX = circle.centerX * bitmap.width
        val centerY = circle.centerY * bitmap.height
        val radius = circle.radius
        val radiusSquared = radius * radius

        // Reduced strength calculation for more subtle effects
        val strength = (stretchFactor - 1.0f) * 1.2f  // Reduced from 2.0f to 1.2f

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
                val falloff = 1f - normalizedDistance.pow(3f)  // Reduced from ^4 to ^3

                // Reduced effect strength
                val scale = 1.0f + (falloff * strength * 0.8f)  // Reduced from 1.5f to 0.8f

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

        // 1. Create blurred version of the circle areas
        val circleArea = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvasCircle = Canvas(circleArea)

        // Draw both circle areas from original bitmap
        val paintCircle = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }

        // Draw first circle
        canvasCircle.drawCircle(
            circle1.centerX * width,
            circle1.centerY * height,
            circle1.radius,
            paintCircle
        )

        // Draw second circle
        canvasCircle.drawCircle(
            circle2.centerX * width,
            circle2.centerY * height,
            circle2.radius,
            paintCircle
        )

        paintCircle.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvasCircle.drawBitmap(bitmap, 0f, 0f, paintCircle)

        // Blur the circle areas
        fastBlur(circleArea, (25 * blurFactor).toInt())

        // 2. Combine with original
        val canvas = Canvas(output)
        canvas.drawBitmap(bitmap, 0f, 0f, null) // Draw original

        // Draw blurred circles with transparency
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
            verticalOffset = (height - bitmap.height) / 2f
            canvas.drawBitmap(bitmap, 0f, verticalOffset, null)
            drawControlElements(canvas, bitmap, circle1)
            drawControlElements(canvas, bitmap, circle2)
        } ?: run {
            super.onDraw(canvas)
        }
    }

    private fun drawControlElements(canvas: Canvas, bitmap: Bitmap, circle: CircleParams) {
        val centerX = circle.centerX * bitmap.width
        val centerY = circle.centerY * bitmap.height + verticalOffset

        // Draw circle
        canvas.drawCircle(centerX, centerY, circle.radius, circlePaint)

        // Draw handle - left side for circle1, right side for circle2
        val isCircle1 = circle === circle1
        val handleX = if (isCircle1) centerX - circle.radius else centerX + circle.radius
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
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Check which circle (if any) is being touched
                    activeCircle = findTouchedCircle(event, bitmap)

                    if (activeCircle != null) {
                        val centerX = activeCircle!!.centerX * bitmap.width
                        val centerY = activeCircle!!.centerY * bitmap.height + verticalOffset
                        val isCircle1 = activeCircle === circle1
                        val handleX =
                            if (isCircle1) centerX - activeCircle!!.radius else centerX + activeCircle!!.radius
                        val handleY = centerY

                        activeHandle = if (hypot(
                                event.x - handleX,
                                event.y - handleY
                            ) <= handleRadius + touchSlop
                        ) {
                            HandleType.RIGHT
                        } else {
                            null
                        }

                        if (activeHandle == null && hypot(
                                event.x - centerX,
                                event.y - centerY
                            ) <= activeCircle!!.radius + touchSlop
                        ) {
                            // Just dragging the circle, not the handle
                        } else if (activeHandle == null) {
                            activeCircle = null
                        }

                        if (activeCircle != null) {
                            parent.requestDisallowInterceptTouchEvent(true)
                            return true
                        } else {

                        }
                    } else {

                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    activeCircle?.let { circle ->
                        if (activeHandle == HandleType.RIGHT) {
                            val centerX = circle.centerX * bitmap.width
                            val isCircle1 = circle === circle1
                            val newRadius = if (isCircle1) {
                                (centerX - event.x).coerceIn(
                                    20f,
                                    min(bitmap.width, bitmap.height) / 2f
                                )
                            } else {
                                (event.x - centerX).coerceIn(
                                    20f,
                                    min(bitmap.width, bitmap.height) / 2f
                                )
                            }
                            if (abs(newRadius - circle.radius) > 2f) {
                                circle.radius = newRadius
                                invalidate()
                            }
                        } else {
                            // Moving the circle center
                            circle.centerX = (event.x / bitmap.width).coerceIn(0f, 1f)
                            circle.centerY =
                                ((event.y - verticalOffset) / bitmap.height).coerceIn(0f, 1f)
                            invalidate()
                        }
                        return true
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (activeCircle != null) {
                        applyEffectAsync()
                    }
                    activeCircle = null
                    activeHandle = null
                    parent.requestDisallowInterceptTouchEvent(false)
                    return true
                }

                else -> {}
            }
        }
        return super.onTouchEvent(event)
    }


    private fun findTouchedCircle(event: MotionEvent, bitmap: Bitmap): CircleParams? {
        // Check circle1 first
        val center1X = circle1.centerX * bitmap.width
        val center1Y = circle1.centerY * bitmap.height + verticalOffset
        val handle1X = center1X - circle1.radius  // Changed to left side

        if (hypot(event.x - handle1X, event.y - center1Y) <= handleRadius + touchSlop ||
            hypot(event.x - center1X, event.y - center1Y) <= circle1.radius + touchSlop
        ) {
            return circle1
        }

        // Check circle2 (remains on right side)
        val center2X = circle2.centerX * bitmap.width
        val center2Y = circle2.centerY * bitmap.height + verticalOffset
        val handle2X = center2X + circle2.radius

        if (hypot(event.x - handle2X, event.y - center2Y) <= handleRadius + touchSlop ||
            hypot(event.x - center2X, event.y - center2Y) <= circle2.radius + touchSlop
        ) {
            return circle2
        }

        return null
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