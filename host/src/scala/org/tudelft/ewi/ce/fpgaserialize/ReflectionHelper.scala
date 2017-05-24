package org.tudelft.ewi.ce.fpgaserialize

import java.lang.reflect.{Field, Modifier}

import scala.collection.mutable.ArrayBuffer

object ReflectionHelper {
  def getAllFields(klass: Class[_]): Array[Field] = {
    var fields = ArrayBuffer.empty[Field]
    var superKlass = klass
    while ((superKlass != classOf[java.lang.Object]) && (superKlass != null)) {
      for (f <- superKlass.getDeclaredFields) {
        if (!Modifier.isStatic(f.getModifiers) && !Modifier.isTransient(f.getModifiers)) {
          fields.append(f)
        }
      }
      superKlass = superKlass.getSuperclass
    }
    fields.toArray
  }

  def getAllClasses(klass: Class[_], prev: ArrayBuffer[Class[_]] = null): Array[Class[_]] = {
    var klasses = if (prev == null) ArrayBuffer[Class[_]](klass) else prev
    if (klass.isArray) {
      val compType = klass.getComponentType
      if (!compType.isPrimitive) {
        if (!klasses.contains(klass)) {
          klasses.append(klass)
        }
        if (!klasses.contains(compType)) {
          klasses.append(compType)
          getAllClasses(compType, klasses)
        }
      }
    }
    val fields = ReflectionHelper.getAllFields(klass)
    fields.foreach { f =>
      val typ = f.getType
      if (!typ.isPrimitive && !klasses.contains(typ)) {
        if (!klasses.contains(typ)) {
          klasses.append(typ)
          getAllClasses(typ, klasses)
        }
      }

    }
    klasses.toArray
  }

}
