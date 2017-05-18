import java.io.{FileOutputStream, ObjectInputStream, ObjectOutputStream, StringBufferInputStream}

import org.openjdk.jol._
import org.openjdk.jol.datamodel.{X86_64_COOPS_DataModel, X86_64_DataModel}
import org.openjdk.jol.info.ClassLayout
import org.openjdk.jol.layouters.{CurrentLayouter, HotSpotLayouter}
import sun.misc.Unsafe

import scala.util.Random

class VectorWithNorms(val norm: Double, val values: Array[Double])

class SomeRandomClass(val a : Long, val b : Object, val c : Int, val d : Object)
class ExtendedClass(val e : Short, val f : Object, val g : Byte, val h : Object, i : Object, j : Object)
  //extends VectorWithNorms(i.toDouble, Array(0.0,0.1,0.2))
  extends SomeRandomClass(0xDEADBEEFL,i,0x8BADF00D,j)

class X(val a: Long, val b: Int, val c: Y, val d: Y) extends Serializable
class Y(val e: Int, val f: Char, val g: Array[Int], val h : Array[VectorWithNorms]) extends Serializable


object fpgaserialize {

  //@native def serializeNative(obj: AnyRef)

  def main(args: Array[String]): Unit = {

    val lay = new CurrentLayouter()
    val v = new VectorWithNorms(-3.7206620809969885439391603375E-103, Array(-5.87276176762981513250835401879E-21, -5.87276176762981513250835401879E-21, -5.87276176762981513250835401879E-21))
    val s = Seq.fill[VectorWithNorms](4)(new VectorWithNorms(Random.nextDouble,Array(Random.nextDouble, Random.nextDouble, Random.nextDouble)))
    val r = new SomeRandomClass(0x12345678L,v,0xABCDEF12,v)
    val e = new ExtendedClass(0xDEAD.toShort,r,0xAA.toByte,r,r,r)

    //val a = Array.fill[VectorWithNorms](128)(new VectorWithNorms(Random.nextDouble,Array(Random.nextDouble, Random.nextDouble, Random.nextDouble)))
    val a = Array.fill[Int](128)(0xDEADBEEF)

    val i = Array(0xA, 0xB, 0xC, 0xD)
    val j = Array(0xE, 0xF)
    val k = Array.fill(0)(new VectorWithNorms(Random.nextDouble,Array(Random.nextDouble, Random.nextDouble, Random.nextDouble)))
    val l = new Y(3,'l', i, k)
    val m = new Y(4,'m', j, k)
    val n = new X(1L, 2, l, m)

    /*println(ClassLayout.parseClass(i.getClass, lay).toPrintable)
    println(ClassLayout.parseClass(j.getClass, lay).toPrintable)
    println(ClassLayout.parseClass(k.getClass, lay).toPrintable)
    println(ClassLayout.parseClass(l.getClass, lay).toPrintable)
    println(ClassLayout.parseClass(m.getClass, lay).toPrintable)
    println(ClassLayout.parseClass(n.getClass, lay).toPrintable)*/

    val o = Seq[X](n,n,n,n).toArray

    val fo = new FileOutputStream("hi")
    val os = new ObjectOutputStream(fo)
    os.writeObject(o)


    println(ClassLayout.parseClass(o.getClass, lay).toPrintable)

    println(CompactLayouter.assembleCompactClassLayout(o))

  }
}
