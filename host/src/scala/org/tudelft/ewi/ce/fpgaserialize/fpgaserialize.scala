package org.tudelft.ewi.ce.fpgaserialize

import java.awt.color.ColorSpace
import java.awt.image.{BufferedImage, ColorConvertOp, DataBufferByte}
import java.io.{ByteArrayOutputStream, File, FileOutputStream, ObjectOutputStream}
import javax.imageio.ImageIO

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.{Output, UnsafeMemoryOutput, UnsafeOutput}
import org.openjdk.jol.info.ClassLayout
import org.openjdk.jol.layouters.CurrentLayouter

import scala.util.Random

class SomeClass(val a: Int, val b: Int, val c: Array[Int])

class Person(val name: String,
             var age : Int)

class Employee(var accountNumber : Int,
               var salary: Int,
               var department : String,
               var hoursPerDay : Array[Int],
               name : String,
               age : Int)
  extends Person(name,age)

class SimpleImage(var pixels : Array[Int], var w: Int, var h: Int)

object fpgaserialize {

  System.loadLibrary("fpgaserializer")

  @native def test(obj: Any) : Unit
  @native def raiseSalary(obj : Any) : Unit
  @native def testPictures(obj : Any) : Unit
  @native def testPicturesJNI(obj : Any) : Unit

  var hi : Person = new Person("hi",10)

  val curLayouter = new CurrentLayouter()

  var t0 = System.nanoTime()
  var t1 = System.nanoTime()

  def getPixels(ImageName: String) : SimpleImage = {
    val imgPath = new File(ImageName)
    val bufferedImage = ImageIO.read(imgPath)

    /*val cs = ColorSpace.getInstance(ColorSpace.CS_GRAY)
    val op = new ColorConvertOp(cs,null)
    val grayImage = op.filter(bufferedImage, null)
    */
    val grayImage = bufferedImage

    new SimpleImage(grayImage.getRGB(0, 0, grayImage.getWidth, grayImage.getHeight, null, 0, grayImage.getWidth),
      grayImage.getWidth,
      grayImage.getHeight
    )

    //val raster = grayImage.getRaster
    //val data : DataBufferByte = raster.getDataBuffer.asInstanceOf[DataBufferByte]
    //data.getData
  }

  def writePixels(ImageName: String, img : SimpleImage) = {
    val bufferedImage = new BufferedImage(img.w,img.h, BufferedImage.TYPE_INT_ARGB)
    bufferedImage.setRGB(0,0,img.w,img.h,img.pixels,0,img.w)
    val fos = new FileOutputStream(ImageName.replace("jpg", "png"))
    ImageIO.write(bufferedImage, "png", fos)
    fos.close()
  }

  def clas_atyp_aobj() : Unit = {
    val a0 = Array[Int](0x33333333, 0x33333333)
    val a1 = Array[Int](0x66666666)
    val a2 = Array[Int](0x99999999, 0x99999999, 0x99999999)
    val a3 = Array[Int](0xCCCCCCCC, 0xCCCCCCCC, 0xCCCCCCCC, 0xCCCCCCCC)

    val sc0 = new SomeClass(0x11111111, 0x22222222, a0)
    val sc1 = new SomeClass(0x44444444, 0x55555555, a1)
    val sc2 = new SomeClass(0x77777777, 0x88888888, a2)
    val sc3 = new SomeClass(0xAAAAAAAA, 0xBBBBBBBB, a3)

    val oa0 = Array[SomeClass](sc0, sc1)
    val oa1 = Array[SomeClass](sc2, sc3)
    val ooa = Array[Array[SomeClass]](oa0, oa1)

    println(CCLInstructionGenerator.convertInstructionsToVHDL(CCLInstructionGenerator.generateCompactClassLayoutInstructions(ooa)))
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

    println(ClassLayout.parseClass(ooa.getClass, curLayouter).toPrintable)
    println(ClassLayout.parseClass(classOf[SomeClass], curLayouter).toPrintable)
    println(ClassLayout.parseClass(classOf[Array[Int]], curLayouter).toPrintable)
    println(ClassLayout.parseClass(sc3.getClass, curLayouter).toPrintable)
    println(ClassLayout.parseClass(a3.getClass, curLayouter).toPrintable)
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
    t0 = System.nanoTime()
    oos.writeObject(a)
    t1 = System.nanoTime()
    val fosa = new FileOutputStream("testa.ser")
    fosa.write(baosa.toByteArray)
    println((t1 - t0).toDouble * 1E-9)

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
    println((t1 - t0).toDouble * 1E-9)
  }

  def testEmployees() : Unit = {
    RecklessGenerator(classOf[Array[Employee]], "employees")

    val empCount = Math.pow(2,16).toInt

    Random.setSeed(0)
    val emps = Array.fill(empCount)(new Employee(Random.nextInt,
      Random.nextInt,
      Random.nextString(16),
      Array(Random.nextInt,Random.nextInt,Random.nextInt,Random.nextInt,Random.nextInt,Random.nextInt,Random.nextInt),
      Random.nextString(16),
      Random.nextInt)
    )

    Random.setSeed(0)
    val emps2 = Array.fill(empCount)(new Employee(Random.nextInt,
      Random.nextInt,
      Random.nextString(16),
      Array(Random.nextInt,Random.nextInt,Random.nextInt,Random.nextInt,Random.nextInt,Random.nextInt,Random.nextInt),
      Random.nextString(16),
      Random.nextInt)
    )

    t0 = System.nanoTime()
    for (i <- Range(0,64)) {
      raiseSalaryJVM(emps)
    }
    t1 = System.nanoTime()
    println((t1 - t0).toDouble * 1E-9)


    t0 = System.nanoTime()
    for (i <- Range(0,64)) {
      raiseSalary(emps2)
    }
    t1 = System.nanoTime()
    println((t1 - t0).toDouble * 1E-9)
  }

  def raiseSalaryJVM(emps : Array[Employee]) : Unit = {
    emps.foreach(e => e.salary += 1000)
    emps(0).age = 50
  }

  def main(args: Array[String]): Unit = {

    //testEmployees()

    t0 = System.nanoTime()
    RecklessGenerator(classOf[Array[SimpleImage]], "pictures")
    t1 = System.nanoTime()
    println((t1 - t0).toDouble * 1E-9)

    t0 = System.nanoTime()
    val dataFolder = new File("data/lfw")
    val files = dataFolder.listFiles().filter(f => f.isFile) // && f.getName.charAt(0) == 'A')
    val imagesReckless = files.map(f => getPixels(f.getPath))
    val imagesJNI = files.map(f => getPixels(f.getPath))
    t1 = System.nanoTime()
    println((t1 - t0).toDouble * 1E-9)

    t0 = System.nanoTime()
    testPictures(imagesReckless)
    t1 = System.nanoTime()
    println("Reckless:" + ((t1 - t0).toDouble * 1E-9).toString)

    t0 = System.nanoTime()
    imagesReckless.indices.foreach(i => writePixels("data/reckless/" + files(i).getName,imagesReckless(i)))
    t1 = System.nanoTime()
    println((t1 - t0).toDouble * 1E-9)

    t0 = System.nanoTime()
    testPicturesJNI(imagesJNI)
    t1 = System.nanoTime()
    println("JNI:" + ((t1 - t0).toDouble * 1E-9).toString)

    t0 = System.nanoTime()
    imagesJNI.indices.foreach(i => writePixels("data/jni/" + files(i).getName,imagesJNI(i)))
    t1 = System.nanoTime()
    println((t1 - t0).toDouble * 1E-9)

  }
}

//println(ClassLayout.parseClass(classOf[Person], curLayouter).toPrintable)
//println(ClassLayout.parseClass(classOf[String], curLayouter).toPrintable)
//println(ClassLayout.parseClass(classOf[Array[Char]], curLayouter).toPrintable)

//val employeeFields = CompactLayouter.getFields(classOf[Employee])
//employeeFields.foreach(f => println(f"${f.offset}%4d ${f.size}%4d ${f.name}%24s : ${f.typ.getName}%s"))

//val stringFields = CompactLayouter.getFields(classOf[String])
//stringFields.foreach(f => println(f"${f.offset}%4d ${f.size}%4d ${f.name}%24s : ${f.typ.getName}%s"))

//println(ClassLayout.parseClass(classOf[Array[Char]], curLayouter).toPrintable)
//println(ClassLayout.parseClass(classOf[Array[Int]], curLayouter).toPrintable)
//println(ClassLayout.parseClass(classOf[Employee], curLayouter).toPrintable)

//println(CCLInstructionGenerator.convertInstructionsToVHDL(CCLInstructionGenerator.generateCompactClassLayoutInstructions("hi")))
//testString()
/*
var count = 1
for (n <- Range(0,32)) {
  System.gc()
  val amt = count << n
  print(f"$n%8d ${amt}%16d ")
  Random.setSeed(0)
  val names = Array.fill(amt)(Random.nextString(16))
  t0 = System.nanoTime()
  val tuples = names.map(s => (s, 1))
  t1 = System.nanoTime()
  print(f"${(t1 - t0).toDouble * 10E-9}%12.5f")
  //print(tuples.reduce((a,b) => (a._1,b._2)))
  print("\n")
}
*/
