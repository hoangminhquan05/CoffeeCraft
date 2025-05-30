package com.example.dacs3

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.dacs3.Model.Banner
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class BannerViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val bannersCollection = db.collection("banner")

    private val _banners = MutableStateFlow<List<Banner>>(emptyList())
    val banners: StateFlow<List<Banner>> = _banners

    private val _selectedBanner = MutableStateFlow<Banner?>(null)
    val selectedBanner: StateFlow<Banner?> = _selectedBanner

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading

    init {
        loadBanners()
    }

    fun loadBanners() {
        viewModelScope.launch {
            try {
                val result = bannersCollection.get().await()
                _banners.value = result.documents.map { doc ->
                    doc.toObject<Banner>()?.copy(id = doc.id) ?: Banner()
                }
            } catch (e: Exception) {
                Log.e("BannerViewModel", "Error loading banners: ${e.message}")
            }
        }
    }

    fun selectBanner(banner: Banner) {
        _selectedBanner.value = banner
    }

    fun clearSelection() {
        _selectedBanner.value = null
    }

    suspend fun uploadImageToCloudinary(context: Context, uri: Uri): String {
        return suspendCancellableCoroutine { continuation ->
            _isUploading.value = true
            _uploadProgress.value = 0

            val requestId = MediaManager.get().upload(uri)
                .option("folder", "banner")
                .option("public_id", "banner_${UUID.randomUUID()}")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        _uploadProgress.value = ((bytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
                    }

                    override fun onSuccess(requestId: String, resultData: Map<Any?, Any?>) {
                        _isUploading.value = false
                        val url = resultData["secure_url"]?.toString()
                            ?: resultData["url"].toString().replace("http://", "https://")
                        Log.d("CloudinaryUpload", "Upload success. URL: $url")
                        continuation.resume(url)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        _isUploading.value = false
                        continuation.resumeWithException(Exception(error.description))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        _isUploading.value = false
                        continuation.resumeWithException(Exception(error.description))
                    }
                })
                .dispatch(context)

            continuation.invokeOnCancellation {
                MediaManager.get().cancelRequest(requestId)
            }
        }
    }

    fun addBanner(banner: Banner) {
        viewModelScope.launch {
            try {
                val docRef = bannersCollection.add(banner).await()
                val bannerWithId = banner.copy(id = docRef.id)
                docRef.set(bannerWithId).await()
                loadBanners()
            } catch (e: Exception) {
                Log.e("BannerViewModel", "Error adding banner: ${e.message}")
            }
        }
    }

    // Trong BannerViewModel.kt
    fun updateBanner(banner: Banner) {
        viewModelScope.launch {
            try {
                if (banner.id.isNotEmpty()) {
                    // Kiểm tra xem có URL ảnh cũ không
                    val oldBanner = bannersCollection.document(banner.id).get().await()
                        .toObject<Banner>()

                    bannersCollection.document(banner.id).set(banner).await()

                    // Nếu có ảnh cũ và URL mới khác URL cũ thì xóa ảnh cũ
                    oldBanner?.let { old ->
                        if (old.banner_url.isNotEmpty() && old.banner_url != banner.banner_url) {
                            deleteImageFromCloudinary(old.banner_url)
                        }
                    }

                    loadBanners()
                }
            } catch (e: Exception) {
                Log.e("BannerViewModel", "Error updating banner: ${e.message}")
            }
        }
    }

    fun deleteBanner(bannerId: String) {
        viewModelScope.launch {
            try {
                val docSnapshot = bannersCollection.document(bannerId).get().await()
                val banner = docSnapshot.toObject<Banner>()

                if (banner != null) {
                    bannersCollection.document(bannerId).delete().await()

                    if (banner.banner_url.isNotEmpty()) {
                        deleteImageFromCloudinary(banner.banner_url)
                    }

                    loadBanners()
                    clearSelection()
                }
            } catch (e: Exception) {
                Log.e("DeleteBanner", "Error deleting banner: ${e.message}")
            }
        }
    }

    private suspend fun deleteImageFromCloudinary(imageUrl: String) {
        try {
            Log.d("CloudinaryDelete", "Image URL: $imageUrl")

            // Lấy phần sau "/upload/"
            val afterUpload = imageUrl.substringAfter("/upload/")
            // Bỏ phần version v138263763 và lấy phần sau dấu /
            val publicIdWithVersionRemoved = afterUpload.substringAfter("/")
            // Bỏ đuôi
            val publicId = publicIdWithVersionRemoved.substringBeforeLast(".")

            Log.d("CloudinaryDelete", "Deleting public_id: $publicId")

            val result = withContext(Dispatchers.IO) {
                MediaManager.get().cloudinary.uploader()
                    .destroy(publicId, mapOf("invalidate" to true))
            }

            Log.d("CloudinaryDelete", "Deleted image result: $result")
        } catch (e: Exception) {
            Log.e("CloudinaryDelete", "Failed to delete image: ${e.message}")
        }
    }


}
