/**
 * Created with IntelliJ IDEA.
 * User: aparrish
 * Date: 2/14/14
 * Time: 10:04 AM
 *
 * Example of this code lifted from here:
 * https://gist.github.com/mumoshu/1587327
 *
 */
package com.shoutout.ds.framework.protocol {

  import annotation.implicitNotFound

  import java.io.{ DataOutputStream, ByteArrayOutputStream }

  @implicitNotFound(msg = "Could not find a Writes for ${T}")
  trait Writes[T] {

    def writes(value : T) : Array[Byte]
  }

  class DataOutputStreamWrites[T](writeValue : (DataOutputStream, T) => Unit) extends Writes[T] {

    def writes(value : T) : Array[Byte] = {
      val bos = new ByteArrayOutputStream
      val dos = new DataOutputStream(bos)
      writeValue(dos, value)
      dos.flush()
      val byteArray = bos.toByteArray
      bos.close()
      byteArray
    }
  }

  object defaults {
    implicit object WritesString extends Writes[String] {
      def writes(value : String) = value.getBytes("UTF-8")
    }
    implicit object WritesLong extends DataOutputStreamWrites[Long](_.writeLong(_))
    implicit object WritesInt extends DataOutputStreamWrites[Int](_.writeInt(_))
    implicit object WritesShort extends DataOutputStreamWrites[Short](_.writeShort(_))
  }

}

package com.shoutout.ds.framework.crypto {

  import com.shoutout.ds.framework.protocol.Writes

  import javax.crypto.spec.{ IvParameterSpec, SecretKeySpec }
  import javax.crypto.Cipher

  trait Encryption {
    def encrypt(dataBytes : Array[Byte], secret : String) : Array[Byte]
    def decrypt(codeBytes : Array[Byte], secret : String) : Array[Byte]

    def encrypt[T : Writes](data : T, secret : String) : Array[Byte] = encrypt(implicitly[Writes[T]].writes(data), secret)
  }

  class JavaCryptoEncryption(algorithmName : String) extends Encryption {

    def encrypt(bytes : Array[Byte], secret : String) : Array[Byte] = {
      val secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), algorithmName)
      val encipher = Cipher.getInstance(algorithmName)
      encipher.init(Cipher.ENCRYPT_MODE, secretKey)
      encipher.doFinal(bytes)
    }

    def decrypt(bytes : Array[Byte], secret : String) : Array[Byte] = {
      val secretKey = new SecretKeySpec(secret.getBytes("UTF-8"), algorithmName)
      val encipher = Cipher.getInstance(algorithmName)
      encipher.init(Cipher.DECRYPT_MODE, secretKey)
      encipher.doFinal(bytes)
    }
  }

  object DES extends JavaCryptoEncryption("DES")
  object AES extends JavaCryptoEncryption("AES")

}

package com.shoutout.ds.framework.crypto.aes {

  import com.shoutout.ds.framework.crypto.AES
  import org.apache.commons.codec.binary.Base64
  import com.shoutout.ds.framework.crypto._
  import com.shoutout.ds.framework.protocol.defaults._

  trait AESEncryption {
    private def encodeBase64(bytes : Array[Byte]) = Base64.encodeBase64String(bytes)
    private def decodeBase64(bytes : Array[Byte]) = Base64.decodeBase64(bytes)

    private def encryptField(fieldValue : String) : String =
      encodeBase64(AES.encrypt(fieldValue, "secretEncryption"))

    private def decryptField(fieldValue : String) : String =
      new String(AES.decrypt(decodeBase64(fieldValue.getBytes()), "secretEncryption"))
  }

}