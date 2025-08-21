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
import android.util.Log
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
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentSlimBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SlimFragment : Fragment() {
    private var _binding: FragmentSlimBinding? = null
    private val binding get() = _binding!!
    private val helper = PhotoEditorHelper()
    lateinit var ImageUri: Uri

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && ImageUri != null) {
            // Clear any existing processing
            binding.stretchView.cleanupWorkingBitmaps()

            val bitmap = helper.fixImageOrientation(requireContext(), ImageUri)
            bitmap?.let {
                // Ensure we're on UI thread when setting the bitmap
                binding.stretchView.post {
                    binding.stretchView.setImageBitmap(it)
                }
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSlimBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("ImageUriGallery", "Image URI: ${arguments?.getString("imageUri")}")
        // Get imageUri from arguments
        ImageUri = arguments?.getString("imageUri").let {
            Uri.parse(it)
        }
        if (ImageUri != null) {
            val bitmap = helper.fixImageOrientation(requireContext(), ImageUri)
            binding.stretchView.setImageBitmap(bitmap!!)

            /*
             binding.stretchView.setLinesRatio(0.3f, 0.6f)
            */

        } else {
            Toast.makeText(requireContext(), "Image not found", Toast.LENGTH_SHORT).show()
        }

        setupClickListeners()
        setupScaleControls()
        setupBackPressHandler()
    }


    private fun setupClickListeners() {

        binding.btnTick.setOnClickListener {

/*
            val bitmap = binding.stretchView.getStretchedBitmapWithoutLines()

*/
            val bitmap = binding.stretchView.getStretchedBitmap()

            bitmap?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    val uri = helper.saveBitmapToGallery(it, requireContext())
                    withContext(Dispatchers.Main) {
                        if (uri != null) {
                            Toast.makeText(requireContext(), "Image saved!", Toast.LENGTH_SHORT)
                                .show()
                            requireActivity().showAdAndGo {
                                val action =
                                    SlimFragmentDirections.actionSlimFragmentToSavedImageStatus(uri.toString())
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
            // Navigate back to previous fragment
            findNavController().navigate(R.id.action_slim_to_home)
        }
        binding.backBtn.setOnClickListener {
            // Navigate back to previous fragment
            findNavController().navigate(R.id.action_slim_to_home)
        }
        binding.galleryButton.setOnClickListener {

            val action = SlimFragmentDirections.actionSlimFragmentToGalleryFragment(
                    "slim",
                    imageUri = ImageUri.toString()
                )
            findNavController().navigate(action)

        }

        binding.cameraButton.setOnClickListener {
            ImageUri = helper.createImageUri(requireContext())
            cameraLauncher.launch(ImageUri)

        }

        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.action_slim_to_home)
                }
            })



    }

    private fun setupScaleControls() {
        binding.scaleSeekBar.max = 100
        binding.scaleSeekBar.progress = 50
        binding.scaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Inverted calculation:
                // - Progress 0: widest (1.2f)
                // - Progress 50: normal (1.0f)
                // - Progress 100: narrowest (0.8f)
                val stretchFactor = when {
                    progress < 50 -> 1.2f - (progress / 50f) * 0.2f
                    else -> 1.0f - ((progress - 50) / 50f) * 0.2f
                }
                binding.stretchView.setStretchFactor(stretchFactor)
                binding.stretchView.setBlurFactor(0.2f)
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

    private fun setupBackPressHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Handle back press with navigation component
                    findNavController().navigate(R.id.action_slim_to_home)
                }
            })
    }

    override fun onResume() {
        super.onResume()
        // Reset to normal state (progress 50 = 1.0f stretch factor)
        binding.scaleSeekBar.progress = 50
        binding.stretchView.setStretchFactor(1f)
        binding.scaleValueText.text = "Normal"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class SlimBody @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    private val redPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val bluePaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val greenPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val yellowPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }
    private val centerDotPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4f
        style = Paint.Style.FILL_AND_STROKE
        isAntiAlias = true
    }
    private val fillPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Bitmap management
    private var originalBitmap: Bitmap? = null
    private var workingBitmap: Bitmap? = null
    private var resultBitmap: Bitmap? = null

    // Thread control
    @Volatile
    private var shouldContinueProcessing = true
    private val executor = Executors.newSingleThreadExecutor()
    private var currentTask: Future<*>? = null

    // Control parameters
    private var centerDotX = 0.5f
    private var centerDotY = 0.4f
    private var centerDotSize = 1.0f
    private val leftLineXOffset = 0.1f
    private val rightLineXOffset = 0.1f
    private var topLineYOffset = 0.1f
    private var bottomLineYOffset = 0.1f
    private var leftMiddleYOffset = 0.0f
    private var rightMiddleYOffset = 0.0f
    private var effectRadius = 100f
    private var stretchFactor = 1.0f
    private var blurFactor = 0f
    private val meshWidth = 30
    private val meshHeight = 20
    private lateinit var meshVertices: FloatArray
    private var meshVerticesCount = 0
    private val handleRadius = 30f
    private var isMovingCenter = false
    private var isMovingTopCenter = false
    private var isMovingBottomCenter = false
    private var isMovingLeftMiddle = false
    private var isMovingRightMiddle = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var verticalOffset = 0f
    private val arrowSize = 20f
    private val curveHeightFactor = 0.3f
    private val centerDotRadius = 15f

    // Dot position calculations remain the same
    private fun getDot1(): Pair<Float, Float> {
        return Pair(
            centerDotX - leftLineXOffset * centerDotSize,
            centerDotY - topLineYOffset * centerDotSize
        )
    }

    private fun getDot2(): Pair<Float, Float> {
        return Pair(
            centerDotX + rightLineXOffset * centerDotSize,
            centerDotY - topLineYOffset * centerDotSize
        )
    }

    private fun getDot3(): Pair<Float, Float> {
        return Pair(
            centerDotX - leftLineXOffset * centerDotSize,
            centerDotY + bottomLineYOffset * centerDotSize
        )
    }

    private fun getDot4(): Pair<Float, Float> {
        return Pair(
            centerDotX + rightLineXOffset * centerDotSize,
            centerDotY + bottomLineYOffset * centerDotSize
        )
    }

    private fun getDot5(): Pair<Float, Float> {
        return Pair(
            centerDotX - leftLineXOffset * centerDotSize,
            centerDotY + leftMiddleYOffset * centerDotSize
        )
    }

    private fun getDot6(): Pair<Float, Float> {
        return Pair(
            centerDotX + rightLineXOffset * centerDotSize,
            centerDotY + rightMiddleYOffset * centerDotSize
        )
    }

    private fun getTopCenter(): Pair<Float, Float> {
        val (dot1X, dot1Y) = getDot1()
        val (dot2X, dot2Y) = getDot2()
        return Pair((dot1X + dot2X) / 2, (dot1Y + dot2Y) / 2)
    }

    private fun getBottomCenter(): Pair<Float, Float> {
        val (dot3X, dot3Y) = getDot3()
        val (dot4X, dot4Y) = getDot4()
        return Pair((dot3X + dot4X) / 2, (dot3Y + dot4Y) / 2)
    }

    override fun setImageBitmap(bitmap: Bitmap) {
        // Ensure we're on the UI thread
        post {
            cleanupWorkingBitmaps()
            originalBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            scaleBitmapToView()
            applyEffectAsync()
        }
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
            if (width > 0 && height > 0 && !bitmap.isRecycled) {
                val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
                val scaledWidth = width
                val scaledHeight = (scaledWidth * aspectRatio).toInt()

                cleanupWorkingBitmaps()
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
            if (bitmap.isRecycled) return

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
        shouldContinueProcessing = true
        currentTask?.cancel(true)

        // Get local references to avoid race conditions
        val bitmapToProcess = workingBitmap?.takeUnless { it.isRecycled } ?: return

        currentTask = executor.submit {
            try {
                if (!shouldContinueProcessing || bitmapToProcess.isRecycled) return@submit

                val stretched = applyStretchEffect(bitmapToProcess)
                if (!shouldContinueProcessing || stretched.isRecycled) return@submit

                val blurred = if (blurFactor > 0) applyBlurEffect(stretched) else stretched
                if (!shouldContinueProcessing || blurred.isRecycled) return@submit

                post {
                    if (shouldContinueProcessing) {
                        // Only update if we're still valid
                        resultBitmap?.takeUnless { it.isRecycled }?.recycle()
                        resultBitmap = blurred
                        invalidate()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun applyStretchEffect(bitmap: Bitmap): Bitmap {
        if (bitmap.isRecycled || !shouldContinueProcessing) {
            return bitmap
        }

        return try {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val distortedVertices = meshVertices.copyOf()
            applyHorizontalMeshDistortion(bitmap, distortedVertices)
            canvas.drawBitmapMesh(bitmap, meshWidth, meshHeight, distortedVertices, 0, null, 0, null)
            output
        } catch (e: Exception) {
            bitmap
        }
    }

    private fun applyHorizontalMeshDistortion(bitmap: Bitmap, vertices: FloatArray) {
        if (bitmap.isRecycled || !shouldContinueProcessing) return

        val width = bitmap.width
        val height = bitmap.height

        val (dot1X, dot1Y) = getDot1()
        val (dot2X, dot2Y) = getDot2()
        val (dot3X, dot3Y) = getDot3()
        val (dot4X, dot4Y) = getDot4()
        val (dot5X, dot5Y) = getDot5()
        val (dot6X, dot6Y) = getDot6()

        applyDistortionForLinePair(
            vertices,
            dot1X * width, dot1Y * height,
            dot2X * width, dot2Y * height,
            width, height
        )

        applyDistortionForLinePair(
            vertices,
            dot3X * width, dot3Y * height,
            dot4X * width, dot4Y * height,
            width, height
        )

        applyDistortionForLinePair(
            vertices,
            dot5X * width, dot5Y * height,
            dot6X * width, dot6Y * height,
            width, height
        )
    }

    private fun applyDistortionForLinePair(
        vertices: FloatArray,
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        width: Int, height: Int
    ) {
        val centerX = (x1 + x2) / 2
        val centerY = (y1 + y2) / 2

        for (i in 0 until vertices.size step 2) {
            if (!shouldContinueProcessing) return

            val x = vertices[i]
            val y = vertices[i + 1]

            val distanceToLine = pointToLineDistance(x, y, x1, y1, x2, y2)

            if (distanceToLine <= effectRadius) {
                val normalizedDistance = distanceToLine / effectRadius
                val falloff = (1 - normalizedDistance.pow(3f)).pow(2f)
                val scale = stretchFactor * falloff * 0.9f + (1 - falloff) * 1.0f
                val dx = (x - centerX) * (scale - 1) * 0.8f
                vertices[i] = (x + dx).coerceIn(0f, width.toFloat())
            }
        }
    }

    private fun pointToLineDistance(
        x: Float, y: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
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
        if (bitmap.isRecycled || !shouldContinueProcessing) return bitmap

        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val effectArea = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvasEffect = Canvas(effectArea)

        val (dot1X, dot1Y) = getDot1()
        val (dot2X, dot2Y) = getDot2()
        val (dot3X, dot3Y) = getDot3()
        val (dot4X, dot4Y) = getDot4()
        val (dot5X, dot5Y) = getDot5()
        val (dot6X, dot6Y) = getDot6()

        drawEffectAreaMask(canvasEffect, width, height, dot1X, dot1Y, dot5X, dot5Y)
        drawEffectAreaMask(canvasEffect, width, height, dot5X, dot5Y, dot3X, dot3Y)
        drawEffectAreaMask(canvasEffect, width, height, dot2X, dot2Y, dot6X, dot6Y)
        drawEffectAreaMask(canvasEffect, width, height, dot6X, dot6Y, dot4X, dot4Y)
        drawEffectAreaMask(canvasEffect, width, height, dot5X, dot5Y, dot6X, dot6Y)

        fastBlur(effectArea, (25 * blurFactor).toInt())

        val canvas = Canvas(output)
        canvas.drawBitmap(bitmap, 0f, 0f, null)
        val paintBlend = Paint().apply {
            alpha = (blurFactor * 255).toInt()
        }
        canvas.drawBitmap(effectArea, 0f, 0f, paintBlend)

        effectArea.recycle()
        return output
    }

    private fun drawEffectAreaMask(
        canvas: Canvas,
        width: Int,
        height: Int,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ) {
        workingBitmap?.let { bitmap ->
            if (bitmap.isRecycled || !shouldContinueProcessing) return

            val path = Path().apply {
                val px1 = x1 * width
                val py1 = y1 * height
                val px2 = x2 * width
                val py2 = y2 * height

                val angle = atan2(py2 - py1, px2 - px1)
                val perpendicularAngle = angle + Math.PI / 2

                val dx = cos(perpendicularAngle).toFloat() * effectRadius
                val dy = sin(perpendicularAngle).toFloat() * effectRadius

                moveTo(px1 - dx, py1 - dy)
                lineTo(px1 + dx, py1 + dy)
                lineTo(px2 + dx, py2 + dy)
                lineTo(px2 - dx, py2 - dy)
                close()
            }

            val paintEffect = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC)
            }
            canvas.drawPath(path, paintEffect)
            paintEffect.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, 0f, 0f, paintEffect)
        }
    }

    private fun fastBlur(bitmap: Bitmap, radius: Int): Bitmap {
        if (bitmap.isRecycled || !shouldContinueProcessing || radius <= 0) return bitmap

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



    private fun drawControlElements(canvas: Canvas, bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height

        // Get current dot positions
        val (dot1X, dot1Y) = getDot1()
        val (dot2X, dot2Y) = getDot2()
        val (dot3X, dot3Y) = getDot3()
        val (dot4X, dot4Y) = getDot4()
        val (dot5X, dot5Y) = getDot5()
        val (dot6X, dot6Y) = getDot6()
        val (topCenterX, topCenterY) = getTopCenter()
        val (bottomCenterX, bottomCenterY) = getBottomCenter()

        // Draw center control dot
        canvas.drawCircle(
            centerDotX * width,
            centerDotY * height + verticalOffset,
            centerDotRadius,
            centerDotPaint
        )

        // Draw top center control dot
        canvas.drawCircle(
            topCenterX * width,
            topCenterY * height + verticalOffset,
            centerDotRadius,
            redPaint.apply { style = Paint.Style.FILL }
        )

        // Draw bottom center control dot
        canvas.drawCircle(
            bottomCenterX * width,
            bottomCenterY * height + verticalOffset,
            centerDotRadius,
            bluePaint.apply { style = Paint.Style.FILL }
        )

        // Draw left middle control dot
        canvas.drawCircle(
            dot5X * width,
            dot5Y * height + verticalOffset,
            centerDotRadius,
            yellowPaint.apply { style = Paint.Style.FILL }
        )

        // Draw right middle control dot
        canvas.drawCircle(
            dot6X * width,
            dot6Y * height + verticalOffset,
            centerDotRadius,
            yellowPaint.apply { style = Paint.Style.FILL }
        )

        // Draw left line (dot1 to dot5 to dot3)
        canvas.drawLine(
            dot1X * width, dot1Y * height + verticalOffset,
            dot5X * width, dot5Y * height + verticalOffset,
            redPaint
        )
        canvas.drawLine(
            dot5X * width, dot5Y * height + verticalOffset,
            dot3X * width, dot3Y * height + verticalOffset,
            redPaint
        )

        // Draw right line (dot2 to dot6 to dot4)
        canvas.drawLine(
            dot2X * width, dot2Y * height + verticalOffset,
            dot6X * width, dot6Y * height + verticalOffset,
            bluePaint
        )
        canvas.drawLine(
            dot6X * width, dot6Y * height + verticalOffset,
            dot4X * width, dot4Y * height + verticalOffset,
            bluePaint
        )

        // Draw center line (top center to bottom center)
        canvas.drawLine(
            topCenterX * width, topCenterY * height + verticalOffset,
            bottomCenterX * width, bottomCenterY * height + verticalOffset,
            greenPaint
        )

        // Draw middle line (dot5 to dot6)
        canvas.drawLine(
            dot5X * width, dot5Y * height + verticalOffset,
            dot6X * width, dot6Y * height + verticalOffset,
            yellowPaint
        )

        // Draw arrows on center line
        drawArrowOnLine(canvas, topCenterX * width, topCenterY * height + verticalOffset, true)
        drawArrowOnLine(
            canvas,
            bottomCenterX * width,
            bottomCenterY * height + verticalOffset,
            false
        )

        // Draw dots with arrows inside circles
        drawArrowInCircle(canvas, dot1X * width, dot1Y * height + verticalOffset, true, redPaint)
        drawArrowInCircle(canvas, dot2X * width, dot2Y * height + verticalOffset, true, redPaint)
        drawArrowInCircle(canvas, dot3X * width, dot3Y * height + verticalOffset, false, bluePaint)
        drawArrowInCircle(canvas, dot4X * width, dot4Y * height + verticalOffset, false, bluePaint)
        drawArrowInCircle(
            canvas,
            dot5X * width,
            dot5Y * height + verticalOffset,
            false,
            yellowPaint
        )
        drawArrowInCircle(
            canvas,
            dot6X * width,
            dot6Y * height + verticalOffset,
            false,
            yellowPaint
        )
    }

    private fun drawArrowOnLine(canvas: Canvas, x: Float, y: Float, isUpArrow: Boolean) {
        val arrowSize = 12f
        val circleRadius = 8f

        // Draw small circle
        canvas.drawCircle(x, y, circleRadius, greenPaint)

        // Draw arrow
        val path = Path()
        if (isUpArrow) {
            // Up arrow
            path.moveTo(x - arrowSize, y + arrowSize)
            path.lineTo(x, y - arrowSize / 2)
            path.lineTo(x + arrowSize, y + arrowSize)
        } else {
            // Down arrow
            path.moveTo(x - arrowSize, y - arrowSize)
            path.lineTo(x, y + arrowSize / 2)
            path.lineTo(x + arrowSize, y - arrowSize)
        }

        // Save current paint style
        val oldStyle = greenPaint.style

        // Draw filled arrow
        greenPaint.style = Paint.Style.FILL
        greenPaint.color = Color.WHITE
        canvas.drawPath(path, greenPaint)

        // Draw arrow outline
        greenPaint.style = Paint.Style.STROKE
        greenPaint.color = Color.GREEN
        canvas.drawPath(path, greenPaint)

        // Restore paint style
        greenPaint.style = oldStyle
    }

    private fun drawArrowInCircle(
        canvas: Canvas,
        x: Float,
        y: Float,
        isUpArrow: Boolean,
        paint: Paint
    ) {
        val circleRadius = 15f
        val arrowSize = 10f

        // Save current paint style
        val oldStyle = paint.style
        val oldColor = paint.color

        // Draw the circle
        paint.style = Paint.Style.FILL
        canvas.drawCircle(x, y, circleRadius, paint)

        // Draw the arrow
        if (isUpArrow) {
            // Up arrow
            val path = Path().apply {
                moveTo(x - arrowSize, y + arrowSize / 2)
                lineTo(x, y - arrowSize / 2)
                lineTo(x + arrowSize, y + arrowSize / 2)
                close()
            }
            canvas.drawPath(path, fillPaint.apply { color = Color.WHITE })
        } else {
            // Down arrow
            val path = Path().apply {
                moveTo(x - arrowSize, y - arrowSize / 2)
                lineTo(x, y + arrowSize / 2)
                lineTo(x + arrowSize, y - arrowSize / 2)
                close()
            }
            canvas.drawPath(path, fillPaint.apply { color = Color.WHITE })
        }

        // Draw circle border
        paint.style = Paint.Style.STROKE
        paint.color = when (paint) {
            redPaint -> Color.RED
            bluePaint -> Color.BLUE
            yellowPaint -> Color.YELLOW
            else -> oldColor
        }
        canvas.drawCircle(x, y, circleRadius, paint)

        // Restore paint style
        paint.style = oldStyle
        paint.color = oldColor
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        workingBitmap?.let { bitmap ->
            val width = bitmap.width
            val height = bitmap.height

            val centerX = centerDotX * width
            val centerY = centerDotY * height + verticalOffset
            val (topCenterX, topCenterY) = getTopCenter()
            val (bottomCenterX, bottomCenterY) = getBottomCenter()
            val (dot5X, dot5Y) = getDot5()
            val (dot6X, dot6Y) = getDot6()

            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // Check if touch is within center dot
                    val distanceToCenter = sqrt(
                        (event.x - centerX).pow(2) +
                                (event.y - centerY).pow(2)
                    )
                    if (distanceToCenter <= centerDotRadius * 2) {
                        isMovingCenter = true
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }

                    // Check if touch is within top center dot
                    val distanceToTopCenter = sqrt(
                        (event.x - topCenterX * width).pow(2) +
                                (event.y - (topCenterY * height + verticalOffset)).pow(2)
                    )
                    if (distanceToTopCenter <= centerDotRadius * 2) {
                        isMovingTopCenter = true
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }

                    // Check if touch is within bottom center dot
                    val distanceToBottomCenter = sqrt(
                        (event.x - bottomCenterX * width).pow(2) +
                                (event.y - (bottomCenterY * height + verticalOffset)).pow(2)
                    )
                    if (distanceToBottomCenter <= centerDotRadius * 2) {
                        isMovingBottomCenter = true
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }

                    // Check if touch is within left middle dot
                    val distanceToLeftMiddle = sqrt(
                        (event.x - dot5X * width).pow(2) +
                                (event.y - (dot5Y * height + verticalOffset)).pow(2)
                    )
                    if (distanceToLeftMiddle <= centerDotRadius * 2) {
                        isMovingLeftMiddle = true
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }

                    // Check if touch is within right middle dot
                    val distanceToRightMiddle = sqrt(
                        (event.x - dot6X * width).pow(2) +
                                (event.y - (dot6Y * height + verticalOffset)).pow(2)
                    )
                    if (distanceToRightMiddle <= centerDotRadius * 2) {
                        isMovingRightMiddle = true
                        parent.requestDisallowInterceptTouchEvent(true)
                        return true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isMovingCenter) {
                        centerDotX = (event.x / width).coerceIn(0f, 1f)
                        centerDotY = ((event.y - verticalOffset) / height).coerceIn(0f, 1f)
                        invalidate()
                        return true
                    } else if (isMovingTopCenter) {
                        // Adjust top line Y position (dot1 and dot2)
                        val newY = ((event.y - verticalOffset) / height).coerceIn(0f, centerDotY)
                        topLineYOffset = (centerDotY - newY).coerceIn(0.05f, 0.3f)
                        invalidate()
                        return true
                    } else if (isMovingBottomCenter) {
                        // Adjust bottom line Y position (dot3 and dot4)
                        val newY = ((event.y - verticalOffset) / height).coerceIn(centerDotY, 1f)
                        bottomLineYOffset = (newY - centerDotY).coerceIn(0.05f, 0.3f)
                        invalidate()
                        return true
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (isMovingCenter || isMovingTopCenter || isMovingBottomCenter ||
                        isMovingLeftMiddle || isMovingRightMiddle
                    ) {
                        applyEffectAsync()
                        isMovingCenter = false
                        isMovingTopCenter = false
                        isMovingBottomCenter = false
                        isMovingLeftMiddle = false
                        isMovingRightMiddle = false
                        parent.requestDisallowInterceptTouchEvent(false)
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    @Synchronized
    fun getStretchedBitmap(): Bitmap? {
        return resultBitmap?.takeUnless { it.isRecycled }?.copy(resultBitmap?.config ?: Bitmap.Config.ARGB_8888, true)
    }

    @Synchronized
    override fun onDraw(canvas: Canvas) {
        val bitmapToDraw = resultBitmap?.takeUnless { it.isRecycled }
        if (bitmapToDraw != null) {
            verticalOffset = (height - bitmapToDraw.height) / 2f
            canvas.drawBitmap(bitmapToDraw, 0f, verticalOffset, null)
            drawControlElements(canvas, bitmapToDraw)
        } else {
            super.onDraw(canvas)
        }
    }

    fun cleanupBitmaps() {
        originalBitmap?.takeUnless { it.isRecycled }?.recycle()
        cleanupWorkingBitmaps()
    }

    fun cleanupWorkingBitmaps() {
        // Only recycle if we're sure they're not being used
        val oldWorking = workingBitmap
        workingBitmap = null
        val oldResult = resultBitmap
        resultBitmap = null

        // Post the recycling to ensure they're not being drawn
        post {
            oldWorking?.takeUnless { it.isRecycled }?.recycle()
            oldResult?.takeUnless { it.isRecycled }?.recycle()
        }
    }

    fun cleanup() {
        shouldContinueProcessing = false
        currentTask?.cancel(true)
        executor.shutdownNow()
        cleanupBitmaps()
    }

    override fun onDetachedFromWindow() {
        cleanup()
        super.onDetachedFromWindow()
    }
}


/*
class StretchWidthImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    // Paint objects for drawing
    private val linePaint = Paint().apply {
        color = Color.RED
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val arrowPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 3f
        isAntiAlias = true
    }

    // Bitmap handling
    private var originalBitmap: Bitmap? = null
    private var scaledBitmap: Bitmap? = null

    // Line position and scaling (now for X-axis)
    private var lineX1Ratio = 0.3f
    private var lineX2Ratio = 0.6f
    private var scaleFactor = 1.0f

    // Touch handling
    private var isDraggingLine1 = false
    private var isDraggingLine2 = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var horizontalOffset = 0f
    private var lastTouchX = 0f
    private val minLineDistanceRatio = 0.06f // 2% minimum distance

    // Visual elements
    private val arrowSize = 40f
    private val arrowPadding = 10f
    private val edgeThreshold = 0.2f
    private val widthThreshold = 0.5f // 50% threshold for touch area adjustment
    private val indicatorRadius = 15f

    init {
        // Enable touch events
        isClickable = true
        isFocusable = true
    }

    override fun setImageBitmap(bitmap: Bitmap) {
        originalBitmap = bitmap
        scaleBitmapToView()
    }

    fun setLinesRatio(x1Ratio: Float, x2Ratio: Float) {
        // Ensure lines maintain minimum distance (6% of width)
        if (abs(x2Ratio - x1Ratio) < minLineDistanceRatio) {
            // Adjust the moving line to maintain distance
            if (x1Ratio != lineX1Ratio) { // lineX1 is being moved
                lineX1Ratio = x1Ratio.coerceIn(0f, lineX2Ratio - minLineDistanceRatio)
            } else { // lineX2 is being moved
                lineX2Ratio = x2Ratio.coerceIn(lineX1Ratio + minLineDistanceRatio, 1f)
            }
        } else {
            lineX1Ratio = x1Ratio.coerceIn(0f, lineX2Ratio - minLineDistanceRatio)
            lineX2Ratio = x2Ratio.coerceIn(lineX1Ratio + minLineDistanceRatio, 1f)
        }
        invalidate()
    }

    fun getLinesRatio(): Pair<Float, Float> = Pair(lineX1Ratio, lineX2Ratio)

    fun setScaleFactor(scale: Float) {
        scaleFactor = scale.coerceAtLeast(0.1f)
        invalidate()
    }

    fun getScaleFactor(): Float = scaleFactor

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        scaleBitmapToView()
    }

    private fun scaleBitmapToView() {
        originalBitmap?.let { bitmap ->
            val viewHeight = height
            if (viewHeight > 0) {
                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val scaledWidth = (viewHeight * aspectRatio).toInt()
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, viewHeight, true)
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        scaledBitmap?.let { bitmap ->
            val originalLineX1 = lineX1Ratio * bitmap.width
            val originalLineX2 = lineX2Ratio * bitmap.width
            val middleWidth = (originalLineX2 - originalLineX1).toInt()
            val scaledMiddleWidth = (middleWidth * scaleFactor).toInt()

            // Calculate total scaled width
            val leftWidth = originalLineX1.toInt()
            val rightWidth = (bitmap.width - originalLineX2).toInt()
            val totalScaledWidth = leftWidth + scaledMiddleWidth + rightWidth

            // Calculate horizontal offset to center the content
            horizontalOffset = (width - totalScaledWidth) / 2f

            // Only adjust offset if image is wider than view
            if (totalScaledWidth > width) {
                // Allow negative offset but keep at least part of the image visible
                val minOffset = width - totalScaledWidth.toFloat()
                val maxOffset = 0f
                horizontalOffset = horizontalOffset.coerceIn(minOffset, maxOffset)
            }

            // Draw the three parts of the image
            drawImageParts(canvas, bitmap, leftWidth, middleWidth, scaledMiddleWidth, rightWidth)

            // Draw lines and indicators
            drawLinesAndIndicators(canvas, bitmap, leftWidth, scaledMiddleWidth, totalScaledWidth)
        }
    }

    private fun drawImageParts(
        canvas: Canvas,
        bitmap: Bitmap,
        leftWidth: Int,
        middleWidth: Int,
        scaledMiddleWidth: Int,
        rightWidth: Int
    ) {
        // Draw left part (before lineX1)
        if (leftWidth > 0) {
            Bitmap.createBitmap(bitmap, 0, 0, leftWidth, bitmap.height)?.let {
                canvas.drawBitmap(it, horizontalOffset, 0f, null)
            }
        }

        // Draw middle part (between lineX1 and lineX2)
        if (middleWidth > 0 && scaledMiddleWidth > 0) {
            Bitmap.createBitmap(bitmap, leftWidth, 0, middleWidth, bitmap.height)
                ?.let { middle ->
                    Bitmap.createScaledBitmap(middle, scaledMiddleWidth, bitmap.height, true)
                        ?.let { scaled ->
                            canvas.drawBitmap(scaled, horizontalOffset + leftWidth, 0f, null)
                        }
                }
        }

        // Draw right part (after lineX2)
        if (rightWidth > 0) {
            Bitmap.createBitmap(bitmap, leftWidth + middleWidth, 0, rightWidth, bitmap.height)
                ?.let {
                    canvas.drawBitmap(
                        it,
                        horizontalOffset + leftWidth + scaledMiddleWidth,
                        0f,
                        null
                    )
                }
        }
    }

    private fun drawLinesAndIndicators(
        canvas: Canvas,
        bitmap: Bitmap,
        leftWidth: Int,
        scaledMiddleWidth: Int,
        totalScaledWidth: Int
    ) {
        if (totalScaledWidth > 0) {
            // Calculate line positions
            val line1X = horizontalOffset + leftWidth
            val line2X = horizontalOffset + leftWidth + scaledMiddleWidth

            // Ensure lines stay within visible bounds
            val visibleLine1X = line1X.coerceIn(0f, width.toFloat())
            val visibleLine2X = line2X.coerceIn(0f, width.toFloat())

            // Draw lines only if they are visible
            if (visibleLine1X in 0f..width.toFloat()) {
                canvas.drawLine(visibleLine1X, 0f, visibleLine1X, height.toFloat(), linePaint)
            }
            if (visibleLine2X in 0f..width.toFloat()) {
                canvas.drawLine(visibleLine2X, 0f, visibleLine2X, height.toFloat(), linePaint)
            }

            // Draw indicators when lines are near edges (within threshold)
            if (lineX1Ratio <= edgeThreshold && visibleLine1X in 0f..width.toFloat()) {
                drawEdgeIndicator(canvas, visibleLine1X, true)
            }
            if (lineX2Ratio >= (1f - edgeThreshold) && visibleLine2X in 0f..width.toFloat()) {
                drawEdgeIndicator(canvas, visibleLine2X, false)
            }
        }
    }

    private fun drawEdgeIndicator(canvas: Canvas, xPos: Float, isLeft: Boolean) {
        val centerY = height / 2f

        // Draw circle indicator
        canvas.drawCircle(xPos, centerY, indicatorRadius, linePaint)

        // Draw arrow
        val arrowX =
            if (isLeft) xPos + indicatorRadius + arrowPadding else xPos - indicatorRadius - arrowPadding
        val path = Path().apply {
            moveTo(arrowX, centerY - arrowSize / 2)
            lineTo(arrowX, centerY + arrowSize / 2)
            lineTo(if (isLeft) arrowX + arrowSize / 2 else arrowX - arrowSize / 2, centerY)
            close()
        }
        canvas.drawPath(path, arrowPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                return handleTouchDown(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDraggingLine1 || isDraggingLine2) {
                    handleTouchMove(event)
                    lastTouchX = event.x
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                return handleTouchUp()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouchDown(event: MotionEvent): Boolean {
        scaledBitmap?.let { bitmap ->
            // Calculate the distance between lines as a ratio of the image width
            val lineDistanceRatio = lineX2Ratio - lineX1Ratio

            // Determine touch area multiplier based on the line distance
            val touchAreaMultiplier = if (lineDistanceRatio <= widthThreshold) 1f else 15f

            val touchX = event.x
            // Calculate the actual X positions of the lines (including horizontalOffset)
            val originalLineX1 = lineX1Ratio * bitmap.width
            val originalLineX2 = lineX2Ratio * bitmap.width
            val middleWidth = (originalLineX2 - originalLineX1).toInt()
            val scaledMiddleWidth = (middleWidth * scaleFactor).toInt()

            val line1X = horizontalOffset + originalLineX1
            val line2X = horizontalOffset + originalLineX1 + scaledMiddleWidth

            // Check which line is being touched (with dynamic touch area)
            val touchArea = touchSlop * touchAreaMultiplier

            isDraggingLine1 = abs(touchX - line1X) <= touchArea
            isDraggingLine2 = !isDraggingLine1 && abs(touchX - line2X) <= touchArea

            return isDraggingLine1 || isDraggingLine2
        }
        return false
    }

    private fun handleTouchMove(event: MotionEvent) {
        if (!isDraggingLine1 && !isDraggingLine2) return

        scaledBitmap?.let { bitmap ->
            val originalWidth = bitmap.width.toFloat()
            val touchX = event.x - horizontalOffset // Adjust for horizontal offset

            val scaledMiddleWidth = (lineX2Ratio - lineX1Ratio) * originalWidth * scaleFactor
            val originalX = when {
                touchX < 0 -> 0f
                touchX > originalWidth + scaledMiddleWidth - (lineX2Ratio - lineX1Ratio) * originalWidth ->
                    originalWidth

                touchX <= lineX1Ratio * originalWidth -> touchX
                touchX <= lineX1Ratio * originalWidth + scaledMiddleWidth ->
                    lineX1Ratio * originalWidth + (touchX - lineX1Ratio * originalWidth) / scaleFactor

                else -> lineX2Ratio * originalWidth + (touchX - (lineX1Ratio * originalWidth + scaledMiddleWidth))
            }.coerceIn(0f, originalWidth)

            val ratio = originalX / originalWidth

            // Apply minimum distance constraint (6%)
            if (isDraggingLine1) {
                lineX1Ratio = ratio.coerceIn(0f, lineX2Ratio - minLineDistanceRatio)
            } else if (isDraggingLine2) {
                lineX2Ratio = ratio.coerceIn(lineX1Ratio + minLineDistanceRatio, 1f)
            }

            invalidate()
        }
    }

    private fun handleTouchUp(): Boolean {
        isDraggingLine1 = false
        isDraggingLine2 = false
        performClick()
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun getStretchedBitmapWithoutLines(): Bitmap? {
        return scaledBitmap?.let { bitmap ->
            val originalLineX1 = lineX1Ratio * bitmap.width
            val originalLineX2 = lineX2Ratio * bitmap.width
            val middleWidth = (originalLineX2 - originalLineX1).toInt()
            val scaledMiddleWidth = (middleWidth * scaleFactor).toInt()

            val leftWidth = originalLineX1.toInt().coerceAtLeast(0)
            val rightWidth = (bitmap.width - originalLineX2).toInt().coerceAtLeast(0)
            val totalScaledWidth =
                (leftWidth + scaledMiddleWidth + rightWidth).coerceAtLeast(1)  // Ensure width ≥ 1

            // Early return if no scaling is needed (prevents redundant bitmap creation)
            if (scaleFactor == 1f && lineX1Ratio == 0f && lineX2Ratio == 1f) {
                return bitmap.copy(bitmap.config!!, true)
            }

            val result =
                Bitmap.createBitmap(totalScaledWidth, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            // Draw left part (if width > 0)
            if (leftWidth > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, leftWidth, bitmap.height)?.let {
                    canvas.drawBitmap(it, 0f, 0f, null)
                }
            }
            // Draw stretched middle part (if width > 0)
            if (middleWidth > 0 && scaledMiddleWidth > 0) {
                Bitmap.createBitmap(bitmap, leftWidth, 0, middleWidth, bitmap.height)
                    ?.let { middle ->
                        Bitmap.createScaledBitmap(middle, scaledMiddleWidth, bitmap.height, true)
                            ?.let { scaled ->
                                canvas.drawBitmap(scaled, leftWidth.toFloat(), 0f, null)
                            }
                    }
            }

            // Draw right part (if width > 0)
            if (rightWidth > 0) {
                Bitmap.createBitmap(bitmap, leftWidth + middleWidth, 0, rightWidth, bitmap.height)
                    ?.let {
                        canvas.drawBitmap(it, (leftWidth + scaledMiddleWidth).toFloat(), 0f, null)
                    }
            }

            result
        }
    }
}*/
