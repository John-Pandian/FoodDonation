package com.it.fooddonation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.it.fooddonation.core.icons.logo
import com.it.fooddonation.data.model.UserRole
import com.it.fooddonation.ui.components.MinimalTextField
import com.it.fooddonation.ui.theme.Gray500
import com.it.fooddonation.ui.theme.Primary
import com.it.fooddonation.ui.theme.PrimaryLight

@Composable
fun RegisterScreen(
    onRegisterClick: (String, String, String, String, UserRole) -> Unit,
    onLoginClick: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.DONOR) }

    val scrollState = rememberScrollState()

    // Check if message is a success message
    val isSuccessMessage = errorMessage?.contains("Registration successful", ignoreCase = true) == true ||
                          errorMessage?.contains("check your email", ignoreCase = true) == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(scrollState)
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Logo Section
        Column(
            modifier = Modifier.padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(12f)
                        .background(
                            color = PrimaryLight,
                            shape = RoundedCornerShape(24.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .rotate(-6f)
                        .background(
                            color = Primary.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(24.dp)
                        )
                )
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            color = Color.White,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = logo,
                        contentDescription = "Food Donation Logo",
                        modifier = Modifier.size(70.dp),
                        tint = Primary
                    )
                }
            }
        }

        // Title
        Text(
            text = "Join the Community",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Start your journey of sharing and caring today",
            fontSize = 14.sp,
            color = Gray500,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Full Name
        MinimalTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name",
            placeholder = "John Doe",
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Email
        MinimalTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email Address",
            placeholder = "name@example.com",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Phone Number
        MinimalTextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = "Phone Number",
            placeholder = "+91 98765 43210",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Password
        MinimalTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            placeholder = "••••••••",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isLoading
        )

        // Password Strength Indicator
        if (password.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            PasswordStrengthIndicator(password = password)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Confirm Password
        MinimalTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            placeholder = "••••••••",
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            enabled = !isLoading
        )

        if (password != confirmPassword && confirmPassword.isNotBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Role Selection - Segmented Control
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "I WANT TO JOIN AS A",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            SegmentedControl(
                selectedRole = selectedRole,
                onRoleSelected = { selectedRole = it },
                enabled = !isLoading
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Error/Success Message
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isSuccessMessage) {
                            Color(0xFF10B981).copy(alpha = 0.1f) // Green for success
                        } else {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = errorMessage,
                    color = if (isSuccessMessage) {
                        Color(0xFF059669) // Green for success
                    } else {
                        MaterialTheme.colorScheme.error
                    },
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Register Button
        Button(
            onClick = {
                onRegisterClick(email, password, fullName, phoneNumber, selectedRole)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading &&
                    email.isNotBlank() &&
                    password.isNotBlank() &&
                    confirmPassword.isNotBlank() &&
                    fullName.isNotBlank() &&
                    phoneNumber.isNotBlank() &&
                    password == confirmPassword,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                disabledContainerColor = Primary.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Sign Up",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Login Link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 14.sp,
                color = Gray500
            )
            Text(
                text = "Sign In",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Primary,
                modifier = Modifier
                    .clickable(onClick = onLoginClick)
                    .padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun SegmentedControl(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFF3F4F6),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Donor Button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(
                    color = if (selectedRole == UserRole.DONOR) Primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled) { onRoleSelected(UserRole.DONOR) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Donor",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedRole == UserRole.DONOR) Color.White else Color(0xFF6B7280)
            )
        }

        // Receiver Button
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .background(
                    color = if (selectedRole == UserRole.RECEIVER) Primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = enabled) { onRoleSelected(UserRole.RECEIVER) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Receiver",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selectedRole == UserRole.RECEIVER) Color.White else Color(0xFF6B7280)
            )
        }
    }
}

enum class PasswordStrength(val label: String, val color: Color) {
    WEAK("Weak", Color(0xFFEF4444)),
    FAIR("Fair", Color(0xFFF59E0B)),
    GOOD("Good", Color(0xFF3B82F6)),
    STRONG("Strong", Color(0xFF10B981))
}

fun calculatePasswordStrength(password: String): PasswordStrength {
    var score = 0

    // Length check
    when {
        password.length >= 12 -> score += 2
        password.length >= 8 -> score += 1
    }

    // Contains lowercase
    if (password.any { it.isLowerCase() }) score += 1

    // Contains uppercase
    if (password.any { it.isUpperCase() }) score += 1

    // Contains digit
    if (password.any { it.isDigit() }) score += 1

    // Contains special character
    if (password.any { !it.isLetterOrDigit() }) score += 1

    return when {
        score <= 2 -> PasswordStrength.WEAK
        score <= 4 -> PasswordStrength.FAIR
        score <= 5 -> PasswordStrength.GOOD
        else -> PasswordStrength.STRONG
    }
}

@Composable
fun PasswordStrengthIndicator(
    password: String,
    modifier: Modifier = Modifier
) {
    val strength = calculatePasswordStrength(password)

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Strength Bars
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .background(
                            color = when {
                                index < strength.ordinal + 1 -> strength.color
                                else -> Color(0xFFE5E7EB)
                            },
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Strength Label and Requirements
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password Strength: ${strength.label}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = strength.color
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Password Requirements
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PasswordRequirement(
                text = "At least 8 characters",
                isMet = password.length >= 8
            )
            PasswordRequirement(
                text = "Contains uppercase & lowercase",
                isMet = password.any { it.isUpperCase() } && password.any { it.isLowerCase() }
            )
            PasswordRequirement(
                text = "Contains number",
                isMet = password.any { it.isDigit() }
            )
            PasswordRequirement(
                text = "Contains special character (!@#$%)",
                isMet = password.any { !it.isLetterOrDigit() }
            )
        }
    }
}

@Composable
fun PasswordRequirement(
    text: String,
    isMet: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = if (isMet) Color(0xFF10B981).copy(alpha = 0.1f) else Color(0xFFE5E7EB),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isMet) {
                Text(
                    text = "✓",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
        }
        Text(
            text = text,
            fontSize = 11.sp,
            color = if (isMet) Color(0xFF059669) else Gray500
        )
    }
}
