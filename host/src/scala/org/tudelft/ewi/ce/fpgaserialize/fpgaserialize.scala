package org.tudelft.ewi.ce.fpgaserialize

import org.openjdk.jol.info.ClassLayout
import org.openjdk.jol.layouters.CurrentLayouter

class VectorWithNorms(val norm: Double, val values: Array[Double])

class SomeRandomClass(val a : Long, val b : Object, val c : Int, val d : Object)
class ExtendedClass(val e : Short, val f : Object, val g : Byte, val h : Object, i : Object, j : Object)
  extends SomeRandomClass(0xDEADBEEFL,i,0x8BADF00D,j)
class X(val a: Long, val b: Int, val c: Y, val d: Y) extends Serializable
class Y(val e: Int, val f: Char, val g: Array[Int], val h : Array[VectorWithNorms]) extends Serializable


class leaf (val a: Int, val b: Int, val c: Int)
class root (val d: leaf, val e: Array[Byte])

object fpgaserialize {

  def main(args: Array[String]): Unit = {

    val lay = new CurrentLayouter()
    /*
    val v = new VectorWithNorms(-3.7206620809969885439391603375E-103, Array(-5.87276176762981513250835401879E-21, -5.87276176762981513250835401879E-21, -5.87276176762981513250835401879E-21))
    val s = Seq.fill[VectorWithNorms](4)(new VectorWithNorms(Random.nextDouble,Array(Random.nextDouble, Random.nextDouble, Random.nextDouble)))
    val r = new SomeRandomClass(0x12345678L,v,0xABCDEF12,v)
    val e = new ExtendedClass(0xDEAD.toShort,r,0xAA.toByte,r,r,r)


    val a = Array.fill[Int](128)(0xDEADBEEF)
    val i = Array(0xA, 0xB, 0xC, 0xD)
    val j = Array(0xE, 0xF)
    val k = Array.fill(0)(new VectorWithNorms(Random.nextDouble,Array(Random.nextDouble, Random.nextDouble, Random.nextDouble)))
    val l = new Y(3,'l', i, k)
    val m = new Y(4,'m', j, k)
    val n = new X(1L, 2, l, m)
    val o = Seq[X](n,n,n,n).toArray
    println(ClassLayout.parseClass(o.getClass, lay).toPrintable)
    println(CompactLayouter.convertInstructionsToVHDL(CompactLayouter.generateCompactClassLayoutInstructions(o)))
*/
    val a = new leaf(1,2,3)
    val b = Array.fill[Int](8)(0xAABBCCDD)
    val c = new root(a,b)
    println(CompactLayouter.convertInstructionsToVHDL(CompactLayouter.generateCompactClassLayoutInstructions(c)))
    SerializerSimulator.serializeObject(c)

    println(ClassLayout.parseClass(a.getClass, lay).toPrintable)
    println(ClassLayout.parseClass(b.getClass, lay).toPrintable)
    println(ClassLayout.parseClass(c.getClass, lay).toPrintable)

/*
    val a = Array.fill[leaf](4)(new leaf(1,2,3))
    println(CompactLayouter.convertInstructionsToVHDL(CompactLayouter.generateCompactClassLayoutInstructions(a)))
    */
  }
}
