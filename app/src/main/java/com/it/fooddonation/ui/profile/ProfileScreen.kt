package com.it.fooddonation.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.it.fooddonation.ui.auth.viewmodel.AuthViewModel
import com.it.fooddonation.ui.components.MinimalTextField
import com.it.fooddonation.ui.theme.Primary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val authState by authViewModel.authState.collectAsState()
    val userProfile = authState.userProfile

    var fullName by remember { mutableStateOf(userProfile?.fullName ?: "") }
    var phoneNumber by remember { mutableStateOf(userProfile?.phoneNumber ?: "") }
    var address by remember { mutableStateOf(userProfile?.address ?: "") }
    var city by remember { mutableStateOf(userProfile?.city ?: "") }
    var isUpdating by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Update fields when profile changes
    LaunchedEffect(userProfile) {
        userProfile?.let {
            fullName = it.fullName ?: ""
            phoneNumber = it.phoneNumber ?: ""
            address = it.address ?: ""
            city = it.city ?: ""
        }
    }

    // Show update message
    LaunchedEffect(updateMessage) {
        updateMessage?.let {
            snackbarHostState.showSnackbar(it)
            updateMessage = null
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profile & Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A1A1A)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFFAFAFA))
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Email (Read-only)
            Column {
                Text(
                    text = "EMAIL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = userProfile?.email ?: "",
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Color(0xFFF3F4F6),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                )
            }

            // Full Name
            MinimalTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Full Name",
                placeholder = "John Doe",
                enabled = !isUpdating
            )

            // Phone Number
            MinimalTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = "Phone Number",
                placeholder = "+1 234 567 8900",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                enabled = !isUpdating
            )

            // Address
            MinimalTextField(
                value = address,
                onValueChange = { address = it },
                label = "Pickup Address",
                placeholder = "123 Main Street, Apartment 4B",
                enabled = !isUpdating,
                minLines = 2,
                maxLines = 3
            )

            // City
            MinimalTextField(
                value = city,
                onValueChange = { city = it },
                label = "City",
                placeholder = "New York",
                enabled = !isUpdating
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save Changes Button
            Button(
                onClick = {
                    isUpdating = true
                    authViewModel.updateProfile(
                        fullName = fullName,
                        phoneNumber = phoneNumber,
                        address = address,
                        city = city,
                        onSuccess = {
                            isUpdating = false
                            updateMessage = "Profile updated successfully"
                        },
                        onError = { error ->
                            isUpdating = false
                            updateMessage = error
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isUpdating &&
                        fullName.isNotBlank() &&
                        phoneNumber.isNotBlank() &&
                        address.isNotBlank() &&
                        city.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Save Changes",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            // Logout Button
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(
                    text = "Logout",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
