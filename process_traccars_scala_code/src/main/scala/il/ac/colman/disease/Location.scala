package il.ac.colman.disease

import org.opensextant.geodesy.Geodetic2DArc

import scala.collection.mutable.ListBuffer

case class Location(center: Position, radius: Meters, positions: List[Position]) {
  def distanceFromCenter(position: Position): Meters = new Geodetic2DArc(center, position).getDistanceInMeters
  def distanceFromEdge(position: Position): Meters = Math.max(0, distanceFromCenter(position) - radius)

  def distanceBetweenCenters(other: Location): Meters = new Geodetic2DArc(center, other.center).getDistanceInMeters
  def distanceBetweenEdges(other: Location): Meters = Math.max(0, distanceBetweenCenters(other) - radius - other.radius)

  def +(other: Location): Location = {
    if(positions.size >= other.positions.size) {
      new Location.Builder(this).add(other).build()
    } else {
      new Location.Builder(other).add(this).build()
    }
  }
}

  object Location {
  class Builder(locationOpt: Option[Location]) {
    def this() = this(None)
    def this(location: Location) = this(Some(location))

    private val positionsBuilder: ListBuffer[Position] = new ListBuffer[Position]
    private var center: Position = _

    locationOpt match {
      case Some(location) => {
        positionsBuilder ++= location.positions
        center = location.center
      }
      case None => ()
    }

    def isEmpty: Boolean = positionsBuilder.isEmpty
    def nonEmpty: Boolean = positionsBuilder.nonEmpty
    def head: Position = positionsBuilder.head

    def add(position: Position): Builder = {
      if(positionsBuilder.isEmpty) {
        center = position
      }
      position +=: positionsBuilder
      val arc = new Geodetic2DArc(center, position)
      center = new Geodetic2DArc(center, arc.getDistanceInMeters / positionsBuilder.size, arc.getForwardAzimuth).getPoint2
      this
    }

    def add(location: Location): Builder = {
      location.positions.foreach(add)
      this
    }

    def calcRadius(): Meters = {
      positionsBuilder.map(p => new Geodetic2DArc(center, p).getDistanceInMeters).max
    }

    def clear(): Builder = {
      positionsBuilder.clear()
      center = null
      this
    }

    def build(): Location = Location(center, calcRadius(), positionsBuilder.toList)
  }
}
