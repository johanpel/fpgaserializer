package org.tudelft.ewi.ce.fpgaserialize

import java.io.{ByteArrayOutputStream, FileOutputStream, ObjectOutputStream}
import java.nio.ByteBuffer

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Output, UnsafeMemoryOutput, UnsafeOutput}
import org.openjdk.jol.info.ClassLayout
import org.openjdk.jol.layouters.CurrentLayouter

import scala.util.Random

class someClass(val a: Int, val b: Int, val c: Array[Int])

object fpgaserialize {

  def clas_atyp_aobj() : Unit = {
    val lay = new CurrentLayouter()
    
    val a0 = Array[Int](0x33333333, 0x33333333)
    val a1 = Array[Int](0x66666666)
    val a2 = Array[Int](0x99999999, 0x99999999, 0x99999999)
    val a3 = Array[Int](0xCCCCCCCC, 0xCCCCCCCC, 0xCCCCCCCC, 0xCCCCCCCC)

    val sc0 = new someClass(0x11111111, 0x22222222, a0)
    val sc1 = new someClass(0x44444444, 0x55555555, a1)
    val sc2 = new someClass(0x77777777, 0x88888888, a2)
    val sc3 = new someClass(0xAAAAAAAA, 0xBBBBBBBB, a3)

    val oa0 = Array[someClass](sc0, sc1)
    val oa1 = Array[someClass](sc2, sc3)
    val ooa = Array[Array[someClass]](oa0, oa1)

    println(CompactLayouter.convertInstructionsToVHDL(CompactLayouter.generateCompactClassLayoutInstructions(ooa)))
    SerializerSimulator.serializeObject(ooa)
    SerializerSimulator.serializeObject(oa0)
    SerializerSimulator.serializeObject(sc0)
    SerializerSimulator.serializeObject(a0)
    SerializerSimulator.serializeObject(sc1)
    SerializerSimulator.serializeObject(a1)
    SerializerSimulator.serializeObject(oa1)
    SerializerSimulator.serializeObject(sc2)
    SerializerSimulator.serializeObject(a2)
    SerializerSimulator.serializeObject(sc3)
    SerializerSimulator.serializeObject(a3)

    println(ClassLayout.parseClass(ooa.getClass, lay).toPrintable)
    println(ClassLayout.parseClass(classOf[someClass], lay).toPrintable)
    println(ClassLayout.parseClass(classOf[Array[Int]], lay).toPrintable)
    println(ClassLayout.parseClass(sc3.getClass, lay).toPrintable)
    println(ClassLayout.parseClass(a3.getClass, lay).toPrintable)
  }

  def testString() : Unit = {

    val n = Math.pow(2,6).toInt
    val s = Math.pow(2,20).toInt
    val alloc = n * s * 4
    Random.setSeed(0)
    val a = Array.fill[String](n)(Random.nextString(s))
    Random.setSeed(0)
    val b = Array.fill[String](n)(Random.nextString(s))
    //println(CompactLayouter.convertInstructionsToVHDL(CompactLayouter.generateCompactClassLayoutInstructions(a)))
    //SerializerSimulator.serializeObject(a)

    val baosa = new ByteArrayOutputStream(alloc)
    val oos = new ObjectOutputStream(baosa)
    var t0 = System.nanoTime()
    oos.writeObject(a)
    var t1 = System.nanoTime()
    val fosa = new FileOutputStream("testa.ser")
    fosa.write(baosa.toByteArray)
    println((t1 - t0).toDouble * 10E-9)

    val kryo = new Kryo()
    kryo.register(classOf[Array[String]],0)
    kryo.register(classOf[String],1)

    val u = UnsafeInstance.get
    val baosb = new FileOutputStream("testb.ser")
    val addr = u.allocateMemory(alloc)
    val output = new UnsafeMemoryOutput(addr,alloc)
    t0 = System.nanoTime()
    kryo.writeObject(output, b)
    t1 = System.nanoTime()
    u.freeMemory(addr)
    //val fosb = new FileOutputStream("testb.ser")
    //fosa.write(baosb.toByteArray)
    println((t1 - t0).toDouble * 10E-9)
  }

  def main(args: Array[String]): Unit = {
    testString
  }
}
