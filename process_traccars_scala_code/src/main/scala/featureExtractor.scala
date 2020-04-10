import java.io.{FileWriter, PrintWriter}
import java.time.ZonedDateTime
import java.time.temporal.{ChronoUnit, WeekFields}
import java.util.Locale

//import il.ac.colman.disease.{DAO, DataPoint}
//import org.opensextant.geodesy.Geodetic2DArc

/**
  * Created by nirle on 5/7/2017.
  */
object featureExtractor extends App {
  import scala.util.parsing.json._

  var lastDataPoint: il.ac.colman.disease.DataPoint = null
  val dataPoints: Seq[il.ac.colman.disease.DataPoint] = il.ac.colman.disease.DAO.loadDataPoints()
  val data =
    JSONObject(dataPoints.groupBy(dp => dp.deviceId).map{ case (deviceId, byDevice) => deviceId.toString -> {
      JSONObject(Map(
        "email" -> byDevice.head.email,
        "weeks" -> {
          JSONObject(byDevice.groupBy(dp => getWeek(dp.dateTime)).toSeq.sortBy(_._1).map { case (week, byWeek) => week.toString -> {
            JSONObject(Map(
              "minDay" -> byWeek.minBy(dp => dp.dateTime.getDayOfYear).dateTime.truncatedTo(ChronoUnit.DAYS).toOffsetDateTime.toString,
              "maxDay" -> byWeek.maxBy(dp => dp.dateTime.getDayOfYear).dateTime.truncatedTo(ChronoUnit.DAYS).toOffsetDateTime.toString,
              "days" -> {
                JSONObject(byWeek.groupBy(dp => dp.dateTime.getDayOfYear).toSeq.sortBy(_._1).map { case (day, byDay) => day.toString -> {
                  JSONObject(Map(
                    "date" -> byDay.head.dateTime.truncatedTo(ChronoUnit.DAYS).toOffsetDateTime.toString,
                    "totalDistance" -> (byDay.maxBy(dp => dp.totalDistance).totalDistance - byDay.minBy(p => p.totalDistance).totalDistance),
                    "data" -> JSONArray(byDay.sortBy(_.dateTime.toEpochSecond).map(dp => {
                      if (lastDataPoint == null || lastDataPoint.deviceId != deviceId)
                        lastDataPoint = dp
                      val gap = lastDataPoint.dateTime.until(dp.dateTime, ChronoUnit.MINUTES)

                      val obj = JSONObject(Map(
                        "timestamp" -> dp.dateTime.toOffsetDateTime.toString,
                        "speed" -> dp.speed,
                        "distance" -> dp.distance,
                        "gap" -> gap,
                        "latitude" -> dp.position.getLatitude.inDegrees,
                        "longitude" -> dp.position.getLongitude.inDegrees
                      ))
                      lastDataPoint = dp
                      obj
                    }).toList)
                  ))
                }}.toMap)
              }
            ))
          }}.toMap)
        }
      ))
    }})

  val writer: PrintWriter = new PrintWriter(new FileWriter("features.json"))
  writer.println(data.toString())
  writer.close()

  def getWeek(date: ZonedDateTime): Int = {
    date.get(WeekFields.of(Locale.getDefault).weekOfWeekBasedYear)
  }
}
