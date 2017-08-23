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
import org.broadinstitute.gatk.utils.genotyper.ReadLikelihoods
import org.broadinstitute.gatk.utils.haplotype.Haplotype
import org.broadinstitute.gatk.utils.sam.GATKSAMRecord
import org.broadinstitute.gatk.nativebindings.pairhmm.{HaplotypeDataHolder, ReadDataHolder}

import scala.collection.mutable.ArrayBuffer

class SomeClass(val a: Int, val b: Int, val c: Array[Int])

class Person(val name: String,
             var age: Int)

class Employee(var accountNumber: Int,
               var salary: Int,
               var department: String,
               var hoursPerDay: Array[Int],
               name: String,
               age: Int)
  extends Person(name, age)

class SimpleImage(var pixels: Array[Int], var w: Int, var h: Int)

object fpgaserialize {

  System.loadLibrary("fpgaserializer")

  @native def test(obj: Any): Unit
  @native def raiseSalary(obj: Any): Unit
  @native def testPictures(in: Array[SimpleImage], out: Array[SimpleImage]): Unit
  @native def testPicturesJNI(in: Array[SimpleImage], out: Array[SimpleImage]): Unit

  @native def testKMeansRecklessSerialized(in: Array[KMVector], dims : Int, centers : Int, mode : Int, out: Array[Int]): Long
  @native def testKMeansReckless(in: Array[KMVector], dims : Int, centers : Int, mode : Int, out: Array[Int]): Long
  @native def testKMeansJNISerialized(in: Array[KMVector], dims : Int, centers : Int, mode : Int, out: Array[Int]): Long
  @native def testKMeansJNI(in: Array[KMVector], dims : Int, centers : Int, mode : Int, out: Array[Int]): Long
  @native def testKMeansUnsafe(in: Long, objCnt: Int, dims : Int, centers : Int, mode : Int, out: Array[Int]): Long

  var hi: Person = new Person("hi", 10)

  val curLayouter = new CurrentLayouter()

  var t0: Long = System.nanoTime()
  var t1: Long = System.nanoTime()

  def getPixels(ImageName: String): SimpleImage = {
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

  def writePixels(ImageName: String, img: SimpleImage): Unit = {
    val bufferedImage = new BufferedImage(img.w, img.h, BufferedImage.TYPE_INT_ARGB)
    bufferedImage.setRGB(0, 0, img.w, img.h, img.pixels, 0, img.w)
    val fos = new FileOutputStream(ImageName.replace("jpg", "png"))
    ImageIO.write(bufferedImage, "png", fos)
    fos.close()
  }

  def clas_atyp_aobj(): Unit = {
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

  def testString(): Unit = {

    val n = Math.pow(2, 6).toInt
    val s = Math.pow(2, 20).toInt
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
    kryo.register(classOf[Array[String]], 0)
    kryo.register(classOf[String], 1)

    val u = UnsafeInstance.get
    val baosb = new FileOutputStream("testb.ser")
    val addr = u.allocateMemory(alloc)
    val output = new UnsafeMemoryOutput(addr, alloc)
    t0 = System.nanoTime()
    kryo.writeObject(output, b)
    t1 = System.nanoTime()
    u.freeMemory(addr)
    //val fosb = new FileOutputStream("testb.ser")
    //fosa.write(baosb.toByteArray)
    println((t1 - t0).toDouble * 1E-9)
  }

  def testEmployees(): Unit = {
    RecklessGenerator(classOf[Array[Employee]], "employees")

    val empCount = Math.pow(2, 16).toInt

    Random.setSeed(0)
    val emps = Array.fill(empCount)(new Employee(Random.nextInt,
      Random.nextInt,
      Random.nextString(16),
      Array(Random.nextInt, Random.nextInt, Random.nextInt, Random.nextInt, Random.nextInt, Random.nextInt, Random.nextInt),
      Random.nextString(16),
      Random.nextInt)
    )

    Random.setSeed(0)
    val emps2 = Array.fill(empCount)(new Employee(Random.nextInt,
      Random.nextInt,
      Random.nextString(16),
      Array(Random.nextInt, Random.nextInt, Random.nextInt, Random.nextInt, Random.nextInt, Random.nextInt, Random.nextInt),
      Random.nextString(16),
      Random.nextInt)
    )

    t0 = System.nanoTime()
    for (i <- Range(0, 64)) {
      raiseSalaryJVM(emps)
    }
    t1 = System.nanoTime()
    println((t1 - t0).toDouble * 1E-9)


    t0 = System.nanoTime()
    for (i <- Range(0, 64)) {
      raiseSalary(emps2)
    }
    t1 = System.nanoTime()
    println((t1 - t0).toDouble * 1E-9)
  }

  def testPictures(): Unit = {
    val maxSize = Math.pow(2, 24).toInt

    //for (i <- Range(0,20)) {

    //val numObjects = Math.pow(2, i).toInt
    //val reps = 1
    //val w = Math.sqrt(maxSize / numObjects).toInt
    //val h = Math.sqrt(maxSize / numObjects).toInt

    //print(s"$numObjects, $w, $h, ")

    RecklessGenerator(classOf[Array[SimpleImage]], "pictures")

    val dataFolder = new File("data/lfw")
    val files = dataFolder.listFiles().filter(f => f.isFile)
    val alpha = 80
    val beta = 98
    val batch = files.filter(f => f.getName.charAt(0).asInstanceOf[Int] < alpha && f.getName.charAt(1).asInstanceOf[Int] < beta)

    //t0 = System.nanoTime()
    //destReckless.indices.foreach(i => writePixels("data/reckless/" + files(i).getName,imagesReckless(i)))
    //t1 = System.nanoTime()
    //println((t1 - t0).toDouble * 1E-9)

    var imagesJNI = batch.map(f => getPixels(f.getPath))
    var destJNI = batch.map(f => getPixels(f.getPath))
    //var imagesJNI = Array.fill(numObjects)(new SimpleImage(new Array[Int](w * h), w, h))
    //var destJNI = Array.fill(numObjects)(new SimpleImage(new Array[Int](w * h), w, h))

    //        for (i <- Range(0, reps)) {
    //          System.out.flush()
    testPicturesJNI(imagesJNI, destJNI)
    //print("\n")
    //        }
    //print(f"${(t1 - t0).toDouble * 1E-9}%16.8f \n")
    //print("\n")

    //t0 = System.nanoTime()
    destJNI.indices.foreach(i => writePixels("data/jni/" + batch(i).getName, destJNI(i)))
    //t1 = System.nanoTime()
    //println((t1 - t0).toDouble * 1E-9)

    var imagesReckless = batch.map(f => getPixels(f.getPath))
    var destReckless = batch.map(f => getPixels(f.getPath))
    //var imagesReckless = Array.fill(numObjects)(new SimpleImage(new Array[Int](w * h), w, h))
    //var destReckless = Array.fill(numObjects)(new SimpleImage(new Array[Int](w * h), w, h))
    //t1 = System.nanoTime()
    //println((t1 - t0).toDouble * 1E-9)

    //        for (i <- Range(0, reps)) {
    //          System.out.flush()
    testPictures(imagesReckless, destReckless)
    //        }
    destReckless.indices.foreach(i => writePixels("data/reckless/" + batch(i).getName, destReckless(i)))
    //print(f"${(t1 - t0).toDouble * 1E-9}%16.8f ")

    /*
    var same = true
    for (i <- destJNI.indices) {
      for (j <- destJNI(i).pixels.indices) {
        same = destJNI(i).pixels(j) == destReckless(i).pixels(j)
      }
    }
    print(same)
    */

    imagesJNI = null
    destJNI = null
    imagesReckless = null
    destReckless = null
    System.gc()
    print("\n")
  }

  def testKMeans(): Unit = {
    RecklessGenerator(classOf[Array[KMVector]], "kmvector")
    RecklessGenerator(classOf[Array[HaplotypeDataHolder]],"pairhmm")

    val repeats = 16

    for (e <- Range(4,20)) {
      System.gc()
      val objects = Math.pow(2, e).toInt
      val dims = 32
      val centers = 16

      print(f"$objects%6d, $dims%4d, $centers%2d, $repeats%2d, ")

      var to = System.nanoTime()

      var ti = 0L

      val MODE = 1

      /* JNI SERIALIZED */
      Random.setSeed(0)
      var vecsJNISerialized = Array.fill[KMVector](objects)(new KMVector(dims, Array.fill[Float](dims)(Random.nextFloat)))
      ti = 0
      to = System.nanoTime()

      for (r <- Range(0, repeats))
        ti = ti + testKMeansJNISerialized(vecsJNISerialized, dims, centers, MODE, null)

      to = System.nanoTime() - to
      print(f"$to%10d, $ti%10d, ")

      vecsJNISerialized = null
      System.gc()

      /* UNSAFE SERIALIZED*/
      Random.setSeed(0)
      var vecsUnsafe = Array.fill[KMVector](objects)(new KMVector(dims, Array.fill[Float](dims)(Random.nextFloat)))
      ti = 0
      to = System.nanoTime()

      for (r <- Range(0, repeats)) {
        val u = UnsafeInstance.get
        val ua = u.allocateMemory(objects * dims * 4)
        vecsUnsafe.indices.foreach {
          v =>
            vecsUnsafe(v).values.indices.foreach { i =>
              u.putFloat(ua + (v * dims + i) * 4, vecsUnsafe(v).values(i))
            }
        }
        ti = ti + testKMeansUnsafe(ua, objects, dims, centers, MODE, null)
        u.freeMemory(ua)
      }

      to = System.nanoTime() - to
      print(f"$to%10d, $ti%10d, ")

      vecsUnsafe = null
      System.gc()

      /* RECKLESS SERIALIZED */
      Random.setSeed(0)
      var vecsRecklessSerialized = Array.fill[KMVector](objects)(new KMVector(dims, Array.fill[Float](dims)(Random.nextFloat)))
      ti = 0
      to = System.nanoTime()

      for (r <- Range(0, repeats))
        ti = ti + testKMeansRecklessSerialized(vecsRecklessSerialized, dims, centers, MODE, null)

      to = System.nanoTime() - to
      print(f"$to%10d, $ti%10d, ")

      vecsRecklessSerialized = null
      System.gc()

      /* JNI */
      Random.setSeed(0)
      var vecsJNI = Array.fill[KMVector](objects)(new KMVector(dims, Array.fill[Float](dims)(Random.nextFloat)))
      ti = 0
      to = System.nanoTime()

      for (r <- Range(0, repeats))
        ti = ti + testKMeansJNI(vecsJNI, dims, centers, MODE, null)

      to = System.nanoTime() - to
      print(f"$to%10d, $ti%10d, ")

      vecsJNI = null
      System.gc()

      /* RECKLESS */
      Random.setSeed(0)
      var vecsReckless = Array.fill[KMVector](objects)(new KMVector(dims, Array.fill[Float](dims)(Random.nextFloat)))
      ti = 0
      to = System.nanoTime()

      for (r <- Range(0, repeats))
        ti = ti + testKMeansReckless(vecsReckless, dims, centers, MODE, null)

      to = System.nanoTime() - to
      print(f"$to%10d, $ti%10d, ")

      vecsReckless = null
      System.gc()

      /* JVM Scala */
      /*
      Random.setSeed(0)
      var vecsJVM     = Array.fill[KMVector](objects)(new KMVector(dims, Array.fill[Float](dims)(Random.nextFloat)))

      to = System.nanoTime()

      var i = 1
      for (r <- Range(0, repeats)) {
        var assignments = Array.fill[Int](objects)(0)
        var centroids = Array.fill[KMVector](centers)(null)
        (0 until centers).foreach(i => centroids(i) = vecsJVM(i).copy)
        var delta = Float.PositiveInfinity
        i = 1
        //KMVector.printIteration(objects, centers, vecsJVM, assignments, centroids)
        do {
          delta = KMVector.assignPoints(objects, centers, vecsJVM, assignments, centroids)
          KMVector.moveCentroids(objects, centers, vecsJVM, assignments, centroids)
          //KMVector.printIteration(objects, centers, vecsJVM, assignments, centroids)
          i = i + 1
        } while (delta / centers > 0.001f)
      }

      to = System.nanoTime() - to
      print(f"$to%10d, ")
      */

      /* JVM Java */
      Random.setSeed(0)
      var vecsJVMJ = Array.fill[KMVectorJava](objects)(new KMVectorJava(dims, Array.fill[Float](dims)(Random.nextFloat)))

      to = System.nanoTime()

      KMVectorJava.cluster(vecsJVMJ,objects,centers,repeats,0.001f)

      to = System.nanoTime() - to
      print(f"$to%10d, ")

      vecsJVMJ = null
      System.gc()

      print("\n")
    }
  }

  def raiseSalaryJVM(emps: Array[Employee]): Unit = {
    emps.foreach(e => e.salary += 1000)
    emps(0).age = 50
  }

  def main(args: Array[String]): Unit = {
    //testEmployees()
    //testFigures()
    testKMeans()
    //RecklessGenerator(classOf[GATKSAMRecord], "pairhmm")
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
