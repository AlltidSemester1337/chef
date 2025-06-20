package com.formulae.chef.feature.useraccount.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun InputField(
    label: String,
    placeholder: String,
    value: String? = null,
    onValueChange: (String) -> Unit,
    supportingText: String? = null,
    visualTransformation: VisualTransformation? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF403633),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        color = Color.White,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = Color(0xFFF9D8CF),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    //TODO: Test and check if purple shit is also displayed on phone. WHY???
                    OutlinedTextField(
                        value = value ?: "",
                        onValueChange = onValueChange,
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = Color.Transparent)
                        //.border(
                        //    BorderStroke(width = 2.dp, color = Purple40),
                        //    shape = RoundedCornerShape(50)
                        ,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        //textStyle = TextStyle(
                        //    fontSize = 16.sp,
                        //    fontWeight = FontWeight.Normal,
                        //    lineHeight = 24.sp
                        //),
                        //modifier = Modifier.weight(1f),
                        placeholder = { Text(placeholder) },
                        visualTransformation = visualTransformation ?: VisualTransformation.None
                    )

                }
            }

            supportingText?.let { text ->
                Text(
                    text = text,
                    color = Color(0xFF5F5451),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInputField() {
    InputField(
        label = "Email Address",
        placeholder = "Enter your email address",
        value = null,
        onValueChange = {},
        supportingText = "We'll never share your email with anyone else."
    )
}
