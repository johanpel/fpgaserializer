import sun.misc.Unsafe

object UnsafeInstance {
  def get: sun.misc.Unsafe = {
    val f = classOf[Unsafe].getDeclaredField("theUnsafe")
    f.setAccessible(true)
    val unsafe = f.get(null).asInstanceOf[Unsafe]
    unsafe
  }
}
