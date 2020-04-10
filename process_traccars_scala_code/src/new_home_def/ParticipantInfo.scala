package new_home_def

import il.ac.colman.disease.{DataPoint, Latitude, Longitude, Position}
import org.opensextant.geodesy.{Geodetic2DPoint, Latitude => GeodeticLatitude, Longitude => GeodeticLongitude}
//import scala.collection.immutable.RedBlackTree.Tree
import scala.collection.immutable.{RedBlackTree => RB}
import scala.collection.mutable.ListBuffer

/**
  * Created by djoff on 29/03/2019.
  */
class ParticipantInfo(_id: Long, _exp: Int) {
  private val dataPoints: ListBuffer[DataPoint] = new ListBuffer[DataPoint]

  val id = _id
  val experiment = _exp

  private val lons: ListBuffer[Longitude] = new ListBuffer[Longitude]
  private val lats: ListBuffer[Latitude] = new ListBuffer[Latitude]

  private var geodeticMedian: Position = _
  private var geodeticMean: Position = _

  def get_latitude_mean : Double =  {
    this.mean(this.lats)
  }
  def get_latitude_median : Double = {
    this.medianCalculator(this.lats)
  }
  def get_longitude_mean : Double = {
    this.mean(this.lons)
  }
  def get_longitude_median : Double = {
    this.medianCalculator(this.lons)
  }

  def medianCalculator(seq: Seq[Double]): Double= {
    //In order if you are not sure that 'seq' is sorted
    val sortedSeq = seq.sortWith(_ < _)

    if (seq.size % 2 == 1) sortedSeq(sortedSeq.size / 2)
    else {
      val (up, down) = sortedSeq.splitAt(seq.size / 2)
      (up.last + down.head) / 2
    }
  }

  def mean(seq: Seq[Double]): Double = {
    seq.foldLeft((0.0, 1)) ((acc, i) => ((acc._1 + (i - acc._1) / acc._2), acc._2 + 1))._1
  }

  def getDataPoints : List[DataPoint] ={
    this.dataPoints.toList
  }

  def geoMedian: Position = {
    if (geodeticMedian == null) {
      geodeticMedian = new Geodetic2DPoint(new GeodeticLongitude(Math.toRadians(get_longitude_median)),
        new GeodeticLatitude(Math.toRadians(get_latitude_median)))
    }
    geodeticMedian
  }

  def geoMean: Position ={
    if (geodeticMean == null) {
      geodeticMean = new Geodetic2DPoint(new GeodeticLongitude(Math.toRadians(get_longitude_mean)),
        new GeodeticLatitude(Math.toRadians(get_latitude_mean)))
    }
    geodeticMean
  }

  def addDataPoint(dataPoint: DataPoint):Unit = {
    this.dataPoints += dataPoint
    this.lons += dataPoint.rawLongitude
    this.lats += dataPoint.rawLatitude
  }
}

/**
  * calculates median and mean online
  */
//class OnlineStatCalc() {
//  private var n :Int= 0
//  private var sum : Double = 0
//  private var minHeap = new PriorityQueue[Double]((x:Double, y:Double) => y compare x)
//  private var maxHeap = new PriorityQueue[Double]((x:Double, y:Double) => x compare y)
//
//  var mean: Double = 0
//  var median: Double = 0
//
//  def addNumber(number: Double):Unit = {
//    if (minHeap.isEmpty || number < minHeap.peek()) {
//      minHeap.add(number)
//    } else {
//      maxHeap.add(number)
//    }
//    this.sum += number
//    this.n += 1
//    this.updateStat()
//  }
//
//  private def updateStat() {
//    mean = sum/n
//    val minSize = minHeap.size()
//    val maxSize = maxHeap.size()
//    if (minSize > maxSize +1) {
//      maxHeap.add(minHeap.poll())
//    } else if (maxSize > minSize +1) {
//      minHeap.add(maxHeap.poll())
//    }
//
//    if (minSize == maxSize) {
//      median = (maxHeap.peek() + minHeap.peek())/2
//    } else if (minSize > maxSize) {
//      median = minHeap.peek()
//    } else {
//      median = maxHeap.peek()
//    }
//  }
//}
