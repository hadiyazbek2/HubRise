package com.example.hubrise.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Handles camera capture and gallery pick for images and videos.
 * Register in Fragment.onCreate (before the fragment is resumed).
 *
 * Usage:
 *   private lateinit var mediaPicker: MediaPickerHelper
 *
 *   override fun onCreate(savedInstanceState: Bundle?) {
 *       super.onCreate(savedInstanceState)
 *       mediaPicker = MediaPickerHelper(this) { uri -> handleMediaSelected(uri) }
 *   }
 *
 *   // Then to open:
 *   mediaPicker.pickFromGallery()   // image or video
 *   mediaPicker.captureFromCamera() // photo only
 */
class MediaPickerHelper(
    private val fragment: Fragment,
    private val onMediaSelected: (uri: Uri, file: File) -> Unit,
) {
    private var cameraImageUri: Uri? = null

    // Gallery: picks image or video
    private val galleryLauncher: ActivityResultLauncher<Intent> =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: return@registerForActivityResult
                val file = copyUriToCache(fragment.requireContext(), uri) ?: return@registerForActivityResult
                onMediaSelected(uri, file)
            }
        }

    // Camera: captures a photo to a temp file
    private val cameraLauncher: ActivityResultLauncher<Intent> =
        fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = cameraImageUri ?: return@registerForActivityResult
                val file = File(uri.path ?: return@registerForActivityResult)
                if (file.exists()) onMediaSelected(uri, file)
            }
        }

    // Permission request
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            if (grants.values.any { it }) pendingAction?.invoke()
            pendingAction = null
        }

    private var pendingAction: (() -> Unit)? = null

    fun pickFromGallery() {
        val permissions = requiredGalleryPermissions()
        if (hasPermissions(permissions)) {
            launchGallery()
        } else {
            pendingAction = { launchGallery() }
            permissionLauncher.launch(permissions)
        }
    }

    fun captureFromCamera() {
        val permissions = arrayOf(Manifest.permission.CAMERA)
        if (hasPermissions(permissions)) {
            launchCamera()
        } else {
            pendingAction = { launchCamera() }
            permissionLauncher.launch(permissions)
        }
    }

    private fun launchGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        galleryLauncher.launch(Intent.createChooser(intent, "Select media"))
    }

    private fun launchCamera() {
        val ctx = fragment.requireContext()
        val photoFile = createTempImageFile(ctx)
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", photoFile)
        cameraImageUri = uri
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uri)
        }
        cameraLauncher.launch(intent)
    }

    private fun hasPermissions(permissions: Array<String>): Boolean =
        permissions.all { ContextCompat.checkSelfPermission(fragment.requireContext(), it) == PackageManager.PERMISSION_GRANTED }

    private fun requiredGalleryPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun createTempImageFile(context: Context): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(context.externalCacheDir, "JPEG_${timestamp}.jpg").also { it.createNewFile() }
    }

    /** Copies a content:// URI to the app's cache dir so we have a real File path for upload. */
    private fun copyUriToCache(context: Context, uri: Uri): File? = try {
        val ext = when {
            uri.toString().contains("video") -> "mp4"
            else -> "jpg"
        }
        val dest = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        }
        dest
    } catch (e: Exception) { null }
}
