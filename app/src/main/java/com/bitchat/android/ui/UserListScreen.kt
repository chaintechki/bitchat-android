package com.bitchat.android.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bitchat.android.model.NetworkUser

/**
 * Displays a simple list of users similar to WhatsApp contact list.
 */
@Composable
fun UserListScreen(users: List<NetworkUser>) {
    val context = LocalContext.current
    val selectedUser = remember { mutableStateOf<NetworkUser?>(null) }

    Surface(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(users) { user ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedUser.value = user }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = user.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    selectedUser.value?.let { user ->
        UserDetailDialog(
            user = user,
            onCall = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.phone}"))
                context.startActivity(intent)
            },
            onDismiss = { selectedUser.value = null }
        )
    }
}

/**
 * Simple dialog showing user location and offering a call option.
 */
@Composable
fun UserDetailDialog(user: NetworkUser, onCall: () -> Unit, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onCall) {
                Icon(Icons.Default.Call, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Call")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Close") }
        },
        title = { Text(user.name) },
        text = {
            Column {
                Text("Latitude: ${user.latitude}")
                Text("Longitude: ${user.longitude}")
            }
        }
    )
}
