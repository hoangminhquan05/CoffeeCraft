package com.example.dacs3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import com.example.dacs3.ui.theme.DACS3Theme
import com.example.dacs3.util.CloudinaryConfig

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo Cloudinary trước khi setContent
        CloudinaryConfig.init(applicationContext)

        setContent {
            DACS3Theme {
                val navController = rememberNavController()
                val cartViewModel = remember { CartViewModel() }

                AppNavigation(
                    navController = navController,
                    cartViewModel = cartViewModel,
                    cloudinary = CloudinaryConfig.cloudinary
                )
            }
        }
    }
}