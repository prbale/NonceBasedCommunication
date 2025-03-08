package com.project.securecommdemo

//noinspection UsingMaterialAndMaterial3Libraries
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.securecommdemo.ui.theme.SecureCommDemoTheme
import com.project.securecommdemo.utils.CryptoUtils
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    // In production, obtain this key via a secure key exchange mechanism.
    // Server will also has the same key.
    // For demonstration, we're using a hardcoded 32-byte key.
    private val sharedKey = "0123456789abcdef0123456789abcdef".toByteArray()

    // Replace with your backend's URL (use HTTPS in production)
    private val backendUrl = "http://127.0.0.1:3000/api"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SecureCommDemoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // UI
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

                // ----------------------------------------------------------------
                // Prepare request data (JSON string)
                val requestData = "{\"message\": \"Hello from Android using Compose!\"}"
                // ----------------------------------------------------------------'

                try {
                    // Encrypt the request using our CryptoUtils helper class.
                    val payload = CryptoUtils.encrypt(sharedKey, requestData.toByteArray(Charsets.UTF_8))
                    val json = JSONObject().apply {
                        put("nonce", payload.nonceBase64)
                        put("cipherText", payload.cipherTextBase64)
                        put("authTag", payload.authTagBase64)
                    }

                    // Send the secure request to the backend.
                    sendSecureRequest(
                        json.toString(),
                        backendUrl,
                        sharedKey,
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
    val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    val body = jsonData.toRequestBody(jsonMediaType)
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

                // Get the Success Response
                val responseData = response.body?.string()
                if (responseData != null) {
                    val jsonResponse = JSONObject(responseData)

                    // Fetch Nonce, Encrypted Response and Auth Tag
                    val nonce = jsonResponse.getString("nonce")
                    val cipherText = jsonResponse.getString("cipherText")
                    val authTag = jsonResponse.getString("authTag")

                    // Decrypt the Response
                    val decryptedBytes = CryptoUtils.decrypt(sharedKey, nonce, cipherText, authTag)
                    val decryptedResponse = String(decryptedBytes, Charsets.UTF_8)

                    println("Nonce: ${nonce}")
                    println("cipherText: ${cipherText}")
                    println("authTag: ${authTag}")
                    println("decryptedResponse: ${decryptedResponse}")

                    // Display it on to the UI
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
