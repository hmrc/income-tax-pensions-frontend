/*
 * Copyright 2023 HM Revenue & Customs
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

package utils

import com.codahale.metrics.SharedMetricRegistries
import config.AppConfig
import models.mongo.TextAndKey
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import utils.TypeCaster.Converter.stringLoader

import java.security.InvalidAlgorithmParameterException
import java.util.Base64
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.{Cipher, IllegalBlockSizeException, KeyGenerator, NoSuchPaddingException}

class AesGCMCryptoSpec extends UnitTest {
  SharedMetricRegistries.clear()

  private implicit lazy val appConfig: AppConfig = mockAppConfig

  private val underTest = new AesGCMCrypto

  private val secretKey      = "VqmXp7yigDFxbCUdDdNZVIvbW6RgPNJsliv6swQNCL8="
  private val secretKey2     = "cXo7u0HuJK8B/52xLwW7eQ=="
  private val textToEncrypt  = "textNotEncrypted"
  private val associatedText = "associatedText"
  private val encryptedTextTest: EncryptedValue = EncryptedValue(
    "jOrmajkEqb7Jbo1GvK4Mhc3E7UiOfKS3RCy3O/F6myQ=",
    "WM1yMH4KBGdXe65vl8Gzd37Ob2Bf1bFUSaMqXk78sNeorPFOSWwwhOj0Lcebm5nWRhjNgL4K2SV3GWEXyyqeIhWQ4fJIVQRHM9VjWCTyf7/1/f/ckAaMHqkF1XC8bnW9"
  )

  implicit val textAndKey: TextAndKey = TextAndKey(associatedText, secretKey)

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .configure("feature-switch.useEncryption" -> "true")
      .build()

  "encrypt" should {
    "return plain text when turned off" in {
      val encrypterWithNoCrypt = new AesGCMCrypto()(
        new GuiceApplicationBuilder().build().injector.instanceOf[AppConfig]
      )
      val encryptedText = encrypterWithNoCrypt.encrypt(textToEncrypt)

      encryptedText.value shouldBe textToEncrypt
    }

    "return test, test" in {
      val encryptedText = underTest.encrypt(textToEncrypt)
      encryptedText shouldBe an[EncryptedValue]
    }

    "return an EncryptionDecryptionError if the associated text is an empty string" in {
      val emptyAssociatedText             = ""
      implicit val textAndKey: TextAndKey = TextAndKey(emptyAssociatedText, secretKey)

      val encryptedAttempt = intercept[EncryptionDecryptionException](underTest.encrypt(textToEncrypt))

      assert(encryptedAttempt.failureReason.contains("associated text must not be null"))
    }

    "return an EncryptionDecryptionError if the key is empty" in {
      val invalidSecretKey                = ""
      implicit val textAndKey: TextAndKey = TextAndKey(associatedText, invalidSecretKey)

      val encryptedAttempt = intercept[EncryptionDecryptionException](underTest.encrypt(textToEncrypt))

      assert(encryptedAttempt.failureReason.contains("The key provided is invalid"))
    }

    "return an EncryptionDecryptionError if the key is invalid" in {
      val invalidSecretKey = "invalidKey"

      implicit val textAndKey: TextAndKey = TextAndKey(associatedText, invalidSecretKey)

      val encryptedAttempt = intercept[EncryptionDecryptionException](underTest.encrypt(textToEncrypt))

      assert(
        encryptedAttempt.failureReason.contains("Key being used is not valid." +
          " It could be due to invalid encoding, wrong length or uninitialized"))
    }

    "return an EncryptionDecryptionError if the secret key is an invalid type" in {
      val keyGen = KeyGenerator.getInstance("DES")
      val key    = keyGen.generateKey()
      val aesGCMEncrypter = new AesGCMCrypto {
        override val ALGORITHM_KEY: String = "DES"
      }
      val encryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.generateCipherText(textToEncrypt, associatedText.getBytes, new GCMParameterSpec(96, "hjdfbhvbhvbvjvjfvb".getBytes), key)
      )
      assert(
        encryptedAttempt.failureReason.contains("Key being used is not valid." +
          " It could be due to invalid encoding, wrong length or uninitialized"))
    }

    "return an EncryptionDecryptionError if the alg is invalid" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override val ALGORITHM_TO_TRANSFORM_STRING: String = "invalid"
      }
      val encryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.encrypt((textToEncrypt, associatedText, secretKey))
      )
      assert(encryptedAttempt.failureReason.contains("Algorithm being requested is not available in this environment"))
    }

    "return an EncryptionDecryptionError if the padding is invalid" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new NoSuchPaddingException()
      }
      val encryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.encrypt((textToEncrypt, associatedText, secretKey))
      )
      assert(encryptedAttempt.failureReason.contains("Padding Scheme being requested is not available this environment"))
    }

    "return an EncryptionDecryptionError if a InvalidAlgorithmParameterException is thrown" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new InvalidAlgorithmParameterException()
      }
      val encryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.encrypt((textToEncrypt, associatedText, secretKey))
      )
      assert(encryptedAttempt.failureReason.contains("Algorithm parameters being specified are not valid"))
    }

    "return an EncryptionDecryptionError if a IllegalStateException is thrown" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new IllegalStateException()
      }
      val encryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.encrypt((textToEncrypt, associatedText, secretKey))
      )
      assert(encryptedAttempt.failureReason.contains("Cipher is in an illegal state"))
    }

    "return an EncryptionDecryptionError if a UnsupportedOperationException is thrown" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new UnsupportedOperationException()
      }
      val encryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.encrypt((textToEncrypt, associatedText, secretKey))
      )
      assert(encryptedAttempt.failureReason.contains("Provider might not be supporting this method"))
    }

    "return an EncryptionDecryptionError if a IllegalBlockSizeException is thrown" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new IllegalBlockSizeException()
      }
      val encryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.encrypt((textToEncrypt, associatedText, secretKey))
      )
      assert(encryptedAttempt.failureReason.contains("Error occured due to block size"))
    }

    "return an EncryptionDecryptionError if a RuntimeException is thrown" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new RuntimeException()
      }
      val encryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.encrypt((textToEncrypt, associatedText, secretKey))
      )
      assert(encryptedAttempt.failureReason.contains("Unexpected exception"))
    }
  }

  "decrypt" should {
    "return plain text when turned off" in {
      val encrypterWithNoCrypt = new AesGCMCrypto()(new GuiceApplicationBuilder().build().injector.instanceOf[AppConfig])
      val encryptedText        = encrypterWithNoCrypt.decrypt(textToEncrypt, "nonce")
      encryptedText shouldBe textToEncrypt
    }

    "decrypt an encrypted value when the encrytedValue, associatedText, nonce, and secretKey are the same used for encryption" in {
      val decryptedText = underTest.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      decryptedText shouldBe textToEncrypt
    }

    "return a EncryptionDecryptionException if the encrytedValue is different" in {
      val decryptedAttempt = intercept[EncryptionDecryptionException](
        underTest.decrypt(Base64.getEncoder.encodeToString("diffentvalues".getBytes), encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("Error occured due to padding scheme"))
    }

    "return a EncryptionDecryptionException if the nonce is different" in {
      val decryptedAttempt = intercept[EncryptionDecryptionException](
        underTest.decrypt(encryptedTextTest.value, Base64.getEncoder.encodeToString("jdbfjdgvcjksabcvajbvjkbvjbdvjbvjkabv".getBytes))
      )
      assert(decryptedAttempt.failureReason.contains("Error occured due to padding scheme"))
    }

    "return a EncryptionDecryptionException if the associatedText is different" in {
      implicit val textAndKey: TextAndKey = TextAndKey("idsngfbsadjvbdsvjb", secretKey)

      val decryptedAttempt = intercept[EncryptionDecryptionException](
        underTest.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("Error occured due to padding scheme"))
    }

    "return a EncryptionDecryptionException if the secretKey is different" in {
      implicit val textAndKey: TextAndKey = TextAndKey("idsngfbsadjvbdsvjb", secretKey2)

      val decryptedAttempt = intercept[EncryptionDecryptionException](
        underTest.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("Error occured due to padding scheme"))
    }

    "return an EncryptionDecryptionError if the associated text is an empty string" in {
      val emptyAssociatedText = ""

      implicit val textAndKey: TextAndKey = TextAndKey(emptyAssociatedText, secretKey2)

      val decryptedAttempt = intercept[EncryptionDecryptionException](
        underTest.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("associated text must not be null"))
    }

    "return an EncryptionDecryptionError if the key is empty" in {
      val invalidSecretKey = ""

      implicit val textAndKey: TextAndKey = TextAndKey(associatedText, invalidSecretKey)

      val decryptedAttempt = intercept[EncryptionDecryptionException](
        underTest.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("The key provided is invalid"))
    }

    "return an EncryptionDecryptionError if the key is invalid" in {
      val invalidSecretKey = "invalidKey"

      implicit val textAndKey: TextAndKey = TextAndKey(associatedText, invalidSecretKey)

      val decryptedAttempt = intercept[EncryptionDecryptionException](
        underTest.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(
        decryptedAttempt.failureReason.contains("Key being used is not valid." +
          " It could be due to invalid encoding, wrong length or uninitialized"))
    }

    "return an EncryptionDecryptionError if the secret key is an invalid type" in {
      val keyGen = KeyGenerator.getInstance("DES")
      val key    = keyGen.generateKey()
      val aesGCMEncrypter = new AesGCMCrypto {
        override val ALGORITHM_KEY: String = "DES"
      }
      val decryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.decryptCipherText(
          encryptedTextTest.value,
          associatedText.getBytes,
          new GCMParameterSpec(96, "hjdfbhvbhvbvjvjfvb".getBytes),
          key)
      )
      assert(
        decryptedAttempt.failureReason.contains("Key being used is not valid." +
          " It could be due to invalid encoding, wrong length or uninitialized"))
    }

    "return an EncryptionDecryptionError if the alg is invalid" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override val ALGORITHM_TO_TRANSFORM_STRING: String = "invalid"
      }
      val decryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("Algorithm being requested is not available in this environment"))
    }

    "return an EncryptionDecryptionError if the padding is invalid" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new NoSuchPaddingException()
      }
      val decryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("Padding Scheme being requested is not available this environment"))
    }

    "return an EncryptionDecryptionError if a InvalidAlgorithmParameterException is thrown" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new InvalidAlgorithmParameterException()
      }
      val decryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("Algorithm parameters being specified are not valid"))
    }

    "return an EncryptionDecryptionError if a IllegalStateException is thrown" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new IllegalStateException()
      }
      val decryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("Cipher is in an illegal state"))
    }

    "return an EncryptionDecryptionError if a UnsupportedOperationException is thrown" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new UnsupportedOperationException()
      }
      val decryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("Provider might not be supporting this method"))
    }

    "return an EncryptionDecryptionError if a IllegalBlockSizeException is thrown" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new IllegalBlockSizeException()
      }
      val decryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("Error occured due to block size"))
    }

    "return an EncryptionDecryptionError if a RuntimeException is thrown" in {
      val aesGCMEncrypter = new AesGCMCrypto {
        override def getCipherInstance: Cipher = throw new RuntimeException()
      }
      val decryptedAttempt = intercept[EncryptionDecryptionException](
        aesGCMEncrypter.decrypt(encryptedTextTest.value, encryptedTextTest.nonce)
      )
      assert(decryptedAttempt.failureReason.contains("Unexpected exception"))
    }
  }
}
