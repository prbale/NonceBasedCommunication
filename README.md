# Secure Communication Demo

This repository contains a sample project demonstrating secure communication between an Android mobile application (built with Jetpack Compose) and a Node.js backend using a nonce-based encryption approach. The project illustrates how to encrypt/decrypt messages with AES-GCM, preventing replay attacks and ensuring data integrity.

## Table of Contents

- [Overview](#overview)
- [Architecture and Approach](#architecture-and-approach)
- [Components](#components)
  - [Android Client](#android-client)
  - [Node.js Backend](#nodejs-backend)
- [Setup and Usage](#setup-and-usage)
  - [Running the Node.js Backend](#running-the-nodejs-backend)
  - [Running the Android App](#running-the-android-app)
- [Dependencies](#dependencies)
- [Notes](#notes)
- [License](#license)

## Overview

This project demonstrates an extra layer of security at the application level by encrypting data before sending it over the network. Although the communication channel should ideally use TLS/HTTPS, this demo further encrypts request and response payloads using a nonce-based approach. The sample uses AES-GCM for encryption and includes nonce generation, encryption, decryption, and authentication of messages.

## Architecture and Approach

1. **Nonce-based Encryption:**  
   - A random nonce (number used once) is generated for every message.
   - The nonce is used as an initialization vector (IV) with AES-GCM to ensure that identical data produces different ciphertexts.
   - Both the Android client and the Node.js backend share a pre-established symmetric key (for demonstration purposes only).

2. **Message Flow:**  
   - **Client:** The Android app encrypts a JSON payload (e.g., a message) using AES-GCM and sends the Base64-encoded nonce, ciphertext, and authentication tag to the Node.js backend.
   - **Server:** The Node.js backend decrypts the payload using the shared key, processes the request, and then encrypts a response with a new nonce before sending it back.
   - **Response Processing:** The Android client decrypts the response and displays it.

3. **Security Considerations:**  
   - This project demonstrates additional encryption on top of TLS. In a production environment, keys should be exchanged securely (e.g., via Diffie-Hellman/ECDHE during a TLS handshake) and never hardcoded.
   - Replay protection and timestamp verification should be incorporated to further secure communications.

## Components

### Android Client

- **Technology:** Jetpack Compose for UI and OkHttp for network communication.
- **Key Files:**
  - `MainActivity.kt`: Contains the Compose UI, calls the encryption utilities, sends requests, and handles responses.
  - `CryptoUtils.java`: Provides AES-GCM encryption/decryption logic with nonce generation.
- **Functionality:**  
  On launch, the app sends an encrypted JSON message to the backend. When a response is received, it is decrypted and displayed in the UI.

### Node.js Backend

- **Technology:** Express.js framework and Node.js `crypto` module.
- **Key Files:**
  - `index.js`: Sets up an API endpoint `/api` that decrypts incoming requests, processes them, and sends back an encrypted response.
  - `package.json`: Lists project dependencies and scripts.
- **Functionality:**  
  The backend decrypts the message from the Android client, logs the request, and sends back an encrypted JSON response.

## Setup and Usage

### Running the Node.js Backend

1. **Clone the repository:**

   ```bash
   git clone https://github.com/your-username/secure-communication-demo.git
   cd secure-communication-demo/node-backend
  

2. **Install dependencies:**

   ```bash
   npm install

  
3. **Start the server:**
   
   ```bash
   npm start

The server will start on port 3000 and listen for requests at /api.


## Running the Android App

### Open the Project in Android Studio:
- Import the Android project folder into Android Studio.

### Update the Backend URL:
- In `MainActivity.kt`, update the `backendUrl` variable with your backend address.
- If testing on an emulator, use:  
  `http://10.0.2.2:3000/api`

### Ensure Permissions and Network Security:
- Verify that the `AndroidManifest.xml` includes the Internet permission:
  ```xml
  <uses-permission android:name="android.permission.INTERNET" />
- If needed for local testing, configure network_security_config.xml to allow cleartext traffic.

### Build and Run:
- Build the project and run the app on an emulator or physical device.
- On launch, tap the button to send an encrypted request. The response (after decryption) will be displayed on the screen.


## Dependencies

### Android Client:
- Jetpack Compose
- OkHttp (`com.squareup.okhttp3:okhttp:4.10.0`)

### Node.js Backend:
- Express (`express`)
- Body Parser (`body-parser`)
- Node.js built-in crypto module

## Notes

### Secure Key Exchange:
- In this demo, the symmetric key is hardcoded for simplicity.
- In a production system, use a secure key exchange protocol (e.g., TLS with ephemeral key exchange) to negotiate session keys dynamically.

### Transport Layer Security:
- Always use HTTPS/TLS in production to secure data in transit, in addition to application-level encryption.

### Replay Protection:
- Consider adding timestamps and maintaining a nonce history to prevent replay attacks.
