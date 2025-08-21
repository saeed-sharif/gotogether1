package com.bodyshapeeditor.slim_body_photo_editor

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException

class PhotoEditorHelper {

    fun fixImageOrientation(context: Context, uri: Uri): Bitmap? {
        try {
            // First get the orientation
            val exifInputStream = context.contentResolver.openInputStream(uri) ?: return null
            val exif = ExifInterface(exifInputStream)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            exifInputStream.close()

            // Then load the scaled bitmap
            val scaledBitmap = loadScaledBitmap(context, uri) ?: return null

            // Rotate the scaled bitmap if needed
            return when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(scaledBitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(scaledBitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(scaledBitmap, 270f)
                else -> scaledBitmap
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }


    fun loadScaledBitmap(context: Context, imageUri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(imageUri)
                ?.use { BitmapFactory.decodeStream(it, null, options) }

            options.inSampleSize = calculateInSampleSize(options, 1024, 1024)
            options.inJustDecodeBounds = false

            context.contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }


    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES,Manifest.permission.CAMERA)

    } else {
        arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
    }


    fun saveBitmapToGallery(bitmap: Bitmap, context: Context): Uri? {
        return try {
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "Edited_Image_${System.currentTimeMillis()}.png")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Make Me Slim")
            }

            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                val outputStream = contentResolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                }
                uri
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    //get images
    fun loadImagesFromDevice(context: Context): List<Uri> {
        val imageUris = mutableListOf<Uri>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(collection, projection, null, null, sortOrder)
            ?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val uri = ContentUris.withAppendedId(collection, id)
                    imageUris.add(uri)
                }
            }

        return imageUris
    }


    fun getAppImages(context: Context): List<Uri> {
        val uris = mutableListOf<Uri>()
        val resolver = context.contentResolver
        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH}=?"
        val path = "${Environment.DIRECTORY_PICTURES}/Make Me Slim/"
        val cursor = resolver.query(
            collection,
            arrayOf(MediaStore.Images.Media._ID),
            selection,
            arrayOf(path),
            null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                uris.add(ContentUris.withAppendedId(collection, id))
            }
        }
        return uris
    }


    fun deleteImageByUri(context: Context, imageUri: Uri): Boolean {
        return try {
            val rowsDeleted = context.contentResolver.delete(imageUri, null, null)
            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun shareImageToAnyApp(context: Context, imageUri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share image via"))
    }



     fun createImageUri(context: Context): Uri {
        val file =
            File(context.filesDir, "camera_photo_${System.currentTimeMillis()}.png")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

}