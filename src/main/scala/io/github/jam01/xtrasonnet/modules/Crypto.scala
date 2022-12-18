package io.github.jam01.xtrasonnet.modules

/*-
 * Copyright 2022 Jose Montoya.
 *
 * Licensed under the Elastic License 2.0; you may not use this file except in
 * compliance with the Elastic License 2.0.
 */

import org.bouncycastle.util.encoders.Hex
import sjsonnet.Std.builtin
import sjsonnet.Val

import java.nio.charset.StandardCharsets
import java.security.{MessageDigest, SecureRandom}
import javax.crypto.{Cipher, Mac}
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}

object Crypto {
  val functions: Seq[(String, Val.Func)] = Seq(
    builtin("hash", "value", "algorithm") {
      (_, _, value: String, algorithm: String) =>
        Crypto.hash(value, algorithm)
    },

    builtin("hmac", "value", "secret", "algorithm") {
      (_, _, value: String, secret: String, algorithm: String) =>
        Crypto.hmac(value, secret, algorithm)
    },

    builtin("encrypt", "value", "secret", "algorithm") {
      (_, _, value: String, secret: String, transformation: String) =>
        val cipher = Cipher.getInstance(transformation)
        val transformTokens = transformation.split("/")

        // special case for ECB because of java.security.InvalidAlgorithmParameterException: ECB mode cannot use IV
        if (transformTokens.length >= 2 && "ECB".equals(transformTokens(1))) {
          cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase))
          java.util.Base64.getEncoder.encodeToString(cipher.doFinal(value.getBytes))
        } else {
          // https://stackoverflow.com/a/52571774/4814697
          val rand: SecureRandom = new SecureRandom()
          val iv = new Array[Byte](cipher.getBlockSize)
          rand.nextBytes(iv)

          cipher.init(Cipher.ENCRYPT_MODE,
            new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase),
            new IvParameterSpec(iv),
            rand)

          // encrypted data:
          val encryptedBytes = cipher.doFinal(value.getBytes)

          // append Initiation Vector as a prefix to use it during decryption:
          val combinedPayload = new Array[Byte](iv.length + encryptedBytes.length)

          // populate payload with prefix IV and encrypted data
          System.arraycopy(iv, 0, combinedPayload, 0, iv.length)
          System.arraycopy(encryptedBytes, 0, combinedPayload, iv.length, encryptedBytes.length)

          java.util.Base64.getEncoder.encodeToString(combinedPayload)
        }
    },

    builtin("decrypt", "value", "secret", "algorithm") {
      (_, _, value: String, secret: String, transformation: String) =>
        val cipher = Cipher.getInstance(transformation)
        val transformTokens = transformation.split("/")

        // special case for ECB because of java.security.InvalidAlgorithmParameterException: ECB mode cannot use IV
        if (transformTokens.length >= 2 && "ECB".equals(transformTokens(1))) {
          cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase))
          new String(cipher.doFinal(java.util.Base64.getDecoder.decode(value)))
        } else {
          // https://stackoverflow.com/a/52571774/4814697
          // separate prefix with IV from the rest of encrypted data//separate prefix with IV from the rest of encrypted data
          val encryptedPayload = java.util.Base64.getDecoder.decode(value)
          val iv = new Array[Byte](cipher.getBlockSize)
          val encryptedBytes = new Array[Byte](encryptedPayload.length - iv.length)
          val rand: SecureRandom = new SecureRandom()

          // populate iv with bytes:
          System.arraycopy(encryptedPayload, 0, iv, 0, iv.length)

          // populate encryptedBytes with bytes:
          System.arraycopy(encryptedPayload, iv.length, encryptedBytes, 0, encryptedBytes.length)

          cipher.init(Cipher.DECRYPT_MODE,
            new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase),
            new IvParameterSpec(iv),
            rand)

          new String(cipher.doFinal(encryptedBytes))
        }
    }
  )

  /* datasonnet-mapper copyright/notice, per Apache-2.0 § 4.c */
  /*-
   * Copyright 2019-2020 the original author or authors.
   *
   * Licensed under the Apache License, Version 2.0 (the "License");
   * you may not use this file except in compliance with the License.
   * You may obtain a copy of the License at
   *
   *     http://www.apache.org/licenses/LICENSE-2.0
   *
   * Unless required by applicable law or agreed to in writing, software
   * distributed under the License is distributed on an "AS IS" BASIS,
   * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   * See the License for the specific language governing permissions and
   * limitations under the License.
   */
  /*
   * Changed:
   * - Incorporated and auto-converted (IntelliJ IDEA) Java datasonnet.Crypto#hash and datasonnet.Crypto#hmac
   */
  /*
   * datasonnet.Crypto: start
   *  Algorithm can be one of MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
   */
  private def hash(value: String, algorithm: String): String = {
    val digest = MessageDigest.getInstance(algorithm)
    val hash = digest.digest(value.getBytes(StandardCharsets.UTF_8))
    new String(Hex.encode(hash), StandardCharsets.UTF_8)
  }

  /*
      Algorithm can be HmacSHA1, HmacSHA256 or HmacSHA512
   */
  private def hmac(value: String, secret: String, algorithm: String): String = {
    val signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm)
    val mac = Mac.getInstance(algorithm)
    mac.init(signingKey)
    val hmac = mac.doFinal(value.getBytes(StandardCharsets.UTF_8))
    new String(Hex.encode(hmac), StandardCharsets.UTF_8)
  }
  /*
   * datasonnet.Crypto: end
   */
}
