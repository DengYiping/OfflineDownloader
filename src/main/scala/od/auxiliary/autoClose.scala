package od.auxiliary

object autoClose {
  /*
  * You should handle the exception by wrapping it with try block
  * */
  def apply[A <: AutoCloseable, B](closeable: => A)(fun: (A) => B):B = {
    try{
      fun(closeable)
    } finally {
      closeable.close()
    }
  }
}
