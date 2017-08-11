package od.auxiliary

import java.io.{File, FileInputStream}
import java.security.DigestInputStream
import javax.xml.bind.annotation.adapters.HexBinaryAdapter

object SHA1Hasher {
  /*
  * SHA1 Hashing method, both Byte Hash and String Hash supported
  *
  * */
  def apply(raw:String): String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.digest(raw.getBytes("UTF-8")).map("%02x".format(_)).mkString
  }

  def apply(bytes:Array[Byte]):String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
    md.digest(bytes).map("%02x".format(_)).mkString
  }

  /*
  * @throws IOException
  * @throws NoSuchAlgorithmException
  * */
  def apply(file:File):String = {
    val md = java.security.MessageDigest.getInstance("SHA-1")


    autoClose(new FileInputStream(file)){
      fileInputStream =>
        autoClose(new DigestInputStream(fileInputStream,md)){
          digestInputStream =>
            //size of the buffer: 1024
            val buf = new Array[Byte](1024)
            while(digestInputStream.read(buf) > 0){}
        }
    }


    new HexBinaryAdapter().marshal(md.digest())
  }
}
