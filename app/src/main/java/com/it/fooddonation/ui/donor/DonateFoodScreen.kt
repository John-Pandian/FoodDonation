package com.it.fooddonation.ui.donor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.it.fooddonation.data.model.FoodItem
import com.it.fooddonation.ui.components.MinimalTextField
import com.it.fooddonation.ui.donor.viewmodel.DonorViewModel
import com.it.fooddonation.ui.theme.Gray300
import com.it.fooddonation.ui.theme.Primary

/**
 * Represents a food item being created in the form
 */
data class FoodItemInput(
    val foodName: String = "",
    val quantity: String = ""
)

/**
 * Screen for creating a new food donation with multiple food items
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonateFoodScreen(
    viewModel: DonorViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onProfileClick: () -> Unit,
    authViewModel: com.it.fooddonation.ui.auth.viewmodel.AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val foodItems = remember { mutableStateListOf(FoodItemInput()) }
    var description by remember { mutableStateOf("") }
    var donationImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    // Image picker launcher - for the shared donation image
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        donationImageUri = uri
    }

    // Show error messages in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
            errorMessage = null
        }
    }

    // Check if user has address - if not, show prompt screen instead
    if (authState.userProfile?.address.isNullOrBlank()) {
        AddressRequiredScreen(
            onNavigateBack = onNavigateBack,
            onAddAddressClick = onProfileClick
        )
        return
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Donate Food",
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            // Instructions
            Text(
                text = "Add the food items you want to donate",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Donation Image Section (Shared for all food items)
            DonationImageSection(
                imageUri = donationImageUri,
                onAddPhoto = { imagePickerLauncher.launch("image/*") },
                onRemovePhoto = { donationImageUri = null },
                isEnabled = !isSubmitting
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Section Title
            Text(
                text = "Food Items",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Food Items List
            foodItems.forEachIndexed { index, item ->
                FoodItemCard(
                    index = index,
                    item = item,
                    onItemChange = { updatedItem ->
                        foodItems[index] = updatedItem
                    },
                    onRemove = {
                        if (foodItems.size > 1) {
                            foodItems.removeAt(index)
                        }
                    },
                    canRemove = foodItems.size > 1,
                    isEnabled = !isSubmitting
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Add Another Food Item Button
            OutlinedButton(
                onClick = {
                    foodItems.add(FoodItemInput())
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSubmitting
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add food item",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Another Food Item")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description Field (Optional)
            MinimalTextField(
                value = description,
                onValueChange = { description = it },
                label = "Additional Notes (Optional)",
                placeholder = "Any additional details about the donation...",
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Submit Button
            Button(
                onClick = {
                    val hasValidItems = foodItems.any {
                        it.foodName.isNotBlank() && it.quantity.isNotBlank()
                    }

                    if (!hasValidItems) {
                        errorMessage = "Please add at least one food item with name and quantity"
                        return@Button
                    }

                    isSubmitting = true
                    viewModel.createDonation(
                        foodItems = foodItems.filter {
                            it.foodName.isNotBlank() && it.quantity.isNotBlank()
                        },
                        description = description,
                        donationImageUri = donationImageUri,
                        onSuccess = {
                            isSubmitting = false
                            onNavigateBack()
                        },
                        onError = { error ->
                            isSubmitting = false
                            errorMessage = error
                        }
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isSubmitting && foodItems.any {
                    it.foodName.isNotBlank() && it.quantity.isNotBlank()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = Primary.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .height(24.dp)
                            .padding(end = 8.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Submitting...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                } else {
                    Text(
                        text = "Submit Donation",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Helper text
            Text(
                text = "Your donation will be visible to receivers once submitted",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

/**
 * Card for editing a single food item
 */
@Composable
private fun FoodItemCard(
    index: Int,
    item: FoodItemInput,
    onItemChange: (FoodItemInput) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with title and remove button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(
                                color = Primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Primary
                        )
                    }
                    Text(
                        text = "Food Item ${index + 1}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A)
                    )
                }

                if (canRemove) {
                    IconButton(
                        onClick = onRemove,
                        enabled = isEnabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove item",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Food Name Field
            MinimalTextField(
                value = item.foodName,
                onValueChange = { onItemChange(item.copy(foodName = it)) },
                label = "Food Name *",
                placeholder = "e.g., Rice, Bread, Vegetables, Fruit, Pasta",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity Field
            MinimalTextField(
                value = item.quantity,
                onValueChange = { onItemChange(item.copy(quantity = it)) },
                label = "Quantity *",
                placeholder = "e.g., 5 kg, 10 servings, 20 pieces, 3 bags, 2 liters",
                enabled = isEnabled,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 2
            )
        }
    }
}

/**
 * Shared donation image section
 */
@Composable
private fun DonationImageSection(
    imageUri: Uri?,
    onAddPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Donation Photo",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
            }

            Text(
                text = "Add a photo of your donation (optional)",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (imageUri != null) {
                // Show selected image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF3F4F6))
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Donation image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Remove image button
                    IconButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(36.dp)
                            .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove image",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Show add image button
                OutlinedButton(
                    onClick = onAddPhoto,
                    enabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = if (isEnabled) Primary.copy(alpha = 0.3f) else Gray300
                    )
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = "Add photo",
                            modifier = Modifier.size(40.dp),
                            tint = Primary.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Add Photo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Tap to upload an image",
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Screen shown when user doesn't have pickup address
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddressRequiredScreen(
    onNavigateBack: () -> Unit,
    onAddAddressClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Donate Food",
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
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(
                        color = Primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add, // TODO: Use location pin icon
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Pickup Address Required",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description
            Text(
                text = "Please add your pickup address before creating a donation. This helps receivers know where to collect the food.",
                fontSize = 15.sp,
                color = Color(0xFF6B7280),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Add Address Button
            Button(
                onClick = onAddAddressClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Add Pickup Address",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel button
            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Cancel",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}
