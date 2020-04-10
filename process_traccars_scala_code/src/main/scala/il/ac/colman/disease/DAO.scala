package il.ac.colman.disease
import java.time.{ZoneOffset, ZonedDateTime}
import java.util.TimeZone

import org.opensextant.geodesy.{Geodetic2DPoint, Latitude => GeodeticLatitude, Longitude => GeodeticLongitude}

import scala.collection.mutable
import scala.util.parsing.json.{JSON, JSONObject}

/**
  * Created by nirle on 3/30/2017.
  */
object DAO {
//  var connectionUrl: String = "jdbc:sqlserver://localhost;databaseName=traccarV2;integratedSecurity=true"

  def loadDataPoints(traccar: String = "traccarV2"): Seq[DataPoint] = {
    import java.sql.DriverManager

    val connectionUrl = "jdbc:sqlserver://localhost;databaseName="+traccar+";integratedSecurity=true"

    val positions = mutable.ListBuffer[DataPoint]()

    try {
      // Load SQL Server JDBC driver and establish connection.
      // create url connection
      val connection = DriverManager.getConnection(connectionUrl)
      try {
        // select statement. takes the ID and Name(=email) of the device, the server time of the
        // position, the speed, attributes (=json), latitude, ongitude. does inner join and
        // filters: the name can't be 'Lior', 'Tal', 'Ron', 'maayan'.

        /* Assumption: data is organized by participants: all the lists are by id, and in every
            list it's by serverTime.
           Assumption: all lists are chronologically ordered. (for calculation optimization)
        */
        val statement = connection.prepareStatement(
          // can change "select" to "insert" in order to create the 'features' table in the SQL data base.
          "select d.id, d.name, p.servertime, p.speed, p.attributes, p.latitude, p.longitude" +
            " from devices d" +
            " inner join positions p" +
            " on d.id = p.deviceid" +
            " where d.name not in('Lior', 'Tal', 'Ron', 'maayan')" +
            " order by d.id, p.servertime")
        val result = statement.executeQuery()
        // for each result from the query
        while (result.next()) {
//          print(result.getTimestamp("servertime"))
          var b = ZonedDateTime.ofInstant(result.getTimestamp("servertime").toLocalDateTime, ZoneOffset.ofHours(2), TimeZone.getTimeZone("Asia/Jerusalem").toZoneId)
//          print(b.toLocalTime)
//          print("---")


          // 'attributes' is a stream and contains json, so we parse it to a json object
          val attributes: JSONObject = JSON.parseRaw(result.getString("attributes")).getOrElse(JSONObject(Map.empty)).asInstanceOf[JSONObject]
          positions += DataPoint(
            result.getLong("id"),
            result.getString("name"), // email = name
            // transferring server-time to local-date-time, by timezone +2
            // if the experiment crossing day light 7-time, the times need to be fixed.
            // Assumption: all times are at winter.
            // Assumption: default timeZone is Israel
            ZonedDateTime.ofInstant(result.getTimestamp("servertime").toLocalDateTime, ZoneOffset.ofHours(2), TimeZone.getTimeZone("Asia/Jerusalem").toZoneId),
            result.getDouble("speed"),
            attributes.obj("distance").asInstanceOf[Double],
            attributes.obj("totalDistance").asInstanceOf[Double],
            new Geodetic2DPoint(new GeodeticLongitude(Math.toRadians(result.getDouble("longitude"))), new GeodeticLatitude(Math.toRadians(result.getDouble("latitude")))),
            result.getDouble("latitude"),
            result.getDouble("longitude")
          )
        }
      } finally {
        if (connection != null)
          connection.close()
      }
    } catch {
      case e: Exception =>
        System.out.println()
        e.printStackTrace()
    }

    positions
  }

  def saveFeatures(participantsData: Iterable[FullParticipantData]): Unit = {
    System.out.println(
      s"""
         |Insert Into features(deviceId, [date], trips, totalDistance, totalWalkingDistance, totalDrivingDistance, averageTripDistance, maxTripDiameter, totalDuration, totalWalkingDuration, totalDrivingDuration, averageTripDuration, maxTripDuration, averageWalkingSpeed, averageDrivingSpeed, hasSleptHome)
         | Values (${participantsData.flatMap { participantData => participantData.dailyData.map { case (date, data) => s"${participantData.deviceId}, '$date', ${data.trips}, ${data.totalDistance}, ${data.totalWalkingDistance}, ${data.totalDrivingDistance}, ${data.averageTripDistance}, ${data.maxTripDiameter}, ${data.totalDuration.toNanos.toDouble / NANOS_IN_SECOND / 60}, ${data.totalWalkingDuration.toNanos.toDouble / NANOS_IN_SECOND / 60}, ${data.totalDrivingDuration.toNanos.toDouble / NANOS_IN_SECOND / 60}, ${data.averageTripDuration.toNanos.toDouble / NANOS_IN_SECOND / 60}, ${data.maxTripDuration.toNanos.toDouble / NANOS_IN_SECOND / 60}, ${data.averageWalkingSpeed * 3.6}, ${data.averageDrivingSpeed * 3.6}, ${if (data.hasSleptHome) 1 else 0}" } }.mkString("),(")});
    """.stripMargin)
  }
  // the content of the printed data should be copied to the sql server
  // then do "play" and it gets it in
}
