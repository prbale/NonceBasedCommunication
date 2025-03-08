const express = require('express');
const bodyParser = require('body-parser');
const crypto = require('crypto');

const app = express();
const PORT = 3000;

// Middleware to parse JSON requests
app.use(bodyParser.json());

const ALGORITHM = 'aes-256-gcm';
const NONCE_SIZE = 12; // 12 bytes for GCM
const TAG_LENGTH = 16; // 16 bytes auth tag

// Shared key (must match the one used by the Android client)
// In production, do NOT hardcode keys!
const sharedKey = Buffer.from("0123456789abcdef0123456789abcdef", "utf-8");

// Encrypt function
function encrypt(plainText) {
    const nonce = crypto.randomBytes(NONCE_SIZE);
    const cipher = crypto.createCipheriv(ALGORITHM, sharedKey, nonce, { authTagLength: TAG_LENGTH });
    let encrypted = cipher.update(plainText, 'utf8');
    encrypted = Buffer.concat([encrypted, cipher.final()]);
    const authTag = cipher.getAuthTag();
    return {
        nonce: nonce.toString('base64'),
        cipherText: encrypted.toString('base64'),
        authTag: authTag.toString('base64')
    };
}

// Decrypt function
function decrypt(nonceB64, cipherTextB64, authTagB64) {
    const nonce = Buffer.from(nonceB64, 'base64');
    const cipherText = Buffer.from(cipherTextB64, 'base64');
    const authTag = Buffer.from(authTagB64, 'base64');
    const decipher = crypto.createDecipheriv(ALGORITHM, sharedKey, nonce, { authTagLength: TAG_LENGTH });
    decipher.setAuthTag(authTag);
    let decrypted = decipher.update(cipherText, undefined, 'utf8');
    decrypted += decipher.final('utf8');
    return decrypted;
}

// API endpoint
app.post('/api', (req, res) => {
    try {
        const { nonce, cipherText, authTag } = req.body;
        // Decrypt incoming request
        const decryptedRequest = decrypt(nonce, cipherText, authTag);
        console.log("Decrypted request:", decryptedRequest);

        // Process the request (for demo purposes, simply respond with a message)
        const responseData = JSON.stringify({ message: "Hello from Node.js backend!" });
        // Encrypt the response with a new nonce
        const encryptedResponse = encrypt(responseData);

        res.json(encryptedResponse);
    } catch (error) {
        console.error("Error processing request:", error);
        res.status(400).json({ error: "Invalid request or decryption failed" });
    }
});

app.listen(PORT, () => {
    console.log(`Server is running on port ${PORT}`);
});
