package org.tudelft.ewi.ce.fpgaserialize

import sun.misc.Unsafe

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

abstract class CCLInstruction(val index: Int, val comment: String = "")

class CCLReference(index: Int, var offset: Long, var classIndex: Int, comment: String = "")
  extends CCLInstruction(index, comment) {
  override def toString: String = s"  REFE $offset, $classIndex"
}

class CCLClass(index: Int, val instanceSize: Long, comment: String = "")
  extends CCLInstruction(index, comment) {
  override def toString: String = s"CLAS $instanceSize"
}

class CCLEndOfClass(index: Int, comment: String = "")
  extends CCLInstruction(index, comment) {
  override def toString: String = s"EOCL"
}

class CCLTypeArray(index: Int, val componentSize: Long, comment: String = "")
  extends CCLInstruction(index, comment) {
  override def toString: String = s"ATYP $componentSize"
}

class CCLObjectArray(index: Int, var classIndex: Int, comment: String = "")
  extends CCLInstruction(index, comment) {
  override def toString: String = s"AOBJ $classIndex"
}

object CCLInstructionGenerator {

  private val u: Unsafe = UnsafeInstance.get
  private val addressBytes = u.addressSize.toLong

  def getSizeAndRefs(fields: Seq[LayoutField]): (Long, Seq[LayoutField]) = {
    val refonly = ListBuffer.empty[LayoutField]
    val totalSize = fields.last.offset + fields.last.size
    fields.foreach { f =>
      if (!f.typ.isPrimitive) {
        refonly.append(f)
      }
    }
    (totalSize, refonly.toList)
  }

  def generateInstructions(klassLayouts: Array[(Class[_], Array[LayoutField])]): Array[CCLInstruction] = {
    val instructions = ArrayBuffer.empty[CCLInstruction]
    val klassIndices = ArrayBuffer.empty[Int]
    var index = 0
    // We need to make two passes, first to put all instructions in the array
    // Second pass to update all the class pointers after they've been determined
    klassLayouts.indices.foreach { kl =>
      val klass = klassLayouts(kl)._1
      // Append the current index
      klassIndices.append(index)
      if (klassLayouts(kl)._2.nonEmpty) {
        val (size, refs) = getSizeAndRefs(klassLayouts(kl)._2)
        // Arrays
        if (klass.isArray) {
          val comp = klassLayouts(kl)._2(0)
          if (klassLayouts(kl)._1.getComponentType.isPrimitive) {
            instructions.append(new CCLTypeArray(index, comp.size, s"${comp.typ.getName}"))
            index += 1
          } else {
            val klassIndex = klassLayouts.indexWhere(g => g._1 == klass.getComponentType)
            instructions.append(new CCLObjectArray(index, klassIndex, s"${comp.typ.getName}"))
            index += 1
          }
          // InstanceKlass
        } else {
          val alignedsize = if (size % 8 == 0) size else size + 8 - (size % 8)
          instructions.append(new CCLClass(index, alignedsize, klass.getName))
          index += 1
          refs.foreach { f =>
            val klassIndex = klassLayouts.indexWhere(g => g._1 == f.typ)
            instructions.append(new CCLReference(index, f.offset, klassIndex, s"Ref to ${klassLayouts(klassIndex)._1.getName}"))
            index += 1
          }
        }
        instructions.append(new CCLEndOfClass(index, "End of class"))
        index += 1
      }
    }
    // A pass over all reference instructions to update the class pointer
    instructions foreach {
      case instr: CCLReference =>
        instr.classIndex = klassIndices(instr.classIndex)
      case instr: CCLObjectArray =>
        instr.classIndex = klassIndices(instr.classIndex)
      case _ =>
    }
    instructions.toArray
  }

  def convertInstructionsToVHDL(instructions: Array[CCLInstruction]): String = {
    val s: StringBuffer = new StringBuffer(512)
    var index = 0
    instructions.foreach {i =>
      val append = i match {
        case c : CCLClass       => f"$index%4d => " + """"""" + f"${c.instanceSize.toBinaryString.toInt}%032d"                                                         + """",""" + f" --${c.index}%4d: $c%-16s # ${c.comment}\n"
        case c : CCLReference   => f"$index%4d => " + """"""" + f"100" + f"${c.offset.toBinaryString.toInt}%021d"        + f"${c.classIndex.toBinaryString.toInt}%08d" + """",""" + f" --${c.index}%4d: $c%-16s # ${c.comment}\n"
        case c : CCLObjectArray => f"$index%4d => " + """"""" + f"101" + f"${addressBytes.toBinaryString.toInt}%021d"    + f"${c.classIndex.toBinaryString.toInt}%08d" + """",""" + f" --${c.index}%4d: $c%-16s # ${c.comment}\n"
        case c : CCLTypeArray   => f"$index%4d => " + """"""" + f"111" + f"${c.componentSize.toBinaryString.toInt}%021d" + f"${0}%08d"                                 + """",""" + f" --${c.index}%4d: $c%-16s # ${c.comment}\n"
        case c : CCLEndOfClass  => f"$index%4d => " + """"""" + f"110" + f"000000000000000000000"                        + f"00000000"                                 + """",""" + f" --${c.index}%4d: $c%-16s # ${c.comment}\n"
        case _ => f"ERROR"
      }
      s.append(append)
      index = index + 1;
    }
    s.toString
  }

  def convertInstructionsToString(instructions: Array[CCLInstruction]): String = {
    val s: StringBuffer = new StringBuffer(512)
    instructions.foreach { i =>
      s.append(f"${i.index}%4d: $i%-16s # ${i.comment}\n")
    }
    s.toString
  }

  def generateCompactClassLayoutInstructions(obj: AnyRef): Array[CCLInstruction] = {
    val klasses = ReflectionHelper.getAllClasses(obj.getClass)
    // Somehow we need to explicitly define this type:
    val klassLayouts: Array[(Class[_], Array[LayoutField])] = klasses.map(k => (k, CompactLayouter.getFields(k)))
    generateInstructions(klassLayouts)
  }
}
