package com.stark.memorygame.view.security

import android.os.Build
import android.util.Base64
import androidx.annotation.RequiresApi
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object Encryption {

    private const val encryption_algorithm = "AES"

    private fun getSecretKey(): SecretKey {
        return KeyGenerator.getInstance(encryption_algorithm).generateKey()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun encrypt(data: String): EncryptionState {
        val secretKey = getSecretKey()
        val cipher = Cipher.getInstance("${encryption_algorithm}/CBC/PKCS7Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val ivBytes = cipher.iv
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        val baseIVEncoded = Base64.encodeToString(ivBytes, Base64.DEFAULT)
        val basePasswordEncoded = Base64.encodeToString(encryptedBytes, Base64.DEFAULT)
        if (secretKey.encoded != null) {
            val baseSecretKey: String = Base64.encodeToString(secretKey.encoded, Base64.DEFAULT)
            return EncryptionState(baseIVEncoded, basePasswordEncoded, baseSecretKey)
        } else {
            throw NullPointerException("Secret key is null")
        }
    }

    fun decrypt(state: EncryptionState): String {
        val password = Base64.decode(state.encryptedPassword, Base64.DEFAULT)
        val ivBytes = Base64.decode(state.iv, Base64.DEFAULT)
        val baseSecretKey = Base64.decode(state.secretKey, Base64.DEFAULT)
        val secretKey = SecretKeySpec(baseSecretKey, 0, baseSecretKey.size, encryption_algorithm)
        val cipher = Cipher.getInstance("${encryption_algorithm}/CBC/PKCS7Padding")
        val spec = IvParameterSpec(ivBytes)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
        return cipher.doFinal(password).toString(Charsets.UTF_8)
    }
}

data class EncryptionState(
    val iv: String,
    val encryptedPassword: String,
    val secretKey: String
)