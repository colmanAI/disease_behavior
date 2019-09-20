package il.ac.colman.disease

import java.time.{Duration, ZonedDateTime}

import il.ac.colman.disease

case class Visit(location: Location, start: ZonedDateTime, end: ZonedDateTime) {
  val duration: Duration = Duration.between(start, end)
}

object Visit {
  class Builder(private var start: ZonedDateTime) {
    private val locationBuilder: Location.Builder = new disease.Location.Builder()
    private var end: ZonedDateTime = _

    def clear(start: ZonedDateTime): Builder = {
      locationBuilder.clear()
      this.start = start
      end = start
      this
    }

    def add(dataPoint: DataPoint): Builder = {
      locationBuilder.add(dataPoint.position)
      end = dataPoint.dateTime
      this
    }

    def isEmpty: Boolean = locationBuilder.isEmpty
    def nonEmpty: Boolean = locationBuilder.nonEmpty
    def head: Position = locationBuilder.head
    def calcRadius(): Meters = locationBuilder.calcRadius()

    def build(): Visit = Visit(locationBuilder.build(), start, end)
  }
}
