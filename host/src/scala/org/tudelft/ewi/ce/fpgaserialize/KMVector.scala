package org.tudelft.ewi.ce.fpgaserialize

import scala.math
import scala.util.Random

class KMVector(val size : Int, val values : Array[Float]) {
  def clear(): Unit = {
    (0 until size).foreach(i => values(i) = 0.0f)
  }

  def add(a : KMVector) : Unit = {
    if (a.size == size) {
      (0 until size).foreach(i => values(i) += a.values(i))
    }
    else {
      throw new Error("Point dimensions must be the same.")
    }
  }

  def scale(factor : Float) : Unit = (0 until size).foreach(i => values(i) = values(i) / factor)

  override def toString: String = {
    var strBuf = new StringBuffer(10*size)
    values.foreach(f => strBuf.append(f"$f%16.14f, "))
    strBuf.toString
  }

  def copy() : KMVector = {
    val ret = new KMVector(size, Array.fill(size)(0.0f))
    ret.add(this)
    ret
  }
}

object KMVector {
  def calculateSquaredDistance(a: KMVector, b: KMVector): Float = {
    if (a.size == b.size) {
      var sum: Float = 0.0f
      (0 until a.size).foreach(i =>
        sum += (a.values(i) - b.values(i)) * (a.values(i) - b.values(i))
      )
      sum
    }
    else
      throw new Error("Point dimensions not equal.")
  }

  def printIteration(numPoints: Int, numCentroids: Int, points: Array[KMVector], assignments: Array[Int], centroids: Array[KMVector]) = {
    // Print centroids

    println(numCentroids)
    (0 until numCentroids).foreach { c =>
      print(f"$c%4d, ")
      print(centroids(c))
      print("\n")
    }
    // Print points
    print(numPoints + ":")
    (0 until numPoints).foreach { p =>
      print(f"${assignments(p)}%d, ")
      //print(points(p))
    }
    print("\n")
  }

  def assignPoints(numPoints: Int, numCentroids: Int, points: Array[KMVector], assignments: Array[Int], centroids: Array[KMVector]) : Float = {
    // Cluster assignment step
    var delta = 0.0f
    (0 until numPoints).foreach { p =>
      var tempDist = Float.PositiveInfinity
      var tempAssignment = assignments(p)
      (0 until numCentroids).foreach { c =>
        val curDist = KMVector.calculateSquaredDistance(points(p), centroids(c))
        if (curDist < tempDist) {
          assignments(p) = c
          tempDist = curDist
        }
      }
      if (assignments(p) != tempAssignment) {
        delta = delta + 1.0f
      }
    }
    delta
  }

  def moveCentroids(numPoints: Int, numCentroids: Int, points: Array[KMVector], assignments: Array[Int], centroids: Array[KMVector]) = {
    val pointsPerCentroid = Array.fill[Int](numCentroids)(0)

    // Reset centroid coordinates:
    centroids.foreach{c => c.clear()}

    (0 until numPoints).foreach {p =>
      centroids(assignments(p)).add(points(p)) // Add the coordinates of the point to this centroid
      pointsPerCentroid(assignments(p)) += 1 // Add the number of points to this centroid
    }

    // Divide all coordinates by number of points in centroid
    (0 until numCentroids).foreach { c =>
      centroids(c).scale(pointsPerCentroid(c))
    }
  }
}
