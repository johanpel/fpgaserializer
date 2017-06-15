package org.tudelft.ewi.ce.fpgaserialize

import sun.misc.Unsafe

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

case class LayoutField(offset: Long, typ: Class[_], size: Long, name: String)

object CompactLayouter {
  private val u: Unsafe = UnsafeInstance.get
  private val addressBytes = u.addressSize.toLong

  def nameToSize(name: String): Byte = name match {
    case "double" => 8
    case "long" => 8
    case "float" => 4
    case "int" => 4
    case "short" => 2
    case "char" => 2
    case "byte" => 1
    case "boolean" => 1
    case _ => throw new Error("unable to extract byte size from type name \"" + name + "\".")
  }

  def getFields(klass: Class[_]): Array[LayoutField] = {
    var fields = ArrayBuffer.empty[LayoutField]
    if (klass.isArray) {
      val typ = klass.getComponentType
      if (typ.isPrimitive) {
        fields.append(LayoutField(24, klass, nameToSize(typ.getTypeName), "TypeArray"))
      } else {
        fields.append(LayoutField(24, klass, addressBytes, "ObjArray"))
      }
    }
    else {
      // Then fields of this class
      val thisFields = ReflectionHelper.getAllFields(klass)
      thisFields.foreach { f =>
        val offset = u.objectFieldOffset(f)
        val typ = f.getType
        val size = if (typ.isPrimitive) {
          nameToSize(f.getType.getTypeName).toLong
        }
        else {
          addressBytes
        }
        val name = f.getName
        fields.append(LayoutField(offset, typ, size, name))
      }
    }
    fields.sortBy(f => f.offset).toArray
  }

  def printFields(fields: Seq[LayoutField]): Unit = {
    fields.foreach(f => println(f"${f.offset}%4d: ${f.typ}%16s[${f.size}%4d]\t(${f.name})"))
  }
}
