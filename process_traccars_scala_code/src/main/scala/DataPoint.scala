import java.time.ZonedDateTime

import org.opensextant.geodesy.Geodetic2DPoint

/**
  * Created by nirle on 3/30/2017.
  */
case class DataPoint(
                      deviceId: Long,
                      email: String,
                      dateTime: ZonedDateTime,
                      speed: Double,
                      distance: Double,
                      totalDistance: Double,
                      position: Geodetic2DPoint,
                      rawLatitude: Double,
                      rawLongitude: Double) {
  def dayPart: DayPart =  if(isDaytime) Day else  Night

  def isDaytime = !(isNightBefore || isNightAfter)

  def isNightBefore = dateTime.getHour < 6

  def isNightAfter = dateTime.getHour >= 22
}
