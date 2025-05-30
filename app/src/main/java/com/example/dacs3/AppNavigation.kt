package com.example.dacs3

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.cloudinary.Cloudinary
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation(
    navController: NavHostController,
    cartViewModel: CartViewModel,
    cloudinary: Cloudinary
) {
    val auth = FirebaseAuth.getInstance()
    var userId by remember { mutableStateOf<String?>(auth.currentUser?.uid) }

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {
        composable("login") {
            LoginScreen(navController, auth) { uid ->
                userId = uid
                navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }

        composable("home") {
            HomeScreen(navController = navController)
        }

        composable("product") {
            ProductScreen(
                navController = navController,
                cartViewModel = cartViewModel
            )
        }

        composable("product_detail/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            ProductDetailScreen(
                navController = navController,
                productId = productId,
                cartViewModel = cartViewModel
            )
        }

        composable("cart") {
            CartScreen(
                navController = navController,
                viewModel = cartViewModel,
                userId = userId // truyền UID
            )
        }


        composable("thongtincanhan/{userId}") { backStackEntry ->
            val userUid = backStackEntry.arguments?.getString("userId")
            ThongTinCaNhan(
                navController = navController,
                userId = userUid
            )
        }

        composable("profile") {
            ProfileScreen(
                navController = navController,
                onLogout = {
                    auth.signOut()
                    userId = null
                    navController.navigate("login") {
                        popUpTo(0)
                    }
                },
                userId = userId
            )
        }

        composable("adminproduct") {
            AdminProduct(navController = navController, cloudinary = cloudinary)
        }

        composable("add_product") {
            AddProductScreen(navController = navController, cloudinary = cloudinary)
        }

        composable("edit_product/{productId}") { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            EditProductScreen(navController = navController, productId = productId, cloudinary = cloudinary)
        }
        composable("staff_management") {
            StaffManagementScreen(navController = navController, cloudinary = cloudinary)
        }
        composable("forgot_pass") {
            ForgotPasswordScreen(navController = navController)
        }
        composable("reset_pass/{phone}") { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            ResetPasswordScreen(navController, phone)
        }
        composable("editUserInfo/{userId}") { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId")
            EditUserInfo(navController = navController, userId = userId, cloudinary = cloudinary)
        }
        composable("payment") {
            PaymentScreen(navController = navController, viewModel = cartViewModel, userId = userId)
        }
        composable("change_password") {
            ChangePasswordScreen(navController)
        }
        composable("statistics") {
            StatisticsScreen(navController = navController, userId = userId)
        }
        composable("bannerManagement") {
            BannerManagementScreen(navController = navController)
        }
        composable("add_staff") {
            AddStaffScreen(navController = navController)
        }
//        composable("invoice_screen") {
//            InvoiceScreen(
//                navController = navController,
//                viewModel = cartViewModel,
//                userId = userId,
//                userName = userName,
//                currentDate = currentDate,
//                paymentMethod = paymentMethod
//            )
//        }
        composable("table_management") {
            TableManagementScreen(navController)
        }



    }
}
