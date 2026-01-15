package com.it.fooddonation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.it.fooddonation.ui.theme.Gray300
import com.it.fooddonation.ui.theme.Primary

@Composable
fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label.uppercase(),
            fontSize = 10.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholder,
                    color = Gray300
                )
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            enabled = enabled,
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Primary,
                unfocusedIndicatorColor = Gray300,
                disabledIndicatorColor = Gray300,
                cursorColor = Primary
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
