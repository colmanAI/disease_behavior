import java.time.temporal.ChronoUnit
import java.time.{Duration, ZonedDateTime}

import scala.collection.mutable.ListBuffer

/**
  * Created by nirle on 3/30/2017.
  */
object dataAggregator extends App {
  val SEPARATOR = '\t'

  val positions: Seq[DataPoint] = DAO.loadDataPoints()
  val devices: Seq[(Long, String)] = positions.map(p => (p.deviceId, p.email)).distinct

  val firstDay = positions.minBy(_.dateTime.getDayOfYear).dateTime.truncatedTo(ChronoUnit.DAYS)
  val lastDay = positions.maxBy(_.dateTime.getDayOfYear).dateTime.truncatedTo(ChronoUnit.DAYS)

  val dataBuffer = ListBuffer[((Long, ZonedDateTime, DayPart), AggregatedData)]()

  val duration6h = Duration.ofHours(6)
  val duration3h = Duration.ofHours(3)
  val duration2h = Duration.ofHours(2)
  val duration1h = Duration.ofHours(1)
  val duration30m = Duration.ofMinutes(30)

  var lastDeviceId = devices.head._1
  var lastDateTime = firstDay.withHour(6)
  var transmissions: Long = 0
  var gaps6h: Long = 0
  var gaps3h: Long = 0
  var gaps2h: Long = 0
  var gaps1h: Long = 0
  var gaps30m: Long = 0

  for (position <- positions) {
    val lastDayPart = if (lastDateTime.getHour >= 6 && lastDateTime.getHour < 22) Day else Night

    // check if last aggregation should be closed
    val isSameDevice = lastDeviceId == position.deviceId
    val isSameDayPart = lastDayPart == position.dayPart
    val isSameDay = lastDateTime.toLocalDate == position.dateTime.toLocalDate
    val isNightBefore = position.isNightBefore && lastDateTime.toLocalDate == position.dateTime.toLocalDate.minusDays(1)

    if (!isSameDevice || !isSameDayPart || !(isSameDay || isNightBefore)) {
      // aggregate gaps until end of last day part
      val lastDayPartEndHour = lastDayPart match { case Day => 22; case Night => 6 }
      val lastDayPartEnd = lastDateTime.withHour(lastDayPartEndHour)
      aggregateGaps(lastDayPartEnd)

      // save the aggregated data
      val lastDate = if (lastDateTime.getHour < 6) lastDateTime.truncatedTo(ChronoUnit.DAYS).minusDays(1) else lastDateTime.truncatedTo(ChronoUnit.DAYS)
      dataBuffer += (lastDeviceId, lastDate, lastDayPart) -> AggregatedData(transmissions, gaps6h, gaps3h, gaps2h, gaps1h, gaps30m)

      // reset aggregation for new day part
      val dayPartStartHour = if (position.dayPart == Day) 6 else 22
      lastDateTime = position.dateTime.withHour(dayPartStartHour)
      transmissions = 0
      gaps6h = 0
      gaps3h = 0
      gaps2h = 0
      gaps1h = 0
      gaps30m = 0
    }

    transmissions += 1
    aggregateGaps(position.dateTime)

    lastDeviceId = position.deviceId
    lastDateTime = position.dateTime
  }

  val lastDayPart = if (lastDateTime.getHour >= 6 && lastDateTime.getHour < 22) Day else Night

  // aggregate gaps until end of last day part
  val lastDayPartEndHour = lastDayPart match { case Day => 22; case Night => 6 }
  val lastDayPartEnd = lastDateTime.withHour(lastDayPartEndHour)
  aggregateGaps(lastDayPartEnd)

  // save the aggregated data
  val lastDate = if (lastDateTime.getHour < 6) lastDateTime.truncatedTo(ChronoUnit.DAYS).minusDays(1) else lastDateTime.truncatedTo(ChronoUnit.DAYS)
  dataBuffer += (lastDeviceId, lastDate, lastDayPart) -> AggregatedData(transmissions, gaps6h, gaps3h, gaps2h, gaps1h, gaps30m)

  val data = dataBuffer.toMap.withDefaultValue(AggregatedData())

  val dayParts = Seq(Day, Night)
  val features = Seq[(String, (AggregatedData) => Long)](
    ("transmissions", _.transmissions),
    ("6 hour gaps", _.gaps6h),
    ("3 hour gaps", _.gaps3h),
    ("2 hour gaps", _.gaps2h),
    ("1 hour gaps", _.gaps1h),
    ("30 minute gaps", _.gaps30m)
  )

  System.out.print(s"device id${SEPARATOR}email${SEPARATOR}time${SEPARATOR}feature")
  var day = firstDay
  while(day.getDayOfYear <= lastDay.getDayOfYear) {
    System.out.print(SEPARATOR)
    System.out.print(day)
    day = day.plusDays(1)
  }
  System.out.println()

  var counter: Long = 0

  for{
    (deviceId, email) <- devices
    dayPart <- dayParts
    (featureName, extractFeature) <- features
  } {
    System.out.print(deviceId)
    System.out.print(SEPARATOR)
    System.out.print(email)
    System.out.print(SEPARATOR)
    System.out.print(dayPart)
    System.out.print(SEPARATOR)
    System.out.print(featureName)
    var day = firstDay
    while(day.getDayOfYear <= lastDay.getDayOfYear) {
      System.out.print(SEPARATOR)
      val value = extractFeature(data(deviceId, day, dayPart))
      if(featureName == "transmissions")
        counter += value
      if(value != 0)
        System.out.print(value)
      day = day.plusDays(1)
    }
    System.out.println()
  }

  private def aggregateGaps(dateTime: ZonedDateTime) = {
    val duration = Duration.between(lastDateTime, dateTime)
    if(duration.compareTo(duration6h) >= 0)
      gaps6h += 1
    else if(duration.compareTo(duration3h) >= 0)
      gaps3h += 1
    else if(duration.compareTo(duration2h) >= 0)
      gaps2h += 1
    else if(duration.compareTo(duration1h) >= 0)
      gaps1h += 1
    else if(duration.compareTo(duration30m) >= 0)
      gaps30m += 1
  }
}
