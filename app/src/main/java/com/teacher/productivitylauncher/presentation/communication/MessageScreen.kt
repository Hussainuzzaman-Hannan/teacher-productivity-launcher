package com.teacher.productivitylauncher.presentation.communication

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageScreen(onClose: () -> Unit) {
    val context = LocalContext.current

    // সরাসরি ডিফল্ট মেসেজিং অ্যাপ ওপেন করুন
    fun openMessagingApp(phoneNumber: String = "", message: String = "") {
        val intent = if (phoneNumber.isNotEmpty()) {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phoneNumber")
                if (message.isNotEmpty()) {
                    putExtra("sms_body", message)
                }
            }
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse("sms:"))
        }
        context.startActivity(intent)
    }

    var phoneNumber by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }

    val templates = listOf(
        "Your child was absent today. Please check.",
        "Fees payment is due. Please pay by tomorrow.",
        "Parent-teacher meeting tomorrow at 10 AM.",
        "Exam results will be published tomorrow.",
        "School will remain closed tomorrow due to holiday."
    )

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
                    text = "💬 Send Message",
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
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Recipient Number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                label = { Text("Message (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                minLines = 4,
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Open Messaging App button
            Button(
                onClick = { openMessagingApp(phoneNumber, messageText) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (phoneNumber.isNotEmpty()) "Send Message to $phoneNumber" else "Open Messaging App")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Quick Message Templates", fontWeight = FontWeight.Medium)
            Text("(Tap to use template)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            templates.forEach { template ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { messageText = template },
                    shape = RoundedCornerShape(8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Text(
                        text = template,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}