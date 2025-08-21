package com.bodyshapeeditor.slim_body_photo_editor.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentWaistBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class WaistFragment : Fragment() {

    private var _binding: FragmentWaistBinding? = null
    private val binding get() = _binding!!
    private val helper = PhotoEditorHelper()
    lateinit var ImageUri: Uri

    // Launch camera
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && ImageUri != null) {
            val bitmap = helper.fixImageOrientation(requireContext(), ImageUri)
            bitmap?.let {
                binding.waistStretchView.setImageBitmap(it)
                binding.waistStretchView.setStretchFactor(1.0f)
                binding.scaleSeekBar.progress = 50

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
        _binding = FragmentWaistBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageUriString = arguments?.getString("imageUri")
        imageUriString?.let {
            val imageUri = Uri.parse(it)
            val bitmap = helper.fixImageOrientation(requireContext(), imageUri)
            bitmap?.let { bmp ->
                binding.waistStretchView.setImageBitmap(bmp)
                binding.waistStretchView.setStretchFactor(1.0f)
                binding.scaleSeekBar.progress = 50
            } ?: Toast.makeText(requireContext(), "Image not found", Toast.LENGTH_SHORT).show()
        }


        setupScaleControls()


        binding.galleryButton.setOnClickListener {

            val action = WaistFragmentDirections.actionWaistFragmentToGalleryFragment(
                "waist",
                imageUriString!!
            )
            findNavController().navigate(action)

        }

        binding.btnTick.setOnClickListener {

            val bitmap = binding.waistStretchView.getStretchedBitmap()
            bitmap?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    val uri = helper.saveBitmapToGallery(it, requireContext())
                    withContext(Dispatchers.Main) {
                        if (uri != null) {
                            Toast.makeText(requireContext(), "Image saved!", Toast.LENGTH_SHORT)
                                .show()
                            requireActivity().showAdAndGo {
                                val action =
                                    WaistFragmentDirections.actionWaistFragmentToSavedImageStatus(
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

        binding.btnClose.setOnClickListener {
            findNavController().navigate(R.id.action_waist_to_home)
        }
        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_waist_to_home)
        }

        binding.cameraButton.setOnClickListener {
            ImageUri = helper.createImageUri(requireContext())
            cameraLauncher.launch(ImageUri)
        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_waist_to_home)
                }
            })
    }

    private fun setupScaleControls() {
        binding.scaleSeekBar.max = 100
        binding.scaleSeekBar.progress = 50
        binding.scaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val stretchFactor = 0.8f + (progress / 100f) * 0.4f
                binding.waistStretchView.setStretchFactor(stretchFactor)
                binding.waistStretchView.setBlurFactor(0.2f)
                binding.scaleValueText.text = when {
                    stretchFactor > 1.03f -> "Widen: +${"%.0f".format((stretchFactor - 1f) * 100)}%"
                    stretchFactor < 0.97f -> "Narrow: -${"%.0f".format((1f - stretchFactor) * 100)}%"
                    else -> "Normal"
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class WaistStretchImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // Drawing tools


    private val redpaint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        isAntiAlias = true
    }

    // Bitmap management
    private var originalBitmap: Bitmap? = null
    private var workingBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null

    // Effect parameters (now for horizontal stretching)
    private var dot1X = 0.3f
    private var dot1Y = 0.5f
    private var dot2X = 0.7f
    private var dot2Y = 0.5f
    private var effectRadius = 100f
    private var stretchFactor = 1.0f
    private var blurFactor = 0f

    // Mesh grid parameters
    private val meshWidth = 30
    private val meshHeight = 20
    private lateinit var meshVertices: FloatArray
    private var meshVerticesCount = 0

    // Touch handling
    private val handleRadius = 30f
    private var activeHandle: HandleType? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var verticalOffset = 0f

    // Thread management
    private val executor = Executors.newSingleThreadExecutor()

    private enum class HandleType { DOT1, DOT2 }

    override fun setImageBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        scaleBitmapToView()
        applyEffectAsync()
    }

    fun setStretchFactor(factor: Float) {
        stretchFactor = factor.coerceIn(0.2f, 2.0f)
        applyEffectAsync()
    }

    fun setBlurFactor(factor: Float) {
        blurFactor = factor.coerceIn(0f, 1f)
        applyEffectAsync()
    }

    fun setEffectRadius(radius: Float) {
        effectRadius = radius.coerceAtLeast(20f)
        applyEffectAsync()
    }

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
        val distortedVertices = meshVertices.copyOf()
        applyHorizontalMeshDistortion(bitmap, distortedVertices)
        canvas.drawBitmapMesh(bitmap, meshWidth, meshHeight, distortedVertices, 0, null, 0, null)
        return output
    }

    // In WaistStretchImageView class, modify the applyHorizontalMeshDistortion function:
    private fun applyHorizontalMeshDistortion(bitmap: Bitmap, vertices: FloatArray) {
        val width = bitmap.width
        val height = bitmap.height

        val centerX = (dot1X + dot2X) / 2 * width
        val centerY = (dot1Y + dot2Y) / 2 * height
        val lineLength = sqrt(
            (dot2X * width - dot1X * width).pow(2) +
                    (dot2Y * height - dot1Y * height).pow(2)
        )

        for (i in 0 until vertices.size step 2) {
            val x = vertices[i]
            val y = vertices[i + 1]

            // Calculate distance from the line segment
            val distanceToLine = pointToLineDistance(
                x, y,
                dot1X * width, dot1Y * height,
                dot2X * width, dot2Y * height
            )

            if (distanceToLine <= effectRadius) {
                // More gradual falloff curve for subtle effect
                val normalizedDistance = distanceToLine / effectRadius
                val falloff = (1 - normalizedDistance.pow(3f)).pow(2f)

                // Reduced scaling effect
                val scale = if (stretchFactor > 1) {
                    1 + (stretchFactor - 1) * falloff * 0.9f  // 0.9 it use for increasing the effect or decreasing
                } else {
                    1 - (1 - stretchFactor) * falloff * 0.9f // 0.9 it use for increasing the effect or decreasing
                }

                // Calculate horizontal displacement
                val dx = (x - centerX) * (scale - 1) * 0.8f // Reduced displacement

                // Apply the transformation
                vertices[i] = (x + dx).coerceIn(0f, width.toFloat())
            }
        }
    }

    private fun pointToLineDistance(
        x: Float,
        y: Float,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float
    ): Float {
        val A = x - x1
        val B = y - y1
        val C = x2 - x1
        val D = y2 - y1

        val dot = A * C + B * D
        val len_sq = C * C + D * D
        val param = if (len_sq != 0f) dot / len_sq else -1f

        val xx: Float
        val yy: Float

        if (param < 0) {
            xx = x1
            yy = y1
        } else if (param > 1) {
            xx = x2
            yy = y2
        } else {
            xx = x1 + param * C
            yy = y1 + param * D
        }

        return sqrt((x - xx).pow(2) + (y - yy).pow(2))
    }

    private fun applyBlurEffect(bitmap: Bitmap): Bitmap {
        if (blurFactor <= 0) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        // Create blurred version of the effect area
        val effectArea = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvasEffect = Canvas(effectArea)

        // Draw effect area mask
        val path = Path().apply {
            val x1 = dot1X * width
            val y1 = dot1Y * height
            val x2 = dot2X * width
            val y2 = dot2Y * height

            // Create a rounded rectangle around the line
            val angle = atan2(y2 - y1, x2 - x1)
            val perpendicularAngle = angle + Math.PI / 2

            val dx = cos(perpendicularAngle).toFloat() * effectRadius
            val dy = sin(perpendicularAngle).toFloat() * effectRadius

            moveTo(x1 - dx, y1 - dy)
            lineTo(x1 + dx, y1 + dy)
            lineTo(x2 + dx, y2 + dy)
            lineTo(x2 - dx, y2 - dy)
            close()
        }

        val paintEffect = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
        }
        canvasEffect.drawPath(path, paintEffect)
        paintEffect.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvasEffect.drawBitmap(bitmap, 0f, 0f, paintEffect)

        // Blur the effect area
        fastBlur(effectArea, (25 * blurFactor).toInt())

        // Combine with original
        val canvas = Canvas(output)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val paintBlend = Paint().apply {
            alpha = (blurFactor * 255).toInt()
        }
        canvas.drawBitmap(effectArea, 0f, 0f, paintBlend)

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
            drawControlElements(canvas, bitmap)  // Pass the bitmap parameter
        } ?: run {
            super.onDraw(canvas)
        }
    }

    private fun drawControlElements(canvas: Canvas, bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height

        val x1 = dot1X * width
        val y1 = dot1Y * height + verticalOffset
        val x2 = dot2X * width
        val y2 = dot2Y * height + verticalOffset

        // Draw first control element
        drawVerticalLineWithCurves(canvas, x1, y1)

        // Draw second control element
        drawVerticalLineWithCurves(canvas, x2, y2)
    }

    private fun drawVerticalLineWithCurves(canvas: Canvas, x: Float, y: Float) {
        val curveWidth = handleRadius * 0.8f
        val curveHeight = handleRadius * 1.5f

        // Draw vertical line through the center
        canvas.drawLine(
            x, y - curveHeight,
            x, y + curveHeight,
            redpaint
        )

        // Draw left curve (like a parenthesis)
        val leftCurve = Path().apply {
            moveTo(x - curveWidth / 2, y - curveHeight / 2)
            quadTo(
                x - curveWidth, y,  // Control point
                x - curveWidth / 2, y + curveHeight / 2  // End point
            )
        }
        canvas.drawPath(leftCurve, redpaint)

        // Draw right curve (mirror of left curve)
        val rightCurve = Path().apply {
            moveTo(x + curveWidth / 2, y - curveHeight / 2)
            quadTo(
                x + curveWidth, y,  // Control point
                x + curveWidth / 2, y + curveHeight / 2  // End point
            )
        }
        canvas.drawPath(rightCurve, redpaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        workingBitmap?.let { bitmap ->
            val width = bitmap.width
            val height = bitmap.height

            val x1 = dot1X * width
            val y1 = dot1Y * height + verticalOffset
            val x2 = dot2X * width
            val y2 = dot2Y * height + verticalOffset

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    activeHandle = when {
                        hypot(
                            event.x - x1,
                            event.y - y1
                        ) <= handleRadius + touchSlop -> HandleType.DOT1

                        hypot(
                            event.x - x2,
                            event.y - y2
                        ) <= handleRadius + touchSlop -> HandleType.DOT2

                        else -> null
                    }

                    if (activeHandle != null) {
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    when (activeHandle) {
                        HandleType.DOT1 -> {
                            dot1X = (event.x / width).coerceIn(0f, 1f)
                            dot1Y = ((event.y - verticalOffset) / height).coerceIn(0f, 1f)
                            invalidate()
                            return true
                        }

                        HandleType.DOT2 -> {
                            dot2X = (event.x / width).coerceIn(0f, 1f)
                            dot2Y = ((event.y - verticalOffset) / height).coerceIn(0f, 1f)
                            invalidate()
                            return true
                        }

                        else -> {}
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (activeHandle != null) {
                        applyEffectAsync()
                    }
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
