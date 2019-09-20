import org.opensextant.geodesy._

import scala.collection.mutable.ListBuffer

/**
  * Created by nirle on 7/2/2017.
  */
class Location private(id: Long, stdDev: Double, private val geoCircle: Geodetic2DCircle, private val geoPositions: List[Geodetic2DPoint]) {
  import Location._

  val size: Int = geoPositions.size
  val radius: Double = geoCircle.getRadius

  def distanceTo(geoPosition: Geodetic2DPoint): Double = geoPositions.map(gp => new Geodetic2DArc(gp, geoPosition).getDistanceInMeters).min
  def distanceTo(location: Location): Double = distanceTo(location.geoCircle.getCenter)

  def +(geoPosition: Geodetic2DPoint): Location = {
    // nextId isn't incremented here, the new location is only temporary
    this ++ new Location(id, 0, new Geodetic2DCircle(geoPosition, 0), List(geoPosition))
  }

  def ++(other: Location): Location = {
    val positions: List[Geodetic2DPoint] = geoPositions ++ other.geoPositions
    val arc = new Geodetic2DArc(geoCircle.getCenter, other.geoCircle.getCenter)
    val centroid = new Geodetic2DArc(geoCircle.getCenter, arc.getDistanceInMeters * other.size / positions.size, arc.getForwardAzimuth).getPoint2
    val meanDistance = positions.map(gp => frameOfReference.orthodromicDistance(centroid, gp)).sum / positions.size
    val stdDev = Math.sqrt(positions.map(gp => Math.pow(frameOfReference.orthodromicDistance(centroid, gp) - meanDistance, 2)).sum / positions.size)
    // nextId isn't incremented here, the new location is an expansion of this location
    new Location(id, stdDev, new Geodetic2DCircle(centroid, meanDistance), positions)
  }

  def merge(locations: ListBuffer[Location]): Location = {
    for(l <- locations) {
      if(distanceTo(l) <= geoCircle.getRadius + l.geoCircle.getRadius) {
        val mergedLocation = l ++ this
        locations -= l
        locations += mergedLocation
        return mergedLocation
      }
    }
    locations += this
    this
  }

  override def toString: String = {
    s"Location(" +
      s"id: $id, " +
      s"center: within ${geoCircle.getRadius} meters from ${geoCircle.getCenter.toString(6)}, " +
      s"count: $size, " +
      s"positions: ${geoPositions.map(_.toString(6)).mkString(",")})"
  }
}

object Location {
  val frameOfReference: FrameOfReference = new FrameOfReference

  private var nextId: Long = 1
  private def getNextId: Long = {
    val id = nextId
    nextId += 1
    id
  }

  def apply(geoPosition: Geodetic2DPoint) = new Location(getNextId, 0, new Geodetic2DCircle(geoPosition, 0), List(geoPosition))
}
