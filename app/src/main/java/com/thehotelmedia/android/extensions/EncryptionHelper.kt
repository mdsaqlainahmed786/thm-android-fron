package com.thehotelmedia.android.extensions

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

//object EncryptionHelper {
//
//    private const val ALGORITHM = "AES"
//    private const val TRANSFORMATION = "AES/ECB/PKCS5Padding"
//    private const val SECRET_KEY = "the.hotel.media.secret"
//
//    fun encrypt(input: String): String {
//        val cipher = Cipher.getInstance(TRANSFORMATION)
//        cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec())
//        val encryptedBytes = cipher.doFinal(input.toByteArray(Charsets.UTF_8))
//        return bytesToHex(encryptedBytes)
//    }
//
//    fun decrypt(input: String): String {
//        val cipher = Cipher.getInstance(TRANSFORMATION)
//        cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec())
//        val encryptedBytes = hexToBytes(input)
//        val decryptedBytes = cipher.doFinal(encryptedBytes)
//        return String(decryptedBytes, Charsets.UTF_8)
//    }
//
//    private fun getSecretKeySpec(): SecretKeySpec {
//        val key = ByteArray(16)
//        val secretKeyBytes = SECRET_KEY.toByteArray(Charsets.UTF_8)
//        System.arraycopy(secretKeyBytes, 0, key, 0, secretKeyBytes.size.coerceAtMost(key.size))
//        return SecretKeySpec(key, ALGORITHM)
//    }
//
//    private fun bytesToHex(bytes: ByteArray): String {
//        val hexChars = CharArray(bytes.size * 2)
//        for (i in bytes.indices) {
//            val v = bytes[i].toInt() and 0xFF
//            hexChars[i * 2] = "0123456789ABCDEF"[v ushr 4]
//            hexChars[i * 2 + 1] = "0123456789ABCDEF"[v and 0x0F]
//        }
//        return String(hexChars)
//    }
//
//    private fun hexToBytes(hexString: String): ByteArray {
//        val len = hexString.length
//        require(len % 2 == 0) { "Hex string length must be even" }
//        val data = ByteArray(len / 2)
//        var i = 0
//        while (i < len) {
//            data[i / 2] = ((Character.digit(hexString[i], 16) shl 4)
//                    + Character.digit(hexString[i + 1], 16)).toByte()
//            i += 2
//        }
//        return data
//    }
//}



object EncryptionHelper {

    private const val SECRET_KEY = "1234567890123456" // Update with your actual key
    private const val IV = "1234567890123456" // Update with your actual IV
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"

    fun encrypt(input: String): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(SECRET_KEY.toByteArray(Charsets.UTF_8), "AES")
        val ivParameterSpec = IvParameterSpec(IV.toByteArray(Charsets.UTF_8))

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
        val encryptedBytes = cipher.doFinal(input.toByteArray(Charsets.UTF_8))

        // URL-safe Base64 encoding
        val base64Encoded = Base64.getEncoder().encodeToString(encryptedBytes)
        return base64Encoded.replace("+", "-").replace("/", "_").replace("=", "")
    }

    fun decrypt(input: String): String {
        // Convert URL-safe Base64 back to standard Base64
        val base64 = input.replace("-", "+").replace("_", "/") + "=".repeat((4 - input.length % 4) % 4) // Ensure correct padding

        val cipher = Cipher.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(SECRET_KEY.toByteArray(Charsets.UTF_8), "AES")
        val ivParameterSpec = IvParameterSpec(IV.toByteArray(Charsets.UTF_8))

        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
        val decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(base64))
        return String(decryptedBytes, Charsets.UTF_8)
    }
}