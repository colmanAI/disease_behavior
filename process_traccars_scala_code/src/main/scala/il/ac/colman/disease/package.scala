package il.ac.colman

import java.time.{Duration, LocalTime}

import org.opensextant.geodesy.Geodetic2DPoint

package object disease {
  type DeviceId = Long
  type Email = String

  type Meters = Double
  type MetersPerSecond = Double

  type Latitude = Double
  type Longitude = Double
  type Position = Geodetic2DPoint

  val NANOS_IN_SECOND: Long = 1e9.toLong
  val KILOMETER_PER_HOUR: MetersPerSecond = 1000d / Duration.ofHours(1).getSeconds

  val NIGHT_START: LocalTime = LocalTime.of(2, 0, 0, 0)
  val NIGHT_END: LocalTime = LocalTime.of(4, 0, 0, 0)

  val HOME_RADIUS: Meters = 60
  val TRIP_MINIMUM_DISTANCE: Meters = 10
  val TRIP_MINIMUM_DURATION: Duration = Duration.ofMinutes(10)

  val MIN_WALKING_SPEED: MetersPerSecond = KILOMETER_PER_HOUR
  val MIN_DRIVING_SPEED: MetersPerSecond = 10 * KILOMETER_PER_HOUR
}
