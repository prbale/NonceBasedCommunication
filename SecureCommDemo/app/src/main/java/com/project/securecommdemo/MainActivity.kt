package com.project.securecommdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.securecommdemo.ui.theme.SecureCommDemoTheme
import com.project.securecommdemo.utils.CryptoUtils
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    // In production, obtain this key via a secure key exchange mechanism.
    // For demonstration, we're using a hardcoded 32-byte key.
    private val sharedKey = "0123456789abcdef0123456789abcdef".toByteArray()

    // Replace with your backend's URL (use HTTPS in production)
    private val backendUrl = "http://192.168.1.8:3000/api"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureCommDemoTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SecureAppUI(sharedKey, backendUrl)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureAppUI(sharedKey: ByteArray, backendUrl: String) {
    var responseText by remember { mutableStateOf("No response yet") }
    var isLoading by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Secure Communication Demo") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                isLoading = true
                // Prepare request data (JSON string)
                val requestData = "{\"message\": \"Hello from Android using Compose!\"}"
                try {
                    // Encrypt the request using our CryptoUtils helper class.
                    val payload = CryptoUtils.encrypt(sharedKey, requestData.toByteArray(Charsets.UTF_8))
                    val json = JSONObject().apply {
                        put("nonce", payload.nonceBase64)
                        put("cipherText", payload.cipherTextBase64)
                        put("authTag", payload.authTagBase64)
                    }
                    // Send the secure request to the backend.
                    sendSecureRequest(json.toString(), backendUrl, sharedKey,
                        onSuccess = { decryptedResponse ->
                            responseText = decryptedResponse
                            isLoading = false
                        },
                        onFailure = { error ->
                            responseText = "Error: $error"
                            isLoading = false
                        }
                    )
                } catch (e: Exception) {
                    responseText = "Encryption error: ${e.localizedMessage}"
                    isLoading = false
                }
            }) {
                Text("Send Secure Request")
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = responseText)
        }
    }
}

fun sendSecureRequest(
    jsonData: String,
    backendUrl: String,
    sharedKey: ByteArray,
    onSuccess: (String) -> Unit,
    onFailure: (String) -> Unit
) {
    val client = OkHttpClient()
    val JSONMediaType = "application/json; charset=utf-8".toMediaType()
    val body = jsonData.toRequestBody(JSONMediaType)
    val request = Request.Builder()
        .url(backendUrl)
        .post(body)
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            onFailure(e.localizedMessage ?: "Unknown error")
        }
        override fun onResponse(call: Call, response: Response) {
            if (!response.isSuccessful) {
                onFailure("Unexpected response code: ${response.code}")
                return
            }
            try {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val jsonResponse = JSONObject(responseData)
                    val nonce = jsonResponse.getString("nonce")
                    val cipherText = jsonResponse.getString("cipherText")
                    val authTag = jsonResponse.getString("authTag")
                    val decryptedBytes = CryptoUtils.decrypt(sharedKey, nonce, cipherText, authTag)
                    val decryptedResponse = String(decryptedBytes, Charsets.UTF_8)
                    onSuccess(decryptedResponse)
                } else {
                    onFailure("Empty response")
                }
            } catch (e: Exception) {
                onFailure(e.localizedMessage ?: "Error parsing response")
            }
        }
    })
}