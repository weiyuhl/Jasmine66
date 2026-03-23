package com.lhzkml.jasmine.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ChatComposer(
    value: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isRunning: Boolean
) {
    val composerShape = RoundedCornerShape(22.dp)
    val sendEnabled = enabled && value.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        if (isRunning) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BasicText(
                    text = "⏳",
                    style = TextStyle(
                        color = Color(0xFF10A37F),
                        fontSize = 12.sp
                    )
                )
                BasicText(
                    text = "正在生成回复...",
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = Color(0xFF666666)
                    )
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFE8E8E8), composerShape)
                .background(Color.White, composerShape)
                .padding(start = 16.dp, top = 10.dp, end = 12.dp, bottom = 8.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                singleLine = false,
                maxLines = 3,
                textStyle = TextStyle(
                    color = Color(0xFF1F2937),
                    fontSize = 16.sp,
                    lineHeight = 22.sp
                ),
                cursorBrush = SolidColor(Color(0xFF111111)),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, end = 8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (value.isEmpty()) {
                            BasicText(
                                text = "询问任何问题",
                                style = TextStyle(
                                    fontSize = 16.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                            )
                        }
                        innerTextField()
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .border(1.dp, Color(0xFFE5E7EB), CircleShape)
                        .background(Color(0xFFF8F8F8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = "+",
                        style = TextStyle(
                            fontSize = 20.sp,
                            color = Color(0xFF6B7280),
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                IconButton(
                    onClick = onSendClick,
                    enabled = sendEnabled,
                    modifier = Modifier
                        .size(34.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (sendEnabled) Color(0xFF111111) else Color(0xFFF3F4F6),
                        contentColor = if (sendEnabled) Color.White else Color(0xFF9CA3AF),
                    )
                ) {
                    BasicText(
                        text = "↑",
                        style = TextStyle(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
}
