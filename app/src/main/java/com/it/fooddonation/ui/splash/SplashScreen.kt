package com.it.fooddonation.ui.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.it.fooddonation.core.icons.logo
import com.it.fooddonation.ui.theme.Primary
import com.it.fooddonation.ui.theme.PrimaryLight

/**
 * Splash screen shown while checking authentication status
 */
@Composable
fun SplashScreen(
    onCheckComplete: () -> Unit
) {
    // Trigger auth check when screen is composed
    LaunchedEffect(Unit) {
        onCheckComplete()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background decorative boxes
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
                // Main logo box with app logo
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

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Food Donation",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                color = Primary,
                strokeWidth = 3.dp
            )
        }
    }
}
