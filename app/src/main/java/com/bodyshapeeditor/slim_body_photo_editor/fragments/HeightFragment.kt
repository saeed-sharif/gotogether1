package com.bodyshapeeditor.slim_body_photo_editor.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.net.Uri
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bodyshapeeditor.slim_body_photo_editor.PhotoEditorHelper
import com.bodyshapeeditor.slim_body_photo_editor.R
import com.bodyshapeeditor.slim_body_photo_editor.ads.showAdAndGo
import com.bodyshapeeditor.slim_body_photo_editor.ads.showBanner
import com.bodyshapeeditor.slim_body_photo_editor.databinding.FragmentHeightBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs


class HeightFragment : Fragment() {
    private var _binding: FragmentHeightBinding? = null
    private val binding get() = _binding!!
    private lateinit var activityResultLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private val helper = PhotoEditorHelper()
    lateinit var ImageUri: Uri


    // Launch camera
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && ImageUri != null) {
            val bitmap = helper.fixImageOrientation(requireContext(), ImageUri)
            bitmap?.let {
                binding.stretchView.setImageBitmap(bitmap!!)
                binding.stretchView.setLinesRatio(0.3f, 0.6f)

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
        _binding = FragmentHeightBinding.inflate(inflater, container, false)
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
        val imageUri = arguments?.getString("imageUri")?.let { Uri.parse(it) }
        if (imageUri != null) {
            val bitmap = helper.fixImageOrientation(requireContext(), imageUri)
            binding.stretchView.setImageBitmap(bitmap!!)
            binding.stretchView.setLinesRatio(0.3f, 0.6f)
        } else {
            Toast.makeText(requireContext(), "Image not found", Toast.LENGTH_SHORT).show()
        }

        activityResultLauncher =
            registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { imageUri ->
                if (imageUri != null) {
                    val bitmap = helper.fixImageOrientation(requireContext(), imageUri)
                    bitmap?.let {
                        binding.stretchView.setImageBitmap(it)
                        binding.stretchView.setLinesRatio(0.3f, 0.6f)
                    }
                }
            }

        setupScaleControls()
        binding.backBtn.setOnClickListener {
            findNavController().navigate(R.id.action_height_to_home)
        }

        binding.btnClose.setOnClickListener {
            findNavController().navigate(R.id.action_height_to_home)
        }
        binding.galleryButton.setOnClickListener {

            val action = HeightFragmentDirections.actionHeightFragmentToGalleryFragment(
                "height",
                imageUri.toString()
            )
            findNavController().navigate(action)

        }
        binding.cameraButton.setOnClickListener {
            ImageUri = helper.createImageUri(requireContext())
            cameraLauncher.launch(ImageUri)

        }

        binding.btnTick.setOnClickListener {

            val bitmap = binding.stretchView.getStretchedBitmapWithoutLines()
            bitmap?.let {
                lifecycleScope.launch(Dispatchers.IO) {
                    val uri = helper.saveBitmapToGallery(it, requireContext())
                    withContext(Dispatchers.Main) {
                        if (uri != null) {
                            Toast.makeText(requireContext(), "Image saved!", Toast.LENGTH_SHORT)
                                .show()
                            requireActivity().showAdAndGo {
                                val action =
                                    HeightFragmentDirections.actionHeightFragmentToSavedImageStatus(
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
                    findNavController().navigate(R.id.action_height_to_home)
                }
            })
    }

    private fun setupScaleControls() {
        binding.scaleSeekBar.max = 200 // 0 to 200
        binding.scaleSeekBar.progress = 100 // default at center (1x)

        binding.scaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val center = 100
                val scale = when {
                    progress > center -> 1f + ((progress - center) / 500f)  // small increase
                    progress < center -> 1f - ((center - progress) / 500f)  // small decrease
                    else -> 1f
                }

                binding.stretchView.setScaleFactor(scale)
                binding.scaleValueText.text = "Scale: ${"%.2f".format(scale)}x"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        binding.scaleValueText.text = "Scale: 1.00x"
    }

    override fun onResume() {
        super.onResume()
        binding.scaleSeekBar.progress = 100
        binding.stretchView.setScaleFactor(1f)
        binding.scaleValueText.text = "Scale: 1.00x"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


class StretchHeightImageView @JvmOverloads constructor(
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

    // Line position and scaling (now for Y-axis)
    private var lineY1Ratio = 0.3f
    private var lineY2Ratio = 0.6f
    private var scaleFactor = 1.0f
    private val minLineDistanceRatio = 0.06f // 2% minimum distance

    // Touch handling
    private var isDraggingLine1 = false
    private var isDraggingLine2 = false
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var verticalOffset = 0f
    private var lastTouchY = 0f

    // Visual elements
    private val arrowSize = 40f
    private val arrowPadding = 10f
    private val edgeThreshold = 0.2f
    private val heightThreshold = 0.5f // 50% threshold for touch area adjustment
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

    fun setLinesRatio(y1Ratio: Float, y2Ratio: Float) {
        // Ensure lines maintain minimum distance (2% of height)
        if (abs(y2Ratio - y1Ratio) < minLineDistanceRatio) {
            // Adjust the moving line to maintain distance
            if (y1Ratio != lineY1Ratio) { // lineY1 is being moved
                lineY1Ratio = y1Ratio.coerceIn(0f, lineY2Ratio - minLineDistanceRatio)
            } else { // lineY2 is being moved
                lineY2Ratio = y2Ratio.coerceIn(lineY1Ratio + minLineDistanceRatio, 1f)
            }
        } else {
            lineY1Ratio = y1Ratio.coerceIn(0f, lineY2Ratio - minLineDistanceRatio)
            lineY2Ratio = y2Ratio.coerceIn(lineY1Ratio + minLineDistanceRatio, 1f)
        }
        invalidate()
    }

    fun getLinesRatio(): Pair<Float, Float> = Pair(lineY1Ratio, lineY2Ratio)

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
            val viewWidth = width
            if (viewWidth > 0) {
                val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
                val scaledHeight = (viewWidth / aspectRatio).toInt()
                scaledBitmap = Bitmap.createScaledBitmap(bitmap, viewWidth, scaledHeight, true)
                invalidate()
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        scaledBitmap?.let { bitmap ->
            val originalLineY1 = lineY1Ratio * bitmap.height
            val originalLineY2 = lineY2Ratio * bitmap.height
            val middleHeight = (originalLineY2 - originalLineY1).toInt()
            val scaledMiddleHeight = (middleHeight * scaleFactor).toInt()

            // Calculate total scaled height
            val topHeight = originalLineY1.toInt()
            val bottomHeight = (bitmap.height - originalLineY2).toInt()
            val totalScaledHeight = topHeight + scaledMiddleHeight + bottomHeight

            // Calculate vertical offset to center the content
            verticalOffset = (height - totalScaledHeight) / 2f

            // Only adjust offset if image is taller than view
            if (totalScaledHeight > height) {
                // Allow negative offset but keep at least part of the image visible
                val minOffset = height - totalScaledHeight.toFloat()
                val maxOffset = 0f
                verticalOffset = verticalOffset.coerceIn(minOffset, maxOffset)
            }

            // Draw the three parts of the image
            drawImageParts(
                canvas,
                bitmap,
                topHeight,
                middleHeight,
                scaledMiddleHeight,
                bottomHeight
            )

            // Draw lines and indicators
            drawLinesAndIndicators(canvas, bitmap, topHeight, scaledMiddleHeight, totalScaledHeight)
        }
    }

    private fun drawImageParts(
        canvas: Canvas,
        bitmap: Bitmap,
        topHeight: Int,
        middleHeight: Int,
        scaledMiddleHeight: Int,
        bottomHeight: Int
    ) {
        // Draw top part (above lineY1)
        if (topHeight > 0) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, topHeight)?.let {
                canvas.drawBitmap(it, 0f, verticalOffset, null)
            }
        }

        // Draw middle part (between lineY1 and lineY2)
        if (middleHeight > 0 && scaledMiddleHeight > 0) {
            Bitmap.createBitmap(bitmap, 0, topHeight, bitmap.width, middleHeight)
                ?.let { middle ->
                    Bitmap.createScaledBitmap(middle, bitmap.width, scaledMiddleHeight, true)
                        ?.let { scaled ->
                            canvas.drawBitmap(scaled, 0f, verticalOffset + topHeight, null)
                        }
                }
        }

        // Draw bottom part (below lineY2)
        if (bottomHeight > 0) {
            Bitmap.createBitmap(bitmap, 0, topHeight + middleHeight, bitmap.width, bottomHeight)
                ?.let {
                    canvas.drawBitmap(
                        it,
                        0f,
                        verticalOffset + topHeight + scaledMiddleHeight,
                        null
                    )
                }
        }
    }


    private fun drawLinesAndIndicators(
        canvas: Canvas,
        bitmap: Bitmap,
        topHeight: Int,
        scaledMiddleHeight: Int,
        totalScaledHeight: Int
    ) {
        if (totalScaledHeight > 0) {
            // Calculate line positions
            val line1Y = verticalOffset + topHeight
            val line2Y = verticalOffset + topHeight + scaledMiddleHeight

            // Ensure lines stay within visible bounds
            val visibleLine1Y = line1Y.coerceIn(0f, height.toFloat())
            val visibleLine2Y = line2Y.coerceIn(0f, height.toFloat())

            // Draw lines only if they are visible
            if (visibleLine1Y in 0f..height.toFloat()) {
                canvas.drawLine(0f, visibleLine1Y, width.toFloat(), visibleLine1Y, linePaint)
            }
            if (visibleLine2Y in 0f..height.toFloat()) {
                canvas.drawLine(0f, visibleLine2Y, width.toFloat(), visibleLine2Y, linePaint)
            }

            // Draw indicators when lines are near edges (within threshold)
            if (lineY1Ratio <= edgeThreshold && visibleLine1Y in 0f..height.toFloat()) {
                drawEdgeIndicator(canvas, visibleLine1Y, true)
            }
            if (lineY2Ratio >= (1f - edgeThreshold) && visibleLine2Y in 0f..height.toFloat()) {
                drawEdgeIndicator(canvas, visibleLine2Y, false)
            }
        }
    }

    private fun drawEdgeIndicator(canvas: Canvas, yPos: Float, isTop: Boolean) {
        val centerX = width / 2f

        // Draw circle indicator
        canvas.drawCircle(centerX, yPos, indicatorRadius, linePaint)

        // Draw arrow
        val arrowY =
            if (isTop) yPos + indicatorRadius + arrowPadding else yPos - indicatorRadius - arrowPadding
        val path = Path().apply {
            moveTo(centerX - arrowSize / 2, arrowY)
            lineTo(centerX + arrowSize / 2, arrowY)
            lineTo(centerX, if (isTop) arrowY + arrowSize / 2 else arrowY - arrowSize / 2)
            close()
        }
        canvas.drawPath(path, arrowPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchY = event.y
                return handleTouchDown(event)
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDraggingLine1 || isDraggingLine2) {
                    handleTouchMove(event)
                    lastTouchY = event.y
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
            // Calculate the distance between lines as a ratio of the image height
            val lineDistanceRatio = lineY2Ratio - lineY1Ratio

            // Determine touch area multiplier based on the line distance
            val touchAreaMultiplier = if (lineDistanceRatio <= heightThreshold) 1f else 15f

            val touchY = event.y
            // Calculate the actual Y positions of the lines (including verticalOffset)
            val originalLineY1 = lineY1Ratio * bitmap.height
            val originalLineY2 = lineY2Ratio * bitmap.height
            val middleHeight = (originalLineY2 - originalLineY1).toInt()
            val scaledMiddleHeight = (middleHeight * scaleFactor).toInt()

            val line1Y = verticalOffset + originalLineY1
            val line2Y = verticalOffset + originalLineY1 + scaledMiddleHeight

            // Check which line is being touched (with dynamic touch area)
            val touchArea = touchSlop * touchAreaMultiplier

            isDraggingLine1 = abs(touchY - line1Y) <= touchArea
            isDraggingLine2 = !isDraggingLine1 && abs(touchY - line2Y) <= touchArea

            return isDraggingLine1 || isDraggingLine2
        }
        return false
    }

    private fun handleTouchMove(event: MotionEvent) {
        if (!isDraggingLine1 && !isDraggingLine2) return

        scaledBitmap?.let { bitmap ->
            val originalHeight = bitmap.height.toFloat()
            val touchY = event.y - verticalOffset

            val scaledMiddleHeight = (lineY2Ratio - lineY1Ratio) * originalHeight * scaleFactor
            val originalY = when {
                touchY < 0 -> 0f
                touchY > originalHeight + scaledMiddleHeight - (lineY2Ratio - lineY1Ratio) * originalHeight ->
                    originalHeight

                touchY <= lineY1Ratio * originalHeight -> touchY
                touchY <= lineY1Ratio * originalHeight + scaledMiddleHeight ->
                    lineY1Ratio * originalHeight + (touchY - lineY1Ratio * originalHeight) / scaleFactor

                else -> lineY2Ratio * originalHeight + (touchY - (lineY1Ratio * originalHeight + scaledMiddleHeight))
            }.coerceIn(0f, originalHeight)

            val ratio = originalY / originalHeight

            // Apply minimum distance constraint
            if (isDraggingLine1) {
                lineY1Ratio = ratio.coerceIn(0f, lineY2Ratio - minLineDistanceRatio)
            } else if (isDraggingLine2) {
                lineY2Ratio = ratio.coerceIn(lineY1Ratio + minLineDistanceRatio, 1f)
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
            val originalLineY1 = lineY1Ratio * bitmap.height
            val originalLineY2 = lineY2Ratio * bitmap.height
            val middleHeight = (originalLineY2 - originalLineY1).toInt()
            val scaledMiddleHeight = (middleHeight * scaleFactor).toInt()

            val topHeight = originalLineY1.toInt()
            val bottomHeight = (bitmap.height - originalLineY2).toInt()
            val totalScaledHeight = topHeight + scaledMiddleHeight + bottomHeight

            val result =
                Bitmap.createBitmap(bitmap.width, totalScaledHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)

            // Draw top part
            if (topHeight > 0) {
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, topHeight)?.let {
                    canvas.drawBitmap(it, 0f, 0f, null)
                }
            }

            // Draw stretched middle part
            if (middleHeight > 0 && scaledMiddleHeight > 0) {
                Bitmap.createBitmap(bitmap, 0, topHeight, bitmap.width, middleHeight)
                    ?.let { middle ->
                        Bitmap.createScaledBitmap(middle, bitmap.width, scaledMiddleHeight, true)
                            ?.let { scaled ->
                                canvas.drawBitmap(scaled, 0f, topHeight.toFloat(), null)
                            }
                    }
            }

            // Draw bottom part
            if (bottomHeight > 0) {
                Bitmap.createBitmap(bitmap, 0, topHeight + middleHeight, bitmap.width, bottomHeight)
                    ?.let {
                        canvas.drawBitmap(it, 0f, (topHeight + scaledMiddleHeight).toFloat(), null)
                    }
            }

            result
        }
    }
}