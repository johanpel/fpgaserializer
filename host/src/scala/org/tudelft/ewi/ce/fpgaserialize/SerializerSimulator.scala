package org.tudelft.ewi.ce.fpgaserialize

import java.nio.ByteBuffer

object SerializerSimulator {

  System.loadLibrary("fpgaserializer")
  @native def printObjectMemory(obj: AnyRef) : Unit

  def serializeObject(obj: AnyRef) : Unit = {

    printObjectMemory(obj)

    val klass = obj.getClass
    val fields = ReflectionHelper.getAllFields(klass)
    fields foreach {f =>
      if (!f.getType.isPrimitive) {
        f.setAccessible(true)
        serializeObject(f.get(obj))
      }
    }
  }
}
