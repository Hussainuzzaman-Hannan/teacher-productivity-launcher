package com.teacher.productivitylauncher.presentation.questionmaker

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionMakerScreen(onClose: () -> Unit) {
    var questionText by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(List(4) { "" }) }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question Maker",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = questionText,
                onValueChange = { questionText = it },
                label = { Text("Enter Question") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Options", fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(8.dp))

            options.indices.forEach { index ->
                OutlinedTextField(
                    value = options[index],
                    onValueChange = { newValue ->
                        options = options.toMutableList().apply { set(index, newValue) }
                    },
                    label = { Text("Option ${index + 1}") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { /* Add more options */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Option")
                }

                Button(
                    onClick = { /* Save question */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save Question")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Coming soon: Save questions to database and create quizzes",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}