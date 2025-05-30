package com.example.dacs3.util

import android.content.Context
import com.cloudinary.Cloudinary
import com.cloudinary.android.MediaManager
import com.example.dacs3.BuildConfig

import java.util.HashMap


object CloudinaryConfig {
    private val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
    private val API_KEY = BuildConfig.CLOUDINARY_API_KEY
    private val API_SECRET = BuildConfig.CLOUDINARY_API_SECRET


    var isInitialized = false
        private set

    lateinit var cloudinary: Cloudinary
        private set

    fun init(context: Context) {
        if (isInitialized) return

        val config = hashMapOf(
            "cloud_name" to CLOUD_NAME,
            "api_key" to API_KEY,
            "api_secret" to API_SECRET
        )

        MediaManager.init(context, config)

        cloudinary = Cloudinary(config)
        isInitialized = true
    }
}