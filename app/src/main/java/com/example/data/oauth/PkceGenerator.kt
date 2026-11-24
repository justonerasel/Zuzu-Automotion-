package com.example.data.oauth

import java.security.MessageDigest
import java.security.SecureRandom

data class PkcePair(
  val codeVerifier: String,
  val codeChallenge: String,
  val stateToken: String,
)

object PkceGenerator {
  private val secureRandom = SecureRandom()
  private val urlSafeAlphabet =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

  fun create(): PkcePair {
    val verifierBytes = ByteArray(48)
    secureRandom.nextBytes(verifierBytes)
    val verifier = encodeBase64Url(verifierBytes)

    val stateBytes = ByteArray(24)
    secureRandom.nextBytes(stateBytes)
    val state = encodeBase64Url(stateBytes)

    val digest = MessageDigest.getInstance("SHA-256")
      .digest(verifier.toByteArray(Charsets.US_ASCII))
    val challenge = encodeBase64Url(digest)

    return PkcePair(
      codeVerifier = verifier,
      codeChallenge = challenge,
      stateToken = state,
    )
  }

  private fun encodeBase64Url(bytes: ByteArray): String {
    val out = StringBuilder((bytes.size * 4 + 2) / 3)
    var i = 0
    while (i < bytes.size) {
      val b0 = bytes[i].toInt() and 0xFF
      val hasB1 = i + 1 < bytes.size
      val hasB2 = i + 2 < bytes.size
      val b1 = if (hasB1) bytes[i + 1].toInt() and 0xFF else 0
      val b2 = if (hasB2) bytes[i + 2].toInt() and 0xFF else 0

      out.append(urlSafeAlphabet[b0 ushr 2])
      out.append(urlSafeAlphabet[((b0 and 0x03) shl 4) or (b1 ushr 4)])
      if (hasB1) {
        out.append(urlSafeAlphabet[((b1 and 0x0F) shl 2) or (b2 ushr 6)])
      }
      if (hasB2) {
        out.append(urlSafeAlphabet[b2 and 0x3F])
      }
      i += 3
    }
    return out.toString()
  }
}
